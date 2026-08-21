from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session, sessionmaker

from logistics_agent_service.application.dataset import (
    PersistedDiagnosis,
    PersistedEvidence,
    PersistedToolCall,
)
from logistics_agent_service.infrastructure.persistence.models import AgentDiagnosis


class SqlAlchemyDiagnosisReader:
    """DiagnosisReaderPort/DiagnosedOrderPort의 SQLAlchemy 구현(read-only)."""

    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def list_diagnosed_order_ids(self) -> set[UUID]:
        """진단 이력이 있는 orderId 집합. scheduled scan 중복 방지용(§16.2)."""
        with self._session_factory() as session:
            rows = (
                session.execute(
                    select(AgentDiagnosis.order_id).where(
                        AgentDiagnosis.order_id.is_not(None)
                    )
                )
                .scalars()
                .all()
            )
            return {row for row in rows if row is not None}

    def list_all(self) -> list[PersistedDiagnosis]:
        with self._session_factory() as session:
            rows = (
                session.execute(
                    select(AgentDiagnosis).order_by(AgentDiagnosis.created_at)
                )
                .scalars()
                .all()
            )
            return [self._to_dto(row) for row in rows]

    def _to_dto(self, row: AgentDiagnosis) -> PersistedDiagnosis:
        return PersistedDiagnosis(
            diagnosis_id=row.id,
            trigger_type=row.trigger_type,
            incident_type=row.incident_type,
            source_service=row.source_service,
            user_question=row.user_question,
            order_id=row.order_id,
            order_number=row.order_number,
            diagnosis_status=row.diagnosis_status,
            failed_step=row.failed_step,
            compensation_status=row.compensation_status,
            confidence=row.confidence,
            summary=row.summary,
            report=row.report,
            created_at=row.created_at,
            evidence=[
                PersistedEvidence(
                    source_service=e.source_service,
                    tool_name=e.tool_name,
                    result=e.result,
                )
                for e in row.evidence
            ],
            tool_calls=[
                PersistedToolCall(
                    tool_name=tc.tool_name,
                    input=tc.input,
                    output=tc.output,
                    success=tc.success,
                    latency_ms=tc.latency_ms,
                )
                for tc in row.tool_calls
            ],
            model_used=row.llm_traces[0].model if row.llm_traces else None,
        )
