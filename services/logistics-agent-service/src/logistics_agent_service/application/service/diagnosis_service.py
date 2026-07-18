from logistics_agent_service.application.dto import DiagnosisQuery, DiagnosisResult
from logistics_agent_service.application.port.diagnosis_workflow_port import (
    DiagnosisWorkflowPort,
)


class DiagnosisService:
    """진단 유스케이스 진입점. 워크플로우 실행을 포트로 위임한다."""

    def __init__(self, workflow: DiagnosisWorkflowPort) -> None:
        self._workflow = workflow

    def diagnose_query(self, message: str) -> DiagnosisResult:
        return self._workflow.run(DiagnosisQuery(message=message))
