import uuid
from uuid import uuid4

from fastapi.testclient import TestClient

from logistics_agent_service.agent.graph.diagnosis_workflow import (
    LangGraphDiagnosisWorkflow,
)
from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.domain.enums import OrderStatus
from logistics_agent_service.infrastructure.client.fake_hub_context_client import (
    FakeHubContextClient,
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


class _RecordingOrderPort:
    """받은 식별자를 기록하고 CONFIRMED 컨텍스트를 돌려주는 fake."""

    def __init__(self) -> None:
        self.seen: list[str] = []

    def get_order_context(self, identifier: str) -> OrderContext:
        self.seen.append(identifier)
        return OrderContext(
            order_id=uuid4(),
            order_number="ORD-20260804-INCIDENT",
            order_status=OrderStatus.COMPENSATION_FAILED,
        )


def _service(order_port: object) -> DiagnosisService:
    workflow = LangGraphDiagnosisWorkflow(
        order_port=order_port,
        shipment_port=FakeShipmentContextClient(statuses=[]),
        hub_port=FakeHubContextClient(),
        report_port=StubReportGenerator(),
        repository=InMemoryDiagnosisRepository(),
    )
    return DiagnosisService(workflow)


def test_incident_uses_given_order_id_without_regex() -> None:
    order_port = _RecordingOrderPort()
    order_id = str(uuid4())

    result = _service(order_port).diagnose_incident(order_id, "Order create saga failed")

    # message가 아니라 주어진 orderId로 조회해야 한다.
    assert order_port.seen == [order_id]
    assert result.trigger_type.value == "SYSTEM_INCIDENT"
    assert result.diagnosis.diagnosis_status.value == "FAILED_COMPENSATION_FAILED"


def test_incident_endpoint_end_to_end() -> None:
    client = TestClient(app)
    # 기본 배선의 FakeOrderContextClient가 아는 샘플의 실제 orderId를 사용한다.
    namespace = uuid.UUID("00000000-0000-0000-0000-0000000000aa")
    order_id = str(uuid.uuid5(namespace, "ORD-20260718-COMPFAIL"))

    response = client.post(
        "/internal/v1/agent/incidents",
        json={
            "incidentType": "ORDER_FAILED",
            "sourceService": "order-service",
            "orderId": order_id,
            "message": "Order create saga failed",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["trigger_type"] == "SYSTEM_INCIDENT"
    assert body["order_number"] == "ORD-20260718-COMPFAIL"
    assert body["diagnosis_status"] == "FAILED_COMPENSATION_FAILED"
    assert body["diagnosis_id"]
    assert body["report"]
