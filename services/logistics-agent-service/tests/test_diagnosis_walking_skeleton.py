from uuid import uuid4

from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from logistics_agent_service.agent.graph.diagnosis_workflow import (
    LangGraphDiagnosisWorkflow,
)
from logistics_agent_service.agent.node.nodes import DiagnosisNodes
from logistics_agent_service.application.dto import DiagnosisResult, OrderContext
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.domain.enums import OrderStatus
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine
from logistics_agent_service.infrastructure.client.fake_hub_context_client import (
    FakeHubContextClient,
)
from logistics_agent_service.infrastructure.client.fake_order_context_client import (
    FakeOrderContextClient,
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
from logistics_agent_service.infrastructure.persistence.models import AgentDiagnosis, Base
from logistics_agent_service.infrastructure.persistence.sqlalchemy_diagnosis_repository import (
    SqlAlchemyDiagnosisRepository,
)
from logistics_agent_service.main import app


def _service(
    order_port: object | None = None,
    shipment_port: object | None = None,
    hub_port: object | None = None,
) -> DiagnosisService:
    workflow = LangGraphDiagnosisWorkflow(
        order_port=order_port or FakeOrderContextClient(),
        shipment_port=shipment_port or FakeShipmentContextClient(),
        hub_port=hub_port or FakeHubContextClient(),
        report_port=StubReportGenerator(),
        repository=InMemoryDiagnosisRepository(),
    )
    return DiagnosisService(workflow)


class _NoneStatusOrderPort:
    def get_order_context(self, identifier: str) -> OrderContext:
        return OrderContext(
            order_id=uuid4(),
            order_number="ORD-20260718-XYZ99999",
            order_status=None,
        )


def test_unknown_order_status_yields_unknown_without_crash() -> None:
    result = _service(order_port=_NoneStatusOrderPort()).diagnose_query(
        "ORD-20260718-XYZ99999 진단"
    )

    assert result.diagnosis.diagnosis_status.value == "UNKNOWN"
    assert result.order_number == "ORD-20260718-XYZ99999"
    assert result.report


def test_failed_order_maps_to_failed_compensated() -> None:
    result = _service().diagnose_query("ORD-20260718-FAILED01 왜 실패했어?")

    assert result.diagnosis.diagnosis_status.value == "FAILED_COMPENSATED"
    assert result.diagnosis.compensation_status.value == "COMPLETED"
    assert result.order_number == "ORD-20260718-FAILED01"
    assert result.report


def test_compensation_failed_maps_to_manual_review() -> None:
    result = _service().diagnose_query("ORD-20260718-COMPFAIL 확인해줘")

    assert result.diagnosis.diagnosis_status.value == "FAILED_COMPENSATION_FAILED"
    assert result.diagnosis.compensation_status.value == "FAILED"
    assert result.diagnosis.recommended_actions


def test_confirmed_without_shipment_yields_manual() -> None:
    result = _service(shipment_port=FakeShipmentContextClient(statuses=[])).diagnose_query(
        "ORD-20260718-CONFIRM1 진단"
    )

    assert result.diagnosis.diagnosis_status.value == "MANUAL_INTERVENTION_REQUIRED"
    assert result.diagnosis.failed_step.value == "SHIPMENT_CREATION"
    assert result.diagnosis.recommended_actions


def test_confirmed_with_shipment_is_normal() -> None:
    result = _service(
        shipment_port=FakeShipmentContextClient(statuses=["IN_TRANSIT"])
    ).diagnose_query("ORD-20260718-CONFIRM1 진단")

    assert result.diagnosis.diagnosis_status.value == "NORMAL"


def test_confirmed_invalid_route_yields_risk() -> None:
    result = _service(
        shipment_port=FakeShipmentContextClient(statuses=["IN_TRANSIT"]),
        hub_port=FakeHubContextClient(route_exists=False),
    ).diagnose_query("ORD-20260718-CONFIRM1 진단")

    assert result.diagnosis.diagnosis_status.value == "RISK_DETECTED"
    assert result.diagnosis.failed_step.value == "HUB_ROUTE_LOOKUP"
    assert result.diagnosis.recommended_actions


def test_unknown_when_no_order_identifier() -> None:
    result = _service().diagnose_query("아무 주문이나 봐줘")

    assert result.diagnosis.diagnosis_status.value == "UNKNOWN"
    assert result.order_id is None


def test_query_endpoint_end_to_end() -> None:
    client = TestClient(app)

    response = client.post(
        "/api/v1/agent/diagnoses/query",
        json={"message": "ORD-20260718-COMPFAIL 진단해줘"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["diagnosis_status"] == "FAILED_COMPENSATION_FAILED"
    assert body["order_number"] == "ORD-20260718-COMPFAIL"
    assert body["diagnosis_id"]
    assert body["report"]


def test_rule_confirmed_shipment_unavailable_degrades_to_normal() -> None:
    diagnosis = RuleBasedDiagnosisEngine().diagnose(OrderStatus.CONFIRMED, None)

    assert diagnosis.diagnosis_status.value == "NORMAL"


def test_rule_includes_order_and_shipment_evidence() -> None:
    diagnosis = RuleBasedDiagnosisEngine().diagnose(OrderStatus.FAILED, ["IN_TRANSIT"])

    sources = {evidence.source_service for evidence in diagnosis.evidence}
    assert sources == {"order-service", "shipment-service"}


def test_nodes_tolerate_none_message() -> None:
    nodes = DiagnosisNodes(
        order_port=FakeOrderContextClient(),
        shipment_port=FakeShipmentContextClient(),
        hub_port=FakeHubContextClient(),
        rule_engine=RuleBasedDiagnosisEngine(),
        report_port=StubReportGenerator(),
        repository=InMemoryDiagnosisRepository(),
    )

    assert nodes.normalize_input({"message": None}) == {"message": ""}
    assert nodes.resolve_order({"message": None}) == {"order_identifier": None}


def test_workflow_persists_result_to_repository() -> None:
    repository = InMemoryDiagnosisRepository()
    workflow = LangGraphDiagnosisWorkflow(
        order_port=FakeOrderContextClient(),
        shipment_port=FakeShipmentContextClient(),
        hub_port=FakeHubContextClient(),
        report_port=StubReportGenerator(),
        repository=repository,
    )

    result = DiagnosisService(workflow).diagnose_query("ORD-20260718-COMPFAIL 진단")

    assert result.diagnosis_id is not None
    assert len(repository.store) == 1
    assert next(iter(repository.store)) == result.diagnosis_id


def test_sqlalchemy_repository_persists_diagnosis_and_evidence() -> None:
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    session_factory = sessionmaker(engine, expire_on_commit=False)
    repository = SqlAlchemyDiagnosisRepository(session_factory)

    diagnosis = RuleBasedDiagnosisEngine().diagnose(OrderStatus.COMPENSATION_FAILED, None)
    result = DiagnosisResult(
        diagnosis=diagnosis,
        report="테스트 리포트",
        order_id=uuid4(),
        order_number="ORD-20260718-COMPFAIL",
    )

    diagnosis_id = repository.save(result)

    assert result.diagnosis_id == diagnosis_id
    with session_factory() as session:
        rows = session.execute(select(AgentDiagnosis)).scalars().all()
        assert len(rows) == 1
        saved = rows[0]
        assert saved.id == diagnosis_id
        assert saved.diagnosis_status == "FAILED_COMPENSATION_FAILED"
        assert saved.compensation_status == "FAILED"
        assert saved.order_number == "ORD-20260718-COMPFAIL"
        assert len(saved.evidence) == 2
