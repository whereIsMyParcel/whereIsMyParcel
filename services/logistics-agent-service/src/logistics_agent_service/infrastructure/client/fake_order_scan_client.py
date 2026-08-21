from uuid import UUID

from logistics_agent_service.domain.enums import OrderStatus


class FakeOrderScanClient:
    """OrderScanPort의 in-memory 구현(DB/네트워크 없이 실행·테스트용).

    상태별 orderId 매핑을 주입받아 그대로 돌려준다. 기본값은 빈 매핑이라
    로컬/CI에서 스캔이 아무 후보도 진단하지 않는다(부작용 없음).
    """

    def __init__(self, by_status: dict[OrderStatus, list[UUID]] | None = None) -> None:
        self._by_status = by_status or {}

    def list_order_ids_by_status(self, status: OrderStatus) -> list[UUID]:
        return list(self._by_status.get(status, []))
