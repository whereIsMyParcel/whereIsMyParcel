import logging
from uuid import UUID

import httpx

logger = logging.getLogger(__name__)


class HttpShipmentRecoveryClient:
    """RecoveryActionPort의 shipment-service HTTP 구현(design §16.4 T5b).

    `POST /internal/v1/shipments/cancel`(body `{orderId}`)로 주문의 배송을 취소한다.
    system header(§8.3, X-User-Role=MASTER)는 주입된 httpx.Client 기본 헤더로 전달된다.
    이 서비스 최초의 실제 write다. 전송 오류·5xx·business-failure는 False로 강등해
    호출측이 FAILED 상태로 기록하게 한다(crash 대신).
    """

    def __init__(self, client: httpx.Client) -> None:
        self._client = client

    def cancel_orphan_shipment(self, order_id: UUID) -> bool:
        try:
            response = self._client.post(
                "/internal/v1/shipments/cancel", json={"orderId": str(order_id)}
            )
        except httpx.HTTPError as exc:
            logger.warning("배송 취소 호출 실패(전송 오류): %s", exc)
            return False

        if response.is_error:
            logger.warning("배송 취소 호출 실패(status=%s)", response.status_code)
            return False

        body = response.json()
        return bool(body.get("success"))
