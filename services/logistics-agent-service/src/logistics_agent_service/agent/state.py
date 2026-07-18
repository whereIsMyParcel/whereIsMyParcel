from typing import TypedDict

from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.domain.models import Diagnosis


class DiagnosisState(TypedDict, total=False):
    """LangGraph 진단 워크플로우의 공유 상태."""

    message: str
    order_identifier: str | None
    order_context: OrderContext | None
    diagnosis: Diagnosis
    report: str
