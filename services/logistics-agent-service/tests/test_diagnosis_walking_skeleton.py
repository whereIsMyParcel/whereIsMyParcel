from fastapi.testclient import TestClient

from logistics_agent_service.agent.graph.diagnosis_workflow import (
    LangGraphDiagnosisWorkflow,
)
from logistics_agent_service.agent.node.nodes import DiagnosisNodes
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine
from logistics_agent_service.infrastructure.client.fake_order_context_client import (
    FakeOrderContextClient,
)
from logistics_agent_service.infrastructure.llm.stub_report_generator import (
    StubReportGenerator,
)
from logistics_agent_service.main import app


def _service() -> DiagnosisService:
    workflow = LangGraphDiagnosisWorkflow(
        order_port=FakeOrderContextClient(),
        report_port=StubReportGenerator(),
    )
    return DiagnosisService(workflow)


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


def test_nodes_tolerate_none_message() -> None:
    nodes = DiagnosisNodes(
        order_port=FakeOrderContextClient(),
        rule_engine=RuleBasedDiagnosisEngine(),
        report_port=StubReportGenerator(),
    )

    assert nodes.normalize_input({"message": None}) == {"message": ""}
    assert nodes.resolve_order({"message": None}) == {"order_identifier": None}
