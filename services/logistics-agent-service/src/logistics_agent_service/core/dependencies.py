from functools import lru_cache

from logistics_agent_service.agent.graph.diagnosis_workflow import (
    LangGraphDiagnosisWorkflow,
)
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.infrastructure.client.fake_order_context_client import (
    FakeOrderContextClient,
)
from logistics_agent_service.infrastructure.llm.stub_report_generator import (
    StubReportGenerator,
)


@lru_cache
def build_diagnosis_service() -> DiagnosisService:
    """합성 루트 배선. S1은 Fake order client + Stub report generator로 조립한다.

    core는 모든 계층을 조립할 수 있는 유일한 자리다. 실제 어댑터(HTTP order client,
    Gemini report generator)는 후속 슬라이스에서 이 배선만 교체하면 된다.
    """
    workflow = LangGraphDiagnosisWorkflow(
        order_port=FakeOrderContextClient(),
        report_port=StubReportGenerator(),
    )
    return DiagnosisService(workflow)
