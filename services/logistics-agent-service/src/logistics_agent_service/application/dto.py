from uuid import UUID

from pydantic import BaseModel

from logistics_agent_service.domain.enums import OrderStatus, TriggerType
from logistics_agent_service.domain.models import Diagnosis


class OrderContext(BaseModel):
    """order-service 내부 API(GET /internal/v1/orders/{orderId})가 반환하는
    OrderAiContextResponse 중 진단에 필요한 부분만 담는다."""

    order_id: UUID
    order_number: str
    order_status: OrderStatus | None


class DiagnosisQuery(BaseModel):
    message: str
    trigger_type: TriggerType = TriggerType.USER_QUERY
    # incident처럼 orderId를 이미 아는 경우 message 정규식 추출을 건너뛴다.
    order_identifier: str | None = None


class DiagnosisResult(BaseModel):
    diagnosis: Diagnosis
    report: str
    trigger_type: TriggerType = TriggerType.USER_QUERY
    order_id: UUID | None = None
    order_number: str | None = None
    diagnosis_id: UUID | None = None
