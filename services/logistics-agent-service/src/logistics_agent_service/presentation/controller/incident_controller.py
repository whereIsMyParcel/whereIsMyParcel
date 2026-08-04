from fastapi import APIRouter, Depends

from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.presentation.dependencies import get_diagnosis_service
from logistics_agent_service.presentation.dto.diagnosis_dto import (
    DiagnosisResponse,
    IncidentRequest,
)

router = APIRouter(prefix="/internal/v1/agent", tags=["incident"])


@router.post("/incidents", response_model=DiagnosisResponse)
def diagnose_incident(
    request: IncidentRequest,
    service: DiagnosisService = Depends(get_diagnosis_service),
) -> DiagnosisResponse:
    result = service.diagnose_incident(str(request.order_id), request.message)
    return DiagnosisResponse.from_result(result)
