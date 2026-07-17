from fastapi import APIRouter

from logistics_agent_service.core.config import get_settings
from logistics_agent_service.presentation.dto.health_response import HealthResponse

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    settings = get_settings()
    return HealthResponse(
        status="UP",
        service=settings.app_name,
        version=settings.app_version,
    )
