from datetime import UTC, datetime
from uuid import UUID, uuid4

from sqlalchemy import DateTime, Float, ForeignKey, String, Text, Uuid
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


def _utcnow() -> datetime:
    return datetime.now(UTC)


class Base(DeclarativeBase):
    pass


class AgentDiagnosis(Base):
    """design §12.1 agent_diagnosis. 진단 1건의 분류 결과 + 리포트."""

    __tablename__ = "agent_diagnosis"

    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True, default=uuid4)
    trigger_type: Mapped[str] = mapped_column(String(30))
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
