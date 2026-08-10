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
