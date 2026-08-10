from typing import Protocol
from uuid import UUID

from logistics_agent_service.application.dto import ShipmentInfo


class ShipmentContextPort(Protocol):
    """주문의 배송 정보 조회 포트.

    반환:
    - `list[ShipmentInfo]`: 배송 목록. 빈 리스트면 해당 주문에 배송이 없음.
    - `None`: 조회 불가(서비스 오류/타임아웃 등). 진단은 배송 근거 없이 강등된다.
    """

    def get_shipments(self, order_id: UUID) -> list[ShipmentInfo] | None: ...
