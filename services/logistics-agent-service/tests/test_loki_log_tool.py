import httpx

from logistics_agent_service.agent.graph.diagnosis_workflow import (
    LangGraphDiagnosisWorkflow,
)
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.infrastructure.client.fake_hub_context_client import (
    FakeHubContextClient,
)
from logistics_agent_service.infrastructure.client.fake_log_context_client import (
    FakeLogContextClient,
)
from logistics_agent_service.infrastructure.client.fake_order_context_client import (
    FakeOrderContextClient,
)
from logistics_agent_service.infrastructure.client.fake_shipment_context_client import (
    FakeShipmentContextClient,
)
from logistics_agent_service.infrastructure.client.http_loki_log_client import (
    HttpLokiLogClient,
)
from logistics_agent_service.infrastructure.llm.stub_report_generator import (
    StubReportGenerator,
)
from logistics_agent_service.infrastructure.persistence.in_memory_diagnosis_repository import (
    InMemoryDiagnosisRepository,
)

# --- Loki HTTP 클라이언트 ---


def _client(handler) -> httpx.Client:
    return httpx.Client(
        transport=httpx.MockTransport(handler), base_url="http://loki:3100"
    )


def test_parses_log_lines() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/loki/api/v1/query_range"
        assert 'level=~"WARN|ERROR"' in request.url.params["query"]
        assert "the-order-id" in request.url.params["query"]
        return httpx.Response(
            200,
            json={
                "status": "success",
                "data": {
                    "resultType": "streams",
                    "result": [
                        {
                            "stream": {"app": "order-service", "level": "ERROR"},
                            "values": [
                                ["1690000000000000000", "재고 예약 실패 orderId=the-order-id"],
                                ["1690000000000000001", "배송 생성 실패 orderId=the-order-id"],
                            ],
                        }
                    ],
                },
            },
        )

    lines = HttpLokiLogClient(_client(handler)).search_order_logs("the-order-id")

    assert lines == [
        "재고 예약 실패 orderId=the-order-id",
        "배송 생성 실패 orderId=the-order-id",
    ]


def test_empty_result_means_no_lines() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"status": "success", "data": {"result": []}})

    assert HttpLokiLogClient(_client(handler)).search_order_logs("x") == []


def test_limit_caps_lines() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        values = [[str(i), f"line-{i}"] for i in range(50)]
        return httpx.Response(
            200,
            json={"data": {"result": [{"stream": {}, "values": values}]}},
        )

    lines = HttpLokiLogClient(_client(handler), limit=5).search_order_logs("x")

    assert lines is not None
    assert len(lines) == 5


def test_server_error_is_unavailable() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, text="boom")

    assert HttpLokiLogClient(_client(handler)).search_order_logs("x") is None


def test_transport_error_is_unavailable() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("boom")

    assert HttpLokiLogClient(_client(handler)).search_order_logs("x") is None


def test_malformed_body_is_unavailable() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"unexpected": "shape"})

    assert HttpLokiLogClient(_client(handler)).search_order_logs("x") is None


# --- 노드 evidence 반영 ---


def _service(log_port: object) -> DiagnosisService:
    workflow = LangGraphDiagnosisWorkflow(
        order_port=FakeOrderContextClient(),
        shipment_port=FakeShipmentContextClient(statuses=["IN_TRANSIT"]),
        hub_port=FakeHubContextClient(),
        report_port=StubReportGenerator(),
        repository=InMemoryDiagnosisRepository(),
        log_port=log_port,
    )
    return DiagnosisService(workflow)


def test_found_logs_are_added_as_evidence() -> None:
    log_port = FakeLogContextClient(lines=["[Saga] 재고 예약 실패. orderId=..."])

    result = _service(log_port).diagnose_query("ORD-20260718-CONFIRM1 진단")

    loki_evidence = [e for e in result.diagnosis.evidence if e.source_service == "loki"]
    assert len(loki_evidence) == 1
    assert loki_evidence[0].tool_name == "search_order_logs"
    assert "재고 예약 실패" in loki_evidence[0].result
    # 로그는 맥락 근거일 뿐, 분류는 규칙이 유지한다(CONFIRMED+배송 → NORMAL).
    assert result.diagnosis.diagnosis_status.value == "NORMAL"
    # tool_call telemetry에도 기록된다(S8).
    assert any(tc.tool_name == "search_order_logs" for tc in result.tool_calls)


def test_no_logs_means_no_loki_evidence() -> None:
    result = _service(FakeLogContextClient()).diagnose_query("ORD-20260718-CONFIRM1 진단")

    assert all(e.source_service != "loki" for e in result.diagnosis.evidence)
