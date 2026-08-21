from uuid import UUID

import httpx

from logistics_agent_service.domain.enums import OrderStatus


def _as_uuid(value: object) -> UUID | None:
    try:
        return UUID(str(value))
    except (ValueError, AttributeError, TypeError):
        return None


class HttpOrderScanClient:
    """OrderScanPort의 order-service HTTP 구현.

    `GET /internal/v1/orders?status=...`(S14 계약)를 호출해 상태별 고장 후보
    orderId를 열거한다. system header는 주입된 httpx.Client 기본 헤더로 전달한다
    (§8.3 service account).

    조회 실패(전송 오류·5xx·business-failure)는 빈 목록으로 강등해, order-service
    장애가 스캔 루프를 함께 죽이지 않게 한다. 파싱 불가한 orderId는 건너뛴다.
    """

    def __init__(self, client: httpx.Client) -> None:
        self._client = client

    def list_order_ids_by_status(self, status: OrderStatus) -> list[UUID]:
        try:
            response = self._client.get(
                "/internal/v1/orders", params={"status": status.value}
            )
        except httpx.HTTPError:
            return []

        if response.is_error:
            return []

        body = response.json()
        data = body.get("data")
        if not body.get("success") or data is None:
            return []

        order_ids = data.get("orderIds") or []
        return [parsed for raw in order_ids if (parsed := _as_uuid(raw)) is not None]
