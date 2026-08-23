from uuid import UUID

from logistics_agent_service.application.dto import ActionProposalView
from logistics_agent_service.domain.enums import ProposalStatus


class InMemoryActionProposalRepository:
    """DB 없이 실행/테스트할 때 쓰는 ActionProposalPort 구현.

    DB 모드가 아닌 런타임에서는 비어 있다(제안은 진단 시 DB에 영속됨). 테스트는
    seed로 직접 제안을 넣어 승인·실행 흐름을 검증한다.
    """

    def __init__(self) -> None:
        self._store: dict[UUID, ActionProposalView] = {}

    def seed(self, proposal: ActionProposalView) -> None:
        self._store[proposal.action_id] = proposal

    def get(self, action_id: UUID) -> ActionProposalView | None:
        return self._store.get(action_id)

    def update_status(self, action_id: UUID, status: ProposalStatus) -> None:
        current = self._store.get(action_id)
        if current is not None:
            self._store[action_id] = current.model_copy(update={"status": status})
