from uuid import UUID

import httpx

from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.domain.enums import OrderStatus


def _as_uuid(identifier: str) -> UUID | None:
    try:
        return UUID(identifier)
    except (ValueError, AttributeError):
        return None


def _as_order_status(value: str) -> OrderStatus | None:
    """알 수 없는 상태 값은 crash 대신 None으로 흡수한다(→ UNKNOWN 진단).

    agent가 미러링한 OrderStatus와 order-service 계약이 어긋나도(신규 상태 등)
    진단 요청이 죽지 않게 한다.
    """
    try:
        return OrderStatus(value)
    except ValueError:
        return None


class HttpOrderContextClient:
    """OrderContextPort의 order-service HTTP 구현.

    `GET /internal/v1/orders/{orderId}`를 호출한다. system header는 주입된
    httpx.Client의 기본 헤더로 전달한다(§8.3 service account).

    현재 order 내부 API는 orderId(UUID)로만 조회 가능하므로, orderNumber 식별자는
    해석하지 않고 None을 반환한다(orderNumber 조회는 후속 슬라이스).
    """

    def __init__(self, client: httpx.Client) -> None:
        self._client = client

    def get_order_context(self, identifier: str) -> OrderContext | None:
        order_id = _as_uuid(identifier)
        if order_id is None:
            return None

        response = self._client.get(f"/internal/v1/orders/{order_id}")
        if response.status_code == httpx.codes.NOT_FOUND:
            return None
        response.raise_for_status()

        body = response.json()
        data = body.get("data")
        if not body.get("success") or data is None:
            return None

        return OrderContext(
            order_id=data["orderId"],
            order_number=data["orderNumber"],
            order_status=_as_order_status(data["orderStatus"]),
        )
