from fastapi import APIRouter, Depends

from logistics_agent_service.core.config import Settings, get_settings
from logistics_agent_service.presentation.dto.health_response import HealthResponse

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
def health(settings: Settings = Depends(get_settings)) -> HealthResponse:
    return HealthResponse(
        status="UP",
        service=settings.app_name,
        version=settings.app_version,
    )
