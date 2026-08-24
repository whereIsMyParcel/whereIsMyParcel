from pydantic import BaseModel, Field

from logistics_agent_service.domain.enums import (
    ActionRiskLevel,
    CompensationStatus,
    DiagnosisStatus,
    FailureStep,
    Severity,
)


class Evidence(BaseModel):
    source_service: str
    tool_name: str
    result: str


class RecommendedAction(BaseModel):
    action_type: str
    risk_level: ActionRiskLevel
    description: str
    requires_approval: bool = False


class Diagnosis(BaseModel):
    """규칙 엔진이 산출하는 진단 분류 결과. 사람이 읽는 리포트(report)는 별도로 생성한다."""

    diagnosis_status: DiagnosisStatus
    failed_step: FailureStep = FailureStep.UNKNOWN
    compensation_status: CompensationStatus = CompensationStatus.UNKNOWN
    # 심각도(§13). 엔진이 diagnosis_status로부터 결정적으로 채운다(domain.severity).
    severity: Severity = Severity.LOW
    confidence: float = Field(ge=0.0, le=1.0)
    summary: str
    evidence: list[Evidence] = Field(default_factory=list)
    recommended_actions: list[RecommendedAction] = Field(default_factory=list)
