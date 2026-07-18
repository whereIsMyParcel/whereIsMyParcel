from uuid import UUID

from pydantic import BaseModel

from logistics_agent_service.application.dto import DiagnosisResult
from logistics_agent_service.domain.enums import (
    CompensationStatus,
    DiagnosisStatus,
    FailureStep,
    TriggerType,
)
from logistics_agent_service.domain.models import Evidence, RecommendedAction


class DiagnosisQueryRequest(BaseModel):
    message: str


class DiagnosisResponse(BaseModel):
    """운영자 응답 스키마(design §11). diagnosis_id는 영속 계층이 부여한 값을 쓴다."""

    diagnosis_id: UUID
    trigger_type: TriggerType
    order_id: UUID | None
    order_number: str | None
    diagnosis_status: DiagnosisStatus
    failed_step: FailureStep
    compensation_status: CompensationStatus
    confidence: float
    summary: str
    evidence: list[Evidence]
    recommended_actions: list[RecommendedAction]
    report: str

    @classmethod
    def from_result(cls, result: DiagnosisResult) -> "DiagnosisResponse":
        diagnosis = result.diagnosis
        return cls(
            diagnosis_id=result.diagnosis_id,
            trigger_type=result.trigger_type,
            order_id=result.order_id,
            order_number=result.order_number,
            diagnosis_status=diagnosis.diagnosis_status,
            failed_step=diagnosis.failed_step,
            compensation_status=diagnosis.compensation_status,
            confidence=diagnosis.confidence,
            summary=diagnosis.summary,
            evidence=diagnosis.evidence,
            recommended_actions=diagnosis.recommended_actions,
            report=result.report,
        )
