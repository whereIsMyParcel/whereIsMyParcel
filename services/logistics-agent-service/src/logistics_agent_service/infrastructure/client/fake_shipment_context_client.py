import uuid
from uuid import UUID

from logistics_agent_service.application.dto import ShipmentInfo

# Fake 배송에 부여하는 고정 허브 쌍(경로 검증 시나리오용).
_ORIGIN_HUB = uuid.UUID("00000000-0000-0000-0000-0000000000b1")
_DESTINATION_HUB = uuid.UUID("00000000-0000-0000-0000-0000000000b2")


class FakeShipmentContextClient:
    """서비스 없이 실행/테스트할 때 쓰는 ShipmentContextPort 구현.

    기본값은 배송 1건(`["HUB_MOVING"]`, 진행 중)이라, Fake 모드에서 CONFIRMED 주문이
    배송 누락으로 오진되지 않는다. 특정 시나리오는 statuses로 주입한다.
    각 배송에는 고정 허브 쌍을 부여한다(경로 검증용).
    """

    def __init__(self, statuses: list[str] | None = None) -> None:
        source = ["HUB_MOVING"] if statuses is None else statuses
        self._shipments = [
            ShipmentInfo(
                status=status,
                origin_hub_id=_ORIGIN_HUB,
                destination_hub_id=_DESTINATION_HUB,
            )
            for status in source
        ]

    def get_shipments(self, order_id: UUID) -> list[ShipmentInfo] | None:
        return list(self._shipments)
