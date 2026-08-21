import asyncio
import contextlib
import logging
from collections.abc import AsyncIterator

from fastapi import FastAPI

from logistics_agent_service.core.config import Settings, get_settings
from logistics_agent_service.core.dependencies import (
    build_diagnosis_service,
    build_scheduled_scan_service,
)
from logistics_agent_service.presentation.controller import (
    diagnosis_controller,
    health_controller,
    incident_controller,
    scan_controller,
)
from logistics_agent_service.presentation.dependencies import (
    get_diagnosis_service,
    get_scheduled_scan_service,
)

logger = logging.getLogger(__name__)


async def _run_scan_loop(interval_seconds: int) -> None:
    """주기적으로 scheduled scan을 실행한다(in-process, 단일 인스턴스 가정, §16.2).

    스캔은 blocking(httpx sync/LLM sync)이라 to_thread로 오프로딩해 이벤트 루프를
    막지 않는다. 한 회차 실패가 루프를 죽이지 않도록 예외를 삼키고 다음 주기로 넘어간다.
    """
    while True:
        await asyncio.sleep(interval_seconds)
        try:
            summary = await asyncio.to_thread(build_scheduled_scan_service().run_once)
            logger.info("scheduled scan done: %s", summary.model_dump())
        except Exception:
            logger.exception("scheduled scan failed")


@contextlib.asynccontextmanager
async def _lifespan(app: FastAPI) -> AsyncIterator[None]:
    settings = get_settings()
    task: asyncio.Task[None] | None = None
    if settings.scan_enabled:
        task = asyncio.create_task(_run_scan_loop(settings.scan_interval_seconds))
    try:
        yield
    finally:
        if task is not None:
            task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await task


def create_app(settings: Settings | None = None) -> FastAPI:
    app_settings = settings or get_settings()
    app = FastAPI(
        title=app_settings.app_name,
        version=app_settings.app_version,
        docs_url="/docs",
        redoc_url="/redoc",
        lifespan=_lifespan,
    )
    app.include_router(health_controller.router)
    app.include_router(diagnosis_controller.router)
    app.include_router(incident_controller.router)
    app.include_router(scan_controller.router)

    # 합성 루트: presentation의 placeholder 의존성에 실제 구현을 주입한다.
    app.dependency_overrides[get_diagnosis_service] = build_diagnosis_service
    app.dependency_overrides[get_scheduled_scan_service] = build_scheduled_scan_service
    return app


app = create_app()
