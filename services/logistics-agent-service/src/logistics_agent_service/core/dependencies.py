from functools import lru_cache

import httpx
from google import genai

from logistics_agent_service.agent.graph.diagnosis_workflow import (
    LangGraphDiagnosisWorkflow,
)
from logistics_agent_service.application.port.diagnosed_order_port import (
    DiagnosedOrderPort,
)
from logistics_agent_service.application.port.diagnosis_repository_port import (
    DiagnosisRepositoryPort,
)
from logistics_agent_service.application.port.hub_context_port import HubContextPort
from logistics_agent_service.application.port.log_context_port import LogContextPort
from logistics_agent_service.application.port.order_context_port import OrderContextPort
from logistics_agent_service.application.port.order_scan_port import OrderScanPort
from logistics_agent_service.application.port.report_generator_port import (
    ReportGeneratorPort,
)
from logistics_agent_service.application.port.shipment_context_port import (
    ShipmentContextPort,
)
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.application.service.scheduled_scan_service import (
    ScheduledScanService,
)
from logistics_agent_service.core.config import Settings, get_settings
from logistics_agent_service.domain.enums import OrderStatus
from logistics_agent_service.infrastructure.client.fake_hub_context_client import (
    FakeHubContextClient,
)
from logistics_agent_service.infrastructure.client.fake_log_context_client import (
    FakeLogContextClient,
)
from logistics_agent_service.infrastructure.client.fake_order_context_client import (
    FakeOrderContextClient,
)
from logistics_agent_service.infrastructure.client.fake_order_scan_client import (
    FakeOrderScanClient,
)
from logistics_agent_service.infrastructure.client.fake_shipment_context_client import (
    FakeShipmentContextClient,
)
from logistics_agent_service.infrastructure.client.http_hub_context_client import (
    HttpHubContextClient,
)
from logistics_agent_service.infrastructure.client.http_loki_log_client import (
    HttpLokiLogClient,
)
from logistics_agent_service.infrastructure.client.http_order_context_client import (
    HttpOrderContextClient,
)
from logistics_agent_service.infrastructure.client.http_order_scan_client import (
    HttpOrderScanClient,
)
from logistics_agent_service.infrastructure.client.http_shipment_context_client import (
    HttpShipmentContextClient,
)
from logistics_agent_service.infrastructure.llm.gemini_report_generator import (
    GeminiReportGenerator,
)
from logistics_agent_service.infrastructure.llm.stub_report_generator import (
    StubReportGenerator,
)
from logistics_agent_service.infrastructure.persistence.engine import (
    build_engine,
    build_session_factory,
    create_all,
)
from logistics_agent_service.infrastructure.persistence.in_memory_diagnosis_repository import (
    InMemoryDiagnosisRepository,
)
from logistics_agent_service.infrastructure.persistence.sqlalchemy_diagnosis_reader import (
    SqlAlchemyDiagnosisReader,
)
from logistics_agent_service.infrastructure.persistence.sqlalchemy_diagnosis_repository import (
    SqlAlchemyDiagnosisRepository,
)


def _system_headers(settings: Settings) -> dict[str, str]:
    """내부 API 호출용 service account 헤더(§8.3)."""
    return {
        "X-User-Id": settings.internal_user_id,
        "X-Username": settings.internal_username,
        "X-User-Role": settings.internal_user_role,
        "X-User-Status": settings.internal_user_status,
    }


def _build_order_port() -> OrderContextPort:
    """order_service_base_url이 있으면 실 HTTP 클라이언트, 없으면 Fake로 배선한다."""
    settings = get_settings()
    if settings.order_service_base_url:
        client = httpx.Client(
            base_url=settings.order_service_base_url,
            headers=_system_headers(settings),
            timeout=5.0,
        )
        return HttpOrderContextClient(client)
    return FakeOrderContextClient()


def _build_shipment_port() -> ShipmentContextPort:
    """shipment_service_base_url이 있으면 실 HTTP 클라이언트, 없으면 Fake로 배선한다."""
    settings = get_settings()
    if settings.shipment_service_base_url:
        client = httpx.Client(
            base_url=settings.shipment_service_base_url,
            headers=_system_headers(settings),
            timeout=5.0,
        )
        return HttpShipmentContextClient(client)
    return FakeShipmentContextClient()


def _build_hub_port() -> HubContextPort:
    """hub_service_base_url이 있으면 실 HTTP 클라이언트, 없으면 Fake로 배선한다."""
    settings = get_settings()
    if settings.hub_service_base_url:
        client = httpx.Client(
            base_url=settings.hub_service_base_url,
            headers=_system_headers(settings),
            timeout=5.0,
        )
        return HttpHubContextClient(client)
    return FakeHubContextClient()


def _build_log_port() -> LogContextPort:
    """loki_base_url이 있으면 실 Loki 클라이언트, 없으면 Fake로 배선한다.

    Loki는 내부 서비스 API가 아니라 로그 저장소라 service account 헤더를 붙이지 않는다.
    """
    settings = get_settings()
    if settings.loki_base_url:
        client = httpx.Client(base_url=settings.loki_base_url, timeout=5.0)
        return HttpLokiLogClient(
            client,
            window_minutes=settings.loki_search_window_minutes,
            limit=settings.loki_search_limit,
        )
    return FakeLogContextClient()


def _build_report_generator() -> ReportGeneratorPort:
    """GEMINI_API_KEY가 있으면 Gemini, 없으면 Stub 리포트 생성기로 배선한다."""
    settings = get_settings()
    if settings.gemini_api_key:
        client = genai.Client(api_key=settings.gemini_api_key)
        return GeminiReportGenerator(client, settings.gemini_model)
    return StubReportGenerator()


@lru_cache
def _build_persistence() -> tuple[DiagnosisRepositoryPort, DiagnosedOrderPort]:
    """진단 저장소(write)와 진단완료 orderId 조회(read, scheduled scan 중복 방지)를
    같은 저장소 위에 배선한다.

    database_url이 있으면 SQLAlchemy repository + reader, 없으면 In-Memory 저장소
    단일 인스턴스를 둘 다로 공유한다. in-memory에서 인스턴스를 공유해야 방금
    진단한 건까지 중복 방지가 본다. @lru_cache로 프로세스 내 단일 인스턴스를 보장한다.
    """
    settings = get_settings()
    if settings.database_url:
        engine = build_engine(settings.database_url)
        create_all(engine)  # 개발 편의; 운영은 마이그레이션으로 대체
        factory = build_session_factory(engine)
        return SqlAlchemyDiagnosisRepository(factory), SqlAlchemyDiagnosisReader(factory)
    repository = InMemoryDiagnosisRepository()
    return repository, repository


def _build_order_scan_port() -> OrderScanPort:
    """order_service_base_url이 있으면 실 HTTP 클라이언트, 없으면 Fake로 배선한다."""
    settings = get_settings()
    if settings.order_service_base_url:
        client = httpx.Client(
            base_url=settings.order_service_base_url,
            headers=_system_headers(settings),
            timeout=5.0,
        )
        return HttpOrderScanClient(client)
    return FakeOrderScanClient()


@lru_cache
def build_diagnosis_service() -> DiagnosisService:
    """합성 루트 배선. order/shipment 클라이언트(HTTP/Fake), 리포트 생성기(Gemini/Stub),
    진단 저장소를 환경설정에 따라 조립한다.

    core는 모든 계층을 조립할 수 있는 유일한 자리다. 각 어댑터는 대응 환경변수가
    없으면 로컬/CI용 폴백(Fake/Stub/In-memory)으로 배선된다.
    """
    repository, _ = _build_persistence()
    workflow = LangGraphDiagnosisWorkflow(
        order_port=_build_order_port(),
        shipment_port=_build_shipment_port(),
        hub_port=_build_hub_port(),
        report_port=_build_report_generator(),
        repository=repository,
        log_port=_build_log_port(),
    )
    return DiagnosisService(workflow)


@lru_cache
def build_scheduled_scan_service() -> ScheduledScanService:
    """scheduled scan(§16.2) 배선. 상태별 후보 열거(order scan port), 진단완료
    orderId 조회(중복 방지), 진단 코어(build_diagnosis_service)를 조립한다.

    진단완료 조회는 진단 저장소와 같은 인스턴스(_build_persistence)를 공유하므로,
    방금 스캔이 진단한 건도 다음 후보에서 중복 진단되지 않는다.
    """
    settings = get_settings()
    _, diagnosed_port = _build_persistence()
    scan_statuses = [OrderStatus(status) for status in settings.scan_statuses]
    return ScheduledScanService(
        scan_port=_build_order_scan_port(),
        diagnosed_port=diagnosed_port,
        diagnosis_service=build_diagnosis_service(),
        scan_statuses=scan_statuses,
    )
