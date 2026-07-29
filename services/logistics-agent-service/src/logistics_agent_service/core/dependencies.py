from functools import lru_cache

from logistics_agent_service.agent.graph.diagnosis_workflow import (
    LangGraphDiagnosisWorkflow,
)
from logistics_agent_service.application.port.diagnosis_repository_port import (
    DiagnosisRepositoryPort,
)
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.core.config import get_settings
from logistics_agent_service.infrastructure.client.fake_order_context_client import (
    FakeOrderContextClient,
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
from logistics_agent_service.infrastructure.persistence.sqlalchemy_diagnosis_repository import (
    SqlAlchemyDiagnosisRepository,
)


def _build_repository() -> DiagnosisRepositoryPort:
    """database_url이 있으면 SQLAlchemy, 없으면 In-Memory 저장소로 배선한다."""
    settings = get_settings()
    if settings.database_url:
        engine = build_engine(settings.database_url)
        create_all(engine)  # 개발 편의; 운영은 마이그레이션으로 대체
        return SqlAlchemyDiagnosisRepository(build_session_factory(engine))
    return InMemoryDiagnosisRepository()


@lru_cache
def build_diagnosis_service() -> DiagnosisService:
    """합성 루트 배선. S1은 Fake order client + Stub report generator,
    S2는 진단 결과 저장소(DiagnosisRepositoryPort)를 추가로 조립한다.

    core는 모든 계층을 조립할 수 있는 유일한 자리다. 실제 어댑터(HTTP order client,
    Gemini report generator)는 후속 슬라이스에서 이 배선만 교체하면 된다.
    """
    workflow = LangGraphDiagnosisWorkflow(
        order_port=FakeOrderContextClient(),
        report_port=StubReportGenerator(),
        repository=_build_repository(),
    )
    return DiagnosisService(workflow)
