from typing import Protocol
from uuid import UUID

from logistics_agent_service.domain.enums import OrderStatus


class OrderScanPort(Protocol):
    """상태 기준 고장 후보 orderId 열거 포트(read-only, scheduled scan §16.2).

    실제 구현은 order-service 내부 API(GET /internal/v1/orders?status=...)를 호출한다.
    조회 실패(전송 오류·5xx·business-failure)는 빈 목록으로 강등해, order-service
    장애가 스캔 루프를 함께 죽이지 않게 한다.
    """

    def list_order_ids_by_status(self, status: OrderStatus) -> list[UUID]: ...
