from fastapi import APIRouter, Depends

from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.presentation.dependencies import get_diagnosis_service
from logistics_agent_service.presentation.dto.diagnosis_dto import (
    DiagnosisQueryRequest,
    DiagnosisResponse,
)

router = APIRouter(prefix="/api/v1/agent/diagnoses", tags=["diagnosis"])


@router.post("/query", response_model=DiagnosisResponse)
def diagnose_query(
    request: DiagnosisQueryRequest,
    service: DiagnosisService = Depends(get_diagnosis_service),
) -> DiagnosisResponse:
    result = service.diagnose_query(request.message)
    return DiagnosisResponse.from_result(result)
