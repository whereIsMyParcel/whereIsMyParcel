from typing import Protocol
from uuid import UUID


class RecoveryActionPort(Protocol):
    """승인된 recovery 조치를 실제 write로 실행하는 포트(design §16.4 T5b).

    이 서비스 최초의 실제 write다. 구현(HTTP)은 shipment-service
    `POST /internal/v1/shipments/cancel`을 호출한다. 성공 True, 실패 False로
    강등 반환한다(crash 대신 FAILED 상태로 기록되게).
    """

    def cancel_orphan_shipment(self, order_id: UUID) -> bool: ...
