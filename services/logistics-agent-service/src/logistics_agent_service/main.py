from fastapi import FastAPI

from logistics_agent_service.core.config import Settings, get_settings
from logistics_agent_service.presentation.controller import health_controller


def create_app(settings: Settings | None = None) -> FastAPI:
    app_settings = settings or get_settings()
    app = FastAPI(
        title=app_settings.app_name,
        version=app_settings.app_version,
        docs_url="/docs",
        redoc_url="/redoc",
    )
    app.include_router(health_controller.router)
    return app


app = create_app()
