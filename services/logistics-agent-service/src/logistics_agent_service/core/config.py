from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "logistics-agent-service"
    app_version: str = "0.1.0"
    environment: str = "local"

    gemini_api_key: str | None = None
    gemini_model: str = "gemini-2.0-flash"
    database_url: str | None = None
    order_service_base_url: str | None = None
    shipment_service_base_url: str | None = None
    hub_service_base_url: str | None = None

    # 관측: Loki 로그 조회(없으면 Fake). window/limit은 검색 범위.
    loki_base_url: str | None = None
    loki_search_window_minutes: int = 60
    loki_search_limit: int = 20

    # scheduled scan(§16.2): 주기적으로 상태별 고장 후보를 열거해 선제 진단한다.
    # scan_enabled=false(기본)면 in-process 스케줄러를 띄우지 않는다(CI/테스트 안전).
    # 수동 트리거(POST /internal/v1/agent/scans)는 enabled와 무관하게 동작한다.
    scan_enabled: bool = False
    scan_interval_seconds: int = 300
    scan_statuses: list[str] = ["COMPENSATION_FAILED", "FAILED"]

    internal_user_id: str = "00000000-0000-0000-0000-000000000001"
    internal_username: str = "logistics-agent-service"
    internal_user_role: str = "MASTER"
    internal_user_status: str = "ACTIVE"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
