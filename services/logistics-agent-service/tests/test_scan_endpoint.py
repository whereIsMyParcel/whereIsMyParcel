from uuid import UUID, uuid4

from fastapi.testclient import TestClient

from logistics_agent_service.agent.graph.diagnosis_workflow import (
    LangGraphDiagnosisWorkflow,
)
from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.application.service.scheduled_scan_service import (
    ScheduledScanService,
)
from logistics_agent_service.domain.enums import OrderStatus
from logistics_agent_service.infrastructure.client.fake_hub_context_client import (
    FakeHubContextClient,
)
from logistics_agent_service.infrastructure.client.fake_log_context_client import (
    FakeLogContextClient,
)
from logistics_agent_service.infrastructure.client.fake_order_scan_client import (
    FakeOrderScanClient,
)
from logistics_agent_service.infrastructure.client.fake_shipment_context_client import (
    FakeShipmentContextClient,
)
from logistics_agent_service.infrastructure.llm.stub_report_generator import (
    StubReportGenerator,
)
from logistics_agent_service.infrastructure.persistence.in_memory_diagnosis_repository import (
    InMemoryDiagnosisRepository,
)
from logistics_agent_service.main import app
from logistics_agent_service.presentation.dependencies import (
    get_scheduled_scan_service,
)


class _EchoOrderPort:
    def get_order_context(self, identifier: str) -> OrderContext:
        return OrderContext(
            order_id=UUID(identifier),
            order_number=f"ORD-{identifier[:8]}",
            order_status=OrderStatus.COMPENSATION_FAILED,
        )


def _scan_service_with(candidates: list[UUID]) -> ScheduledScanService:
    repository = InMemoryDiagnosisRepository()
    workflow = LangGraphDiagnosisWorkflow(
        order_port=_EchoOrderPort(),
        shipment_port=FakeShipmentContextClient(statuses=[]),
        hub_port=FakeHubContextClient(),
        report_port=StubReportGenerator(),
        repository=repository,
        log_port=FakeLogContextClient(),
    )
    return ScheduledScanService(
        scan_port=FakeOrderScanClient({OrderStatus.COMPENSATION_FAILED: candidates}),
        diagnosed_port=repository,
        diagnosis_service=DiagnosisService(workflow),
        scan_statuses=[OrderStatus.COMPENSATION_FAILED],
    )


def test_scan_endpoint_runs_and_returns_summary() -> None:
    candidates = [uuid4(), uuid4()]
    app.dependency_overrides[get_scheduled_scan_service] = lambda: _scan_service_with(
        candidates
    )
    try:
        response = TestClient(app).post("/internal/v1/agent/scans")
    finally:
        del app.dependency_overrides[get_scheduled_scan_service]

    assert response.status_code == 200
    body = response.json()
    assert body["scanned"] == 2
    assert body["diagnosed"] == 2
    assert body["skipped"] == 0
    assert len(body["diagnosis_ids"]) == 2
