import uuid

from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.domain.enums import OrderStatus

_NAMESPACE = uuid.UUID("00000000-0000-0000-0000-0000000000aa")

# S1 데모/테스트용 시나리오. orderNumber 접미사가 상태를 암시한다.
_SAMPLES: dict[str, OrderStatus] = {
    "ORD-20260718-FAILED01": OrderStatus.FAILED,
    "ORD-20260718-COMPFAIL": OrderStatus.COMPENSATION_FAILED,
    "ORD-20260718-CONFIRM1": OrderStatus.CONFIRMED,
    "ORD-20260718-PENDING1": OrderStatus.PENDING,
}


def _build_context(order_number: str, status: OrderStatus) -> OrderContext:
    return OrderContext(
        order_id=uuid.uuid5(_NAMESPACE, order_number),
        order_number=order_number,
        order_status=status,
    )


class FakeOrderContextClient:
    """S1용 in-memory OrderContextPort 구현.

    실제 order-service 호출(GET /internal/v1/orders/{orderId})은 S3에서 대체한다.
    orderNumber 또는 orderId(문자열) 어느 쪽으로도 조회할 수 있도록 두 키로 색인한다.
    """

    def __init__(self, contexts: dict[str, OrderContext] | None = None) -> None:
        if contexts is not None:
            self._contexts = contexts
            return
        self._contexts = {}
        for order_number, status in _SAMPLES.items():
            context = _build_context(order_number, status)
            self._contexts[order_number] = context
            self._contexts[str(context.order_id)] = context

    def get_order_context(self, identifier: str) -> OrderContext | None:
        return self._contexts.get(identifier)
