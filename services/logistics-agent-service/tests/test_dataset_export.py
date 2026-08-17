import json
from uuid import uuid4

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from logistics_agent_service.application.dto import (
    DiagnosisResult,
    LlmTrace,
    ToolCallRecord,
)
from logistics_agent_service.application.service.dataset_export_service import (
    DatasetExportService,
)
from logistics_agent_service.domain.enums import OrderStatus, TriggerType
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine
from logistics_agent_service.infrastructure.persistence.models import Base
from logistics_agent_service.infrastructure.persistence.sqlalchemy_diagnosis_reader import (
    SqlAlchemyDiagnosisReader,
)
from logistics_agent_service.infrastructure.persistence.sqlalchemy_diagnosis_repository import (
    SqlAlchemyDiagnosisRepository,
)


def _session_factory() -> sessionmaker:
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    return sessionmaker(engine, expire_on_commit=False)


def _seed(sf: sessionmaker) -> None:
    repo = SqlAlchemyDiagnosisRepository(sf)
    engine = RuleBasedDiagnosisEngine()

    repo.save(
        DiagnosisResult(
            diagnosis=engine.diagnose(OrderStatus.CONFIRMED, ["IN_TRANSIT"], True),
            report="정상 리포트",
            trigger_type=TriggerType.USER_QUERY,
            order_id=uuid4(),
            order_number="ORD-DS-QUERY",
            user_question="이 주문 상태 알려줘",
            tool_calls=[
                ToolCallRecord(
                    tool_name="get_order_context",
                    input={"identifier": "ORD-DS-QUERY"},
                    output={"order_status": "CONFIRMED"},
                    success=True,
                    latency_ms=3,
                ),
                ToolCallRecord(
                    tool_name="route_exists",
                    input={"origin": "a", "destination": "b"},
                    output={"route_exists": True},
                    success=True,
                    latency_ms=2,
                ),
            ],
            llm_trace=LlmTrace(
                model="gemini-2.5-flash",
                prompt_version="v1",
                output_message="정상 리포트",
                latency_ms=10,
            ),
        )
    )
    repo.save(
        DiagnosisResult(
            diagnosis=engine.diagnose(OrderStatus.FAILED, []),
            report="실패-보상완료 리포트",
            trigger_type=TriggerType.SYSTEM_INCIDENT,
            order_id=uuid4(),
            order_number="ORD-DS-INCIDENT",
            incident_type="ORDER_FAILED",
            source_service="order-service",
        )
    )


def _by_status(rows: list[dict], status: str) -> dict:
    return next(r for r in rows if r["label"]["diagnosisStatus"] == status)


def test_export_labeled_jsonl_structure() -> None:
    sf = _session_factory()
    _seed(sf)

    jsonl = DatasetExportService(SqlAlchemyDiagnosisReader(sf)).export_jsonl()
    rows = [json.loads(line) for line in jsonl.splitlines()]

    assert len(rows) == 2
    assert {r["id"] for r in rows} == {"diag-000001", "diag-000002"}
    assert all(r["task"] == "order_failure_diagnosis_report" for r in rows)

    query = _by_status(rows, "NORMAL")
    assert query["input"]["userQuestion"] == "이 주문 상태 알려줘"
    assert query["input"]["orderContext"] == {"order_status": "CONFIRMED"}
    assert query["input"]["hubContext"]["routeChecks"] == [{"route_exists": True}]
    assert query["label"]["diagnosisStatus"] == "NORMAL"
    assert query["metadata"]["source"] == "runtime"
    assert query["metadata"]["modelUsedForDraft"] == "gemini-2.5-flash"
    assert query["metadata"]["reviewedByHuman"] is False
    # 규칙 엔진 evidence가 toolEvidence로 실린다.
    assert any(
        e["sourceService"] == "order-service" for e in query["input"]["toolEvidence"]
    )

    incident = _by_status(rows, "FAILED_COMPENSATED")
    assert incident["label"]["diagnosisStatus"] == "FAILED_COMPENSATED"
    assert incident["label"]["compensationStatus"] == "COMPLETED"
    # 미영속 필드는 정직하게 null.
    assert incident["label"]["responsibleService"] is None
    assert incident["metadata"]["modelUsedForDraft"] is None


def test_export_sft_chat_format() -> None:
    sf = _session_factory()
    _seed(sf)

    jsonl = DatasetExportService(SqlAlchemyDiagnosisReader(sf)).export_jsonl(sft=True)
    rows = [json.loads(line) for line in jsonl.splitlines()]

    assert len(rows) == 2
    messages = rows[0]["messages"]
    assert [m["role"] for m in messages] == ["system", "user", "assistant"]
    assert "logistics operations diagnosis assistant" in messages[0]["content"]
    # user/assistant content는 JSON 문자열(context / label).
    assert "userQuestion" in json.loads(messages[1]["content"])
    assert "diagnosisStatus" in json.loads(messages[2]["content"])


def test_export_empty_when_no_diagnoses() -> None:
    sf = _session_factory()

    jsonl = DatasetExportService(SqlAlchemyDiagnosisReader(sf)).export_jsonl()

    assert jsonl == ""
