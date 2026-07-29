from uuid import UUID

import httpx

from logistics_agent_service.application.dto import OrderContext


def _as_uuid(identifier: str) -> UUID | None:
    try:
        return UUID(identifier)
    except (ValueError, AttributeError):
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
            order_status=data["orderStatus"],
        )
