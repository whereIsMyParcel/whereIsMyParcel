from uuid import uuid4

from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from logistics_agent_service.agent.graph.diagnosis_workflow import (
    LangGraphDiagnosisWorkflow,
)
from logistics_agent_service.application.dto import (
    DiagnosisResult,
    LlmTrace,
    OrderContext,
    ReportResult,
    ToolCallRecord,
)
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.domain.enums import OrderStatus
from logistics_agent_service.domain.models import Diagnosis
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine
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
from logistics_agent_service.infrastructure.llm.stub_report_generator import (
    StubReportGenerator,
)
from logistics_agent_service.infrastructure.persistence.in_memory_diagnosis_repository import (
    InMemoryDiagnosisRepository,
)
from logistics_agent_service.infrastructure.persistence.models import (
    AgentLlmTrace,
    AgentToolCall,
    Base,
)
from logistics_agent_service.infrastructure.persistence.sqlalchemy_diagnosis_repository import (
    SqlAlchemyDiagnosisRepository,
)


class _TracingReportGenerator:
    """리포트와 함께 LLM trace를 반환하는 테스트용 ReportGeneratorPort 구현."""

    def generate(
        self, diagnosis: Diagnosis, order_context: OrderContext | None
    ) -> ReportResult:
        return ReportResult(
            report="테스트 리포트",
            trace=LlmTrace(
                model="fake-model",
                prompt_version="v1",
                input_messages=[{"role": "user", "content": "prompt"}],
                output_message="테스트 리포트",
                token_usage={"total_tokens": 42},
                latency_ms=1,
            ),
        )


def _workflow(report_port: object) -> LangGraphDiagnosisWorkflow:
    return LangGraphDiagnosisWorkflow(
        order_port=FakeOrderContextClient(),
        shipment_port=FakeShipmentContextClient(statuses=["IN_TRANSIT"]),
        hub_port=FakeHubContextClient(),
        report_port=report_port,
        repository=InMemoryDiagnosisRepository(),
        log_port=FakeLogContextClient(),
    )


def test_workflow_buffers_tool_calls_for_each_port_call() -> None:
    service = DiagnosisService(_workflow(_TracingReportGenerator()))

    result = service.diagnose_query("ORD-20260718-CONFIRM1 진단")

    assert [call.tool_name for call in result.tool_calls] == [
        "get_order_context",
        "get_shipments",
        "route_exists",
        "search_order_logs",
    ]
    assert all(call.success for call in result.tool_calls)
    assert all(call.latency_ms >= 0 for call in result.tool_calls)
    # input/output 관측 payload가 채워진다.
    assert result.tool_calls[0].input == {"identifier": "ORD-20260718-CONFIRM1"}
    assert result.tool_calls[1].output == {"count": 1, "statuses": ["IN_TRANSIT"]}
    assert result.tool_calls[2].output == {"route_exists": True}


def test_workflow_buffers_llm_trace_when_generator_returns_one() -> None:
    service = DiagnosisService(_workflow(_TracingReportGenerator()))

    result = service.diagnose_query("ORD-20260718-CONFIRM1 진단")

    assert result.llm_trace is not None
    assert result.llm_trace.model == "fake-model"
    assert result.llm_trace.token_usage == {"total_tokens": 42}


def test_stub_generator_yields_no_llm_trace_but_still_records_tool_calls() -> None:
    service = DiagnosisService(_workflow(StubReportGenerator()))

    result = service.diagnose_query("ORD-20260718-CONFIRM1 진단")

    assert result.llm_trace is None
    assert len(result.tool_calls) == 4


def test_no_order_identifier_records_no_tool_calls() -> None:
    service = DiagnosisService(_workflow(StubReportGenerator()))

    result = service.diagnose_query("아무 주문이나 봐줘")

    assert result.tool_calls == []
    assert result.llm_trace is None


def test_sqlalchemy_repository_persists_tool_calls_and_llm_trace() -> None:
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    session_factory = sessionmaker(engine, expire_on_commit=False)
    repository = SqlAlchemyDiagnosisRepository(session_factory)

    diagnosis = RuleBasedDiagnosisEngine().diagnose(OrderStatus.CONFIRMED, ["IN_TRANSIT"])
    result = DiagnosisResult(
        diagnosis=diagnosis,
        report="테스트 리포트",
        order_id=uuid4(),
        order_number="ORD-20260718-CONFIRM1",
        tool_calls=[
            ToolCallRecord(
                tool_name="get_order_context",
                input={"identifier": "ORD-20260718-CONFIRM1"},
                output={"order_status": "CONFIRMED"},
                success=True,
                latency_ms=3,
            ),
            ToolCallRecord(
                tool_name="get_shipments",
                input={"order_id": "x"},
                output=None,
                success=False,
                latency_ms=5,
            ),
        ],
        llm_trace=LlmTrace(
            model="fake-model",
            prompt_version="v1",
            input_messages=[{"role": "user", "content": "prompt"}],
            output_message="테스트 리포트",
            token_usage={"total_tokens": 42},
            latency_ms=7,
        ),
    )

    diagnosis_id = repository.save(result)

    with session_factory() as session:
        calls = (
            session.execute(
                select(AgentToolCall).order_by(AgentToolCall.tool_name)
            )
            .scalars()
            .all()
        )
        traces = session.execute(select(AgentLlmTrace)).scalars().all()

    assert {c.diagnosis_id for c in calls} == {diagnosis_id}
    assert [c.tool_name for c in calls] == ["get_order_context", "get_shipments"]
    assert calls[0].success is True
    assert calls[0].output == {"order_status": "CONFIRMED"}
    assert calls[1].success is False
    assert calls[1].output is None

    assert len(traces) == 1
    assert traces[0].diagnosis_id == diagnosis_id
    assert traces[0].model == "fake-model"
    assert traces[0].token_usage == {"total_tokens": 42}
    assert traces[0].input_messages == [{"role": "user", "content": "prompt"}]
