from typing import Protocol
from uuid import UUID


class DiagnosedOrderPort(Protocol):
    """이미 진단된 orderId 집합 조회 포트(read-only).

    scheduled scan(§16.2)이 중복 진단을 피하려고 조회한다. write용
    DiagnosisRepositoryPort와 책임을 분리하며, 구현은 진단 저장소 위에 배선한다.
    """

    def list_diagnosed_order_ids(self) -> set[UUID]: ...
