from typing import Protocol
from uuid import UUID

from logistics_agent_service.application.dto import DiagnosisResult


class DiagnosisRepositoryPort(Protocol):
    """진단 결과 저장 포트.

    구현은 infrastructure/persistence가 담당한다. 저장 후 부여된 diagnosis_id를
    반환하며, 전달된 result의 diagnosis_id도 함께 채운다.
    """

    def save(self, result: DiagnosisResult) -> UUID: ...
