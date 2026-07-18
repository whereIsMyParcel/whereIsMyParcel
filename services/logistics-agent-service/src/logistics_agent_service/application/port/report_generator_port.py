from typing import Protocol

from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.domain.models import Diagnosis


class ReportGeneratorPort(Protocol):
    """진단 결과로 운영자용 리포트를 생성하는 포트.

    S1은 템플릿 스텁 구현, S4에서 Gemini(Spring AI ChatModel 대응) 구현으로 교체한다.
    """

    def generate(self, diagnosis: Diagnosis, order_context: OrderContext | None) -> str: ...
