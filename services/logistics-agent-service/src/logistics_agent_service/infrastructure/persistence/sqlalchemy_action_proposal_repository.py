from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session, sessionmaker

from logistics_agent_service.application.dto import ActionProposalView
from logistics_agent_service.domain.enums import ProposalStatus
from logistics_agent_service.infrastructure.persistence.models import (
    AgentActionProposal,
    AgentDiagnosis,
)


class SqlAlchemyActionProposalRepository:
    """ActionProposalPort의 SQLAlchemy 구현(design §16.4 T5b).

    제안(agent_action_proposal)엔 orderId가 없어, 실행에 필요한 order_id는 소속
    진단(agent_diagnosis)과 조인해 가져온다.
    """

    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def get(self, action_id: UUID) -> ActionProposalView | None:
        with self._session_factory() as session:
            row = session.execute(
                select(AgentActionProposal, AgentDiagnosis.order_id)
                .join(
                    AgentDiagnosis,
                    AgentActionProposal.diagnosis_id == AgentDiagnosis.id,
                )
                .where(AgentActionProposal.id == action_id)
            ).first()
            if row is None:
                return None
            proposal, order_id = row
            return ActionProposalView(
                action_id=proposal.id,
                order_id=order_id,
                action_type=proposal.action_type,
                status=ProposalStatus(proposal.status),
            )

    def update_status(self, action_id: UUID, status: ProposalStatus) -> None:
        with self._session_factory() as session:
            proposal = session.get(AgentActionProposal, action_id)
            if proposal is not None:
                proposal.status = status.value
                session.commit()
