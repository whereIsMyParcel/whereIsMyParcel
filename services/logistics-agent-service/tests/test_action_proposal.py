from uuid import uuid4

from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from logistics_agent_service.application.dto import DiagnosisResult
from logistics_agent_service.domain.enums import OrderStatus
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine
from logistics_agent_service.infrastructure.persistence.models import (
    AgentActionProposal,
    Base,
)
from logistics_agent_service.infrastructure.persistence.sqlalchemy_diagnosis_repository import (
    SqlAlchemyDiagnosisRepository,
)


def _repo() -> tuple[SqlAlchemyDiagnosisRepository, sessionmaker]:
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    sf = sessionmaker(engine, expire_on_commit=False)
    return SqlAlchemyDiagnosisRepository(sf), sf


def test_recommended_actions_persisted_as_proposals() -> None:
    repo, sf = _repo()
    diagnosis = RuleBasedDiagnosisEngine().diagnose(OrderStatus.COMPENSATION_FAILED, None)
    assert diagnosis.recommended_actions  # 전제: 이 진단은 권장 조치를 낳는다

    repo.save(
        DiagnosisResult(
            diagnosis=diagnosis,
            report="리포트",
            order_id=uuid4(),
            order_number="ORD-20260718-COMPFAIL",
        )
    )

    with sf() as session:
        rows = session.execute(select(AgentActionProposal)).scalars().all()

    assert len(rows) == len(diagnosis.recommended_actions)
    assert all(row.status == "PROPOSED" for row in rows)
    persisted = {(r.action_type, r.risk_level, r.requires_approval) for r in rows}
    expected = {
        (a.action_type, a.risk_level.value, a.requires_approval)
        for a in diagnosis.recommended_actions
    }
    assert persisted == expected


def test_no_actions_means_no_proposals() -> None:
    repo, sf = _repo()
    diagnosis = RuleBasedDiagnosisEngine().diagnose(OrderStatus.CONFIRMED, ["IN_TRANSIT"], True)
    assert diagnosis.diagnosis_status.value == "NORMAL"
    assert diagnosis.recommended_actions == []

    repo.save(
        DiagnosisResult(diagnosis=diagnosis, report="정상", order_number="ORD-NORMAL")
    )

    with sf() as session:
        rows = session.execute(select(AgentActionProposal)).scalars().all()

    assert rows == []
