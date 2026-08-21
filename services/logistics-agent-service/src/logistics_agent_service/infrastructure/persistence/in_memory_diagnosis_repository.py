from uuid import UUID, uuid4

from logistics_agent_service.application.dto import DiagnosisResult


class InMemoryDiagnosisRepository:
    """DB 없이 실행/테스트할 때 쓰는 DiagnosisRepositoryPort 구현."""

    def __init__(self) -> None:
        self._store: dict[UUID, DiagnosisResult] = {}

    def save(self, result: DiagnosisResult) -> UUID:
        diagnosis_id = uuid4()
        result.diagnosis_id = diagnosis_id
        self._store[diagnosis_id] = result
        return diagnosis_id

    def list_diagnosed_order_ids(self) -> set[UUID]:
        """진단 이력이 있는 orderId 집합. scheduled scan 중복 방지용(§16.2).

        같은 인스턴스를 저장(save)과 조회에 공유하면 방금 진단한 건까지 반영된다.
        """
        return {
            result.order_id
            for result in self._store.values()
            if result.order_id is not None
        }

    @property
    def store(self) -> dict[UUID, DiagnosisResult]:
        return self._store
