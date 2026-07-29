from typing import TypedDict

from logistics_agent_service.application.dto import DiagnosisResult, OrderContext
from logistics_agent_service.domain.enums import TriggerType
from logistics_agent_service.domain.models import Diagnosis


class DiagnosisState(TypedDict, total=False):
    """LangGraph 진단 워크플로우의 공유 상태."""

    message: str
    trigger_type: TriggerType
    order_identifier: str | None
    order_context: OrderContext | None
    shipment_statuses: list[str] | None
    diagnosis: Diagnosis
    report: str
    result: DiagnosisResult
