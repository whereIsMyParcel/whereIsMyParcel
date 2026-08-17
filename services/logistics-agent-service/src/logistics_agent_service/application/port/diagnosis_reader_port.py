from typing import Protocol

from logistics_agent_service.application.dataset import PersistedDiagnosis


class DiagnosisReaderPort(Protocol):
    """진단 이력 조회 포트(read-only).

    write용 DiagnosisRepositoryPort와 책임을 분리한다. dataset export 등 조회
    유스케이스가 사용하며, 구현은 infrastructure/persistence가 담당한다.
    """

    def list_all(self) -> list[PersistedDiagnosis]: ...
