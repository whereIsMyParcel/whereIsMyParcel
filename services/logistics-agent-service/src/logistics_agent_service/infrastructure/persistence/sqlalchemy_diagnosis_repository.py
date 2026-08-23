from uuid import UUID

from sqlalchemy.orm import Session, sessionmaker

from logistics_agent_service.application.dto import DiagnosisResult
from logistics_agent_service.domain.enums import ProposalStatus
from logistics_agent_service.infrastructure.persistence.models import (
    AgentActionProposal,
    AgentDiagnosis,
    AgentEvidence,
    AgentLlmTrace,
    AgentToolCall,
)


class SqlAlchemyDiagnosisRepository:
    """DiagnosisRepositoryPort의 SQLAlchemy 구현."""

    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def save(self, result: DiagnosisResult) -> UUID:
        diagnosis = AgentDiagnosis(
            trigger_type=result.trigger_type.value,
            incident_type=result.incident_type,
            source_service=result.source_service,
            user_question=result.user_question,
            order_id=result.order_id,
            order_number=result.order_number,
            diagnosis_status=result.diagnosis.diagnosis_status.value,
            failed_step=result.diagnosis.failed_step.value,
            compensation_status=result.diagnosis.compensation_status.value,
            severity=result.diagnosis.severity.value,
            confidence=result.diagnosis.confidence,
            summary=result.diagnosis.summary,
            report=result.report,
            evidence=[
                AgentEvidence(
                    source_service=item.source_service,
                    tool_name=item.tool_name,
                    result=item.result,
                )
                for item in result.diagnosis.evidence
            ],
            action_proposals=[
                AgentActionProposal(
                    action_type=action.action_type,
                    risk_level=action.risk_level.value,
                    description=action.description,
                    requires_approval=action.requires_approval,
                    status=ProposalStatus.PROPOSED.value,
                )
                for action in result.diagnosis.recommended_actions
            ],
            tool_calls=[
                AgentToolCall(
                    tool_name=call.tool_name,
                    input=call.input,
                    output=call.output,
                    success=call.success,
                    error_code=call.error_code,
                    error_message=call.error_message,
                    latency_ms=call.latency_ms,
                )
                for call in result.tool_calls
            ],
            llm_traces=(
                [
                    AgentLlmTrace(
                        model=result.llm_trace.model,
                        prompt_version=result.llm_trace.prompt_version,
                        input_messages=result.llm_trace.input_messages,
                        output_message=result.llm_trace.output_message,
                        token_usage=result.llm_trace.token_usage,
                        latency_ms=result.llm_trace.latency_ms,
                    )
                ]
                if result.llm_trace is not None
                else []
            ),
        )
        with self._session_factory() as session:
            session.add(diagnosis)
            session.commit()
            diagnosis_id = diagnosis.id

        result.diagnosis_id = diagnosis_id
        return diagnosis_id
