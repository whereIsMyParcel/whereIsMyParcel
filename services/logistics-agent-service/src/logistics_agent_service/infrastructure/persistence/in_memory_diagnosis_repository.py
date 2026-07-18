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

    @property
    def store(self) -> dict[UUID, DiagnosisResult]:
        return self._store
