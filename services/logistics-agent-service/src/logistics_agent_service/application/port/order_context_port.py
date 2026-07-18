from typing import Protocol

from logistics_agent_service.application.dto import OrderContext


class OrderContextPort(Protocol):
    """주문 컨텍스트 조회 포트.

    실제 구현(S3)은 order-service 내부 API(GET /internal/v1/orders/{orderId})를 호출한다.
    S1에서는 in-memory Fake 구현을 사용한다.

    참고: 현재 order-service 내부 API는 orderId(UUID)로만 조회 가능하다. 사용자 질의가
    orderNumber(예: ORD-YYYYMMDD-XXXX)로 들어오는 경우를 위해 식별자를 문자열로 받는다.
    orderNumber -> order 해석은 S3에서 order-service 계약 확인이 필요하다.
    """

    def get_order_context(self, identifier: str) -> OrderContext | None: ...
