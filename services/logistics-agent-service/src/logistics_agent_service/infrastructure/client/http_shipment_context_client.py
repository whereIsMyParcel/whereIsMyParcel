from uuid import UUID

import httpx

from logistics_agent_service.application.dto import ShipmentInfo


class HttpShipmentContextClient:
    """ShipmentContextPort의 shipment-service HTTP 구현.

    `GET /internal/v1/shipments/{orderId}` → `ApiResponse<List<ShipmentInfoResponse>>`.
    조회 실패(전송 오류·5xx)는 `None`(unavailable)로 강등해 진단이 죽지 않게 한다.
    404는 "배송 없음"(`[]`)으로 본다.
    """

    def __init__(self, client: httpx.Client) -> None:
        self._client = client

    def get_shipments(self, order_id: UUID) -> list[ShipmentInfo] | None:
        try:
            response = self._client.get(f"/internal/v1/shipments/{order_id}")
        except httpx.HTTPError:
            return None

        if response.status_code == httpx.codes.NOT_FOUND:
            return []
        if response.is_error:
            return None

        body = response.json()
        data = body.get("data")
        if not body.get("success") or data is None:
            return None

        return [
            ShipmentInfo(
                status=item["shipmentStatus"],
                origin_hub_id=item.get("originHubId"),
                destination_hub_id=item.get("destinationHubId"),
            )
            for item in data
        ]
