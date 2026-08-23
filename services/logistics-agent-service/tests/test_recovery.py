from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from logistics_agent_service.application.dto import ActionProposalView, OrderContext
from logistics_agent_service.application.service.recovery_service import (
    ActionNotFoundError,
    RecoveryService,
    UnsupportedActionError,
)
from logistics_agent_service.domain.enums import OrderStatus, ProposalStatus
from logistics_agent_service.infrastructure.client.fake_shipment_context_client import (
    FakeShipmentContextClient,
)
from logistics_agent_service.infrastructure.client.fake_shipment_recovery_client import (
    FakeShipmentRecoveryClient,
)
from logistics_agent_service.infrastructure.persistence.in_memory_action_proposal_repository import (  # noqa: E501
    InMemoryActionProposalRepository,
)
from logistics_agent_service.main import app
from logistics_agent_service.presentation.dependencies import get_recovery_service


class _FakeOrderCtx:
    def __init__(self, order_status: OrderStatus | None) -> None:
        self._order_status = order_status

    def get_order_context(self, identifier: str):
        return OrderContext(
            order_id=uuid4(),
            order_number="ORD-TEST",
            order_status=self._order_status,
        )


def _proposal(status=ProposalStatus.PROPOSED, action_type="CANCEL_ORPHAN_SHIPMENT"):
    return ActionProposalView(
        action_id=uuid4(),
        order_id=uuid4(),
        action_type=action_type,
        status=status,
    )


def _service(
    proposal: ActionProposalView,
    *,
    order_status=OrderStatus.CANCELLED,
    shipment_statuses=("HUB_MOVING",),
    recovery=None,
):
    proposals = InMemoryActionProposalRepository()
    proposals.seed(proposal)
    recovery = recovery or FakeShipmentRecoveryClient()
    service = RecoveryService(
        proposals=proposals,
        recovery=recovery,
        order_context=_FakeOrderCtx(order_status),
        shipment_context=FakeShipmentContextClient(statuses=list(shipment_statuses)),
    )
    return service, proposals, recovery


def test_approve_executes_when_still_orphan() -> None:
    proposal = _proposal()
    service, proposals, recovery = _service(proposal)

    result = service.approve_and_execute(proposal.action_id)

    assert result.status is ProposalStatus.EXECUTED
    assert result.executed is True
    assert recovery.cancelled == [proposal.order_id]
    assert proposals.get(proposal.action_id).status is ProposalStatus.EXECUTED


def test_supersedes_when_orphan_resolved() -> None:
    # 재검증 시 살아있는 배송이 없다 → orphan 해소, 실행하지 않고 SUPERSEDED.
    proposal = _proposal()
    service, proposals, recovery = _service(proposal, shipment_statuses=())

    result = service.approve_and_execute(proposal.action_id)

    assert result.status is ProposalStatus.SUPERSEDED
    assert result.executed is False
    assert recovery.cancelled == []
    assert proposals.get(proposal.action_id).status is ProposalStatus.SUPERSEDED


def test_supersedes_when_order_no_longer_cancelled() -> None:
    proposal = _proposal()
    service, _, recovery = _service(proposal, order_status=OrderStatus.CONFIRMED)

    result = service.approve_and_execute(proposal.action_id)

    assert result.status is ProposalStatus.SUPERSEDED
    assert recovery.cancelled == []


def test_failed_when_shipment_cancel_fails() -> None:
    proposal = _proposal()
    service, proposals, _ = _service(
        proposal, recovery=FakeShipmentRecoveryClient(succeed=False)
    )

    result = service.approve_and_execute(proposal.action_id)

    assert result.status is ProposalStatus.FAILED
    assert result.executed is False
    assert proposals.get(proposal.action_id).status is ProposalStatus.FAILED


def test_idempotent_when_already_executed() -> None:
    # 이미 종결된 제안은 재실행하지 않고 현재 상태를 반환한다.
    proposal = _proposal(status=ProposalStatus.EXECUTED)
    service, _, recovery = _service(proposal)

    result = service.approve_and_execute(proposal.action_id)

    assert result.status is ProposalStatus.EXECUTED
    assert result.executed is False
    assert recovery.cancelled == []


def test_not_found_raises() -> None:
    service, _, _ = _service(_proposal())

    with pytest.raises(ActionNotFoundError):
        service.approve_and_execute(uuid4())


def test_unsupported_action_type_raises() -> None:
    proposal = _proposal(action_type="CHECK_HUB_ROUTE")
    service, _, _ = _service(proposal)

    with pytest.raises(UnsupportedActionError):
        service.approve_and_execute(proposal.action_id)


def test_approve_endpoint_executes() -> None:
    proposal = _proposal()
    service, _, _ = _service(proposal)
    app.dependency_overrides[get_recovery_service] = lambda: service
    try:
        client = TestClient(app)
        ok = client.post(f"/internal/v1/agent/actions/{proposal.action_id}/approve")
        missing = client.post(f"/internal/v1/agent/actions/{uuid4()}/approve")
    finally:
        del app.dependency_overrides[get_recovery_service]

    assert ok.status_code == 200
    assert ok.json()["status"] == "EXECUTED"
    assert ok.json()["executed"] is True
    assert missing.status_code == 404
