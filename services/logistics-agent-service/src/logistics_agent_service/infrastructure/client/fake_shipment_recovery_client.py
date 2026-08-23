from uuid import UUID


class FakeShipmentRecoveryClient:
    """RecoveryActionPort의 In-Memory Fake(테스트/로컬용).

    실제 write 대신 호출된 orderId를 기록한다. succeed로 성공/실패를 제어한다.
    """

    def __init__(self, succeed: bool = True) -> None:
        self._succeed = succeed
        self.cancelled: list[UUID] = []

    def cancel_orphan_shipment(self, order_id: UUID) -> bool:
        self.cancelled.append(order_id)
        return self._succeed
