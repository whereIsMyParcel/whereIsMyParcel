from uuid import UUID

from logistics_agent_service.application.dto import RecoveryResult
from logistics_agent_service.application.port.action_proposal_port import (
    ActionProposalPort,
)
from logistics_agent_service.application.port.order_context_port import OrderContextPort
from logistics_agent_service.application.port.recovery_action_port import (
    RecoveryActionPort,
)
from logistics_agent_service.application.port.shipment_context_port import (
    ShipmentContextPort,
)
from logistics_agent_service.domain.enums import ProposalStatus
from logistics_agent_service.domain.shipment_consistency import is_orphan_shipment

# T5b가 실행할 수 있는 유일한 recovery 조치. 다른 action_type은 아직 미지원.
_CANCEL_ORPHAN_SHIPMENT = "CANCEL_ORPHAN_SHIPMENT"


class ActionNotFoundError(Exception):
    """승인 대상 제안(actionId)이 없다 → 404."""


class UnsupportedActionError(Exception):
    """실행 가능한 recovery 조치가 아니다(현재 CANCEL_ORPHAN_SHIPMENT만) → 422."""


class RecoveryService:
    """승인 기반 recovery 실행 오케스트레이션(design §16.4 T5b).

    흐름: 제안 조회 → 멱등 확인(PROPOSED만 진행) → 실행 직전 orphan 재검증 →
    shipment 취소 실행 → 상태 전이(EXECUTED/FAILED/SUPERSEDED). 진단(§5)은 계속
    read-only이며, 실제 write는 오직 이 승인 게이트(재검증 통과) 뒤에서만 일어난다.
    """

    def __init__(
        self,
        proposals: ActionProposalPort,
        recovery: RecoveryActionPort,
        order_context: OrderContextPort,
        shipment_context: ShipmentContextPort,
    ) -> None:
        self._proposals = proposals
        self._recovery = recovery
        self._order_context = order_context
        self._shipment_context = shipment_context

    def approve_and_execute(self, action_id: UUID) -> RecoveryResult:
        proposal = self._proposals.get(action_id)
        if proposal is None:
            raise ActionNotFoundError(str(action_id))

        # 멱등: 이미 종결/전이된 제안은 재실행하지 않고 현재 상태를 반환한다.
        if proposal.status is not ProposalStatus.PROPOSED:
            return RecoveryResult(
                action_id=action_id,
                status=proposal.status,
                executed=False,
                detail="이미 처리된 제안입니다(재실행하지 않음).",
            )

        if proposal.action_type != _CANCEL_ORPHAN_SHIPMENT:
            raise UnsupportedActionError(proposal.action_type)

        # 실행 직전 재검증: 제안↔승인 사이 상태가 바뀌었을 수 있다. orphan이 여전히
        # 성립할 때만 실제 취소를 실행한다(§16.4 안전장치).
        if not self._still_orphan(proposal.order_id):
            self._proposals.update_status(action_id, ProposalStatus.SUPERSEDED)
            return RecoveryResult(
                action_id=action_id,
                status=ProposalStatus.SUPERSEDED,
                executed=False,
                detail="orphan 상태가 더 이상 성립하지 않아 실행하지 않았습니다.",
            )

        ok = self._recovery.cancel_orphan_shipment(proposal.order_id)
        status = ProposalStatus.EXECUTED if ok else ProposalStatus.FAILED
        self._proposals.update_status(action_id, status)
        return RecoveryResult(
            action_id=action_id,
            status=status,
            executed=ok,
            detail=(
                "orphan 배송 취소를 실행했습니다."
                if ok
                else "배송 취소 실행에 실패했습니다(shipment-service 오류)."
            ),
        )

    def _still_orphan(self, order_id: UUID | None) -> bool:
        if order_id is None:
            return False
        order = self._order_context.get_order_context(str(order_id))
        shipments = self._shipment_context.get_shipments(order_id)
        order_status = order.order_status if order is not None else None
        statuses = [info.status for info in shipments] if shipments else []
        return is_orphan_shipment(order_status, statuses)
