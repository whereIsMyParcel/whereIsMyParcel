from typing import TypedDict

from logistics_agent_service.application.dto import (
    DiagnosisResult,
    LlmTrace,
    OrderContext,
    ToolCallRecord,
)
from logistics_agent_service.domain.enums import TriggerType
from logistics_agent_service.domain.models import Diagnosis


class DiagnosisState(TypedDict, total=False):
    """LangGraph 진단 워크플로우의 공유 상태."""

    message: str
    trigger_type: TriggerType
    order_identifier: str | None
    order_context: OrderContext | None
    shipment_statuses: list[str] | None
    route_ok: bool | None
    diagnosis: Diagnosis
    report: str
    # 자기관측 telemetry: diagnosis_id 부여(persist) 전에 발생하므로 state에 버퍼링한다.
    tool_calls: list[ToolCallRecord]
    llm_trace: LlmTrace | None
    result: DiagnosisResult
