from datetime import UTC, datetime
from uuid import UUID, uuid4

from sqlalchemy import (
    JSON,
    Boolean,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    Uuid,
)
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship

# design §12는 jsonb를 명시한다. PostgreSQL에서는 jsonb, SQLite 등에서는 json으로
# 매핑해 테스트 호환성을 유지한다(모델에 dialect를 하드코딩하지 않는다).
_JSON = JSON().with_variant(JSONB(), "postgresql")


def _utcnow() -> datetime:
    return datetime.now(UTC)


class Base(DeclarativeBase):
    pass


class AgentDiagnosis(Base):
    """design §12.1 agent_diagnosis. 진단 1건의 분류 결과 + 리포트."""

    __tablename__ = "agent_diagnosis"

    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True, default=uuid4)
    trigger_type: Mapped[str] = mapped_column(String(30))
    incident_type: Mapped[str | None] = mapped_column(String(40), nullable=True)
    source_service: Mapped[str | None] = mapped_column(String(50), nullable=True)
    user_question: Mapped[str | None] = mapped_column(Text, nullable=True)
    order_id: Mapped[UUID | None] = mapped_column(Uuid, nullable=True)
    order_number: Mapped[str | None] = mapped_column(String(100), nullable=True)
    diagnosis_status: Mapped[str] = mapped_column(String(40))
    failed_step: Mapped[str] = mapped_column(String(40))
    compensation_status: Mapped[str] = mapped_column(String(40))
    confidence: Mapped[float] = mapped_column(Float)
    summary: Mapped[str] = mapped_column(Text)
    report: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow)

    evidence: Mapped[list["AgentEvidence"]] = relationship(
        back_populates="diagnosis",
        cascade="all, delete-orphan",
    )
    tool_calls: Mapped[list["AgentToolCall"]] = relationship(
        back_populates="diagnosis",
        cascade="all, delete-orphan",
    )
    llm_traces: Mapped[list["AgentLlmTrace"]] = relationship(
        back_populates="diagnosis",
        cascade="all, delete-orphan",
    )


class AgentEvidence(Base):
    """design §12.2 agent_evidence. 진단 근거(툴 조회 결과) 1건."""

    __tablename__ = "agent_evidence"

    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True, default=uuid4)
    diagnosis_id: Mapped[UUID] = mapped_column(ForeignKey("agent_diagnosis.id"))
    source_service: Mapped[str] = mapped_column(String(50))
    tool_name: Mapped[str] = mapped_column(String(100))
    result: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow)

    diagnosis: Mapped["AgentDiagnosis"] = relationship(back_populates="evidence")


class AgentToolCall(Base):
    """design §12.3 agent_tool_call. 진단 중 호출한 internal API tool 1건의 관측."""

    __tablename__ = "agent_tool_call"

    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True, default=uuid4)
    diagnosis_id: Mapped[UUID] = mapped_column(ForeignKey("agent_diagnosis.id"))
    tool_name: Mapped[str] = mapped_column(String(100))
    input: Mapped[dict | None] = mapped_column(_JSON, nullable=True)
    output: Mapped[dict | None] = mapped_column(_JSON, nullable=True)
    success: Mapped[bool] = mapped_column(Boolean)
    error_code: Mapped[str | None] = mapped_column(String(50), nullable=True)
    error_message: Mapped[str | None] = mapped_column(Text, nullable=True)
    latency_ms: Mapped[int] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow)

    diagnosis: Mapped["AgentDiagnosis"] = relationship(back_populates="tool_calls")


class AgentLlmTrace(Base):
    """design §12.5 agent_llm_trace. 리포트 생성 LLM 호출 1건의 관측."""

    __tablename__ = "agent_llm_trace"

    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True, default=uuid4)
    diagnosis_id: Mapped[UUID] = mapped_column(ForeignKey("agent_diagnosis.id"))
    model: Mapped[str] = mapped_column(String(100))
    prompt_version: Mapped[str] = mapped_column(String(30))
    input_messages: Mapped[list | None] = mapped_column(_JSON, nullable=True)
    output_message: Mapped[str] = mapped_column(Text)
    token_usage: Mapped[dict | None] = mapped_column(_JSON, nullable=True)
    latency_ms: Mapped[int] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow)

    diagnosis: Mapped["AgentDiagnosis"] = relationship(back_populates="llm_traces")
