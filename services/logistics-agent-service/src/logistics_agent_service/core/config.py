from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "logistics-agent-service"
    app_version: str = "0.1.0"
    environment: str = "local"

    gemini_api_key: str | None = None
    gemini_model: str = "gemini-2.0-flash"

    # LLM 리포트 품질 eval(§14, S18): 비결정적·과금이라 CI 게이트 밖 opt-in 레인.
    # eval_llm_enabled=false(기본)면 report_eval_cli/pytest가 즉시 skip한다.
    # judge는 리포트 생성(flash)과 다른 상위 tier로 분리해 self-preference bias를
    # 완화한다(생성=flash가 쓴 글을 pro가 채점). eval_judge_model은 config knob이라
    # 향후 타 벤더로 교차검증 시 코드 변경 없이 교체 가능하다.
    eval_llm_enabled: bool = False
    eval_judge_model: str = "gemini-2.5-pro"
    eval_judge_min_score: int = 4
    eval_report_min_length: int = 40
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
