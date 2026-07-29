from uuid import UUID


class FakeShipmentContextClient:
    """서비스 없이 실행/테스트할 때 쓰는 ShipmentContextPort 구현.

    기본값은 배송 1건(`["IN_TRANSIT"]`)이라, Fake 모드에서 CONFIRMED 주문이
    배송 누락으로 오진되지 않는다. 특정 시나리오는 statuses로 주입한다.
    """

    def __init__(self, statuses: list[str] | None = None) -> None:
        self._statuses = ["IN_TRANSIT"] if statuses is None else statuses

    def get_shipment_statuses(self, order_id: UUID) -> list[str] | None:
        return list(self._statuses)
