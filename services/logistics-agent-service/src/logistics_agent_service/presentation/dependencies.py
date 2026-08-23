from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.application.service.recovery_service import RecoveryService
from logistics_agent_service.application.service.scheduled_scan_service import (
    ScheduledScanService,
)


def get_diagnosis_service() -> DiagnosisService:
    """FastAPI 의존성 키(placeholder).

    실제 구현은 합성 루트(main.create_app)가 dependency_overrides로 주입한다.
    presentation이 core/infrastructure/agent를 직접 import하지 않게 하기 위한 지점이다.
    """
    raise NotImplementedError


def get_scheduled_scan_service() -> ScheduledScanService:
    """scheduled scan 서비스 의존성 키(placeholder).

    get_diagnosis_service와 동일하게 합성 루트가 dependency_overrides로 주입한다.
    """
    raise NotImplementedError


def get_recovery_service() -> RecoveryService:
    """승인 기반 recovery 서비스 의존성 키(placeholder, §16.4 T5b).

    합성 루트가 dependency_overrides로 실제 구현을 주입한다.
    """
    raise NotImplementedError
