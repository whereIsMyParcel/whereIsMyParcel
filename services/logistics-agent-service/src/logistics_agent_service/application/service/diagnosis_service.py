from logistics_agent_service.application.dto import DiagnosisQuery, DiagnosisResult
from logistics_agent_service.application.port.diagnosis_workflow_port import (
    DiagnosisWorkflowPort,
)
from logistics_agent_service.domain.enums import TriggerType


class DiagnosisService:
    """진단 유스케이스 진입점. 워크플로우 실행을 포트로 위임한다."""

    def __init__(self, workflow: DiagnosisWorkflowPort) -> None:
        self._workflow = workflow

    def diagnose_query(self, message: str) -> DiagnosisResult:
        return self._workflow.run(DiagnosisQuery(message=message))

    def diagnose_incident(
        self,
        order_id: str,
        message: str,
        incident_type: str | None = None,
        source_service: str | None = None,
    ) -> DiagnosisResult:
        """시스템 incident로 진단을 시작한다. orderId를 직접 받아 같은 코어를 재사용한다."""
        return self._workflow.run(
            DiagnosisQuery(
                message=message,
                trigger_type=TriggerType.SYSTEM_INCIDENT,
                order_identifier=order_id,
                incident_type=incident_type,
                source_service=source_service,
            )
        )
