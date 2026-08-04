from fastapi import FastAPI

from logistics_agent_service.core.config import Settings, get_settings
from logistics_agent_service.core.dependencies import build_diagnosis_service
from logistics_agent_service.presentation.controller import (
    diagnosis_controller,
    health_controller,
    incident_controller,
)
from logistics_agent_service.presentation.dependencies import get_diagnosis_service


def create_app(settings: Settings | None = None) -> FastAPI:
    app_settings = settings or get_settings()
    app = FastAPI(
        title=app_settings.app_name,
        version=app_settings.app_version,
        docs_url="/docs",
        redoc_url="/redoc",
    )
    app.include_router(health_controller.router)
    app.include_router(diagnosis_controller.router)
    app.include_router(incident_controller.router)

    # 합성 루트: presentation의 placeholder 의존성에 실제 구현을 주입한다.
    app.dependency_overrides[get_diagnosis_service] = build_diagnosis_service
    return app


app = create_app()
