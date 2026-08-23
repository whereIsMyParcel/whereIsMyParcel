from typing import Protocol
from uuid import UUID

from logistics_agent_service.application.dto import ActionProposalView
from logistics_agent_service.domain.enums import ProposalStatus


class ActionProposalPort(Protocol):
    """조치 제안(agent_action_proposal)의 조회·상태 전이 포트(design §16.4 T5b).

    승인 기반 recovery가 제안을 actionId로 찾아(order_id 포함) 실행 후 상태를
    EXECUTED/FAILED/SUPERSEDED로 전이한다. 제안이 없으면 get은 None을 반환한다.
    """

    def get(self, action_id: UUID) -> ActionProposalView | None: ...

    def update_status(self, action_id: UUID, status: ProposalStatus) -> None: ...
