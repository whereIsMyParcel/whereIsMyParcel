from typing import Protocol

from logistics_agent_service.application.dto import DiagnosisQuery, DiagnosisResult


class DiagnosisWorkflowPort(Protocol):
    """진단 워크플로우 실행 포트.

    구현은 agent 계층의 LangGraph 그래프가 담당한다. application이 agent를 직접
    import하지 않도록 하는 의존성 역전 지점이다(agent -> application 방향 유지).
    """

    def run(self, query: DiagnosisQuery) -> DiagnosisResult: ...
