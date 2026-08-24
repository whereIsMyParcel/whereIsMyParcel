"""주문↔배송 상태 정합성 판정 헬퍼(design §7, §16.3).

배송 상태 문자열을 shipment-service 계약(ShipmentStatus)으로 해석해 교차 정합성
판정에 쓴다. 알 수 없는 상태는 보수적으로 무시한다(contract drift에 견고 — 규칙이
오판하지 않게, §7).

배송이 여러 건이고 상태가 섞이는 혼합 케이스(부분 취소/부분 배송)를 '전부(all)'와
'일부(partial)'로 구분하기 위해, 배송 목록을 한 번 훑어 카테고리별로 세는
ShipmentSetSummary를 1차 출처로 둔다. 개별 술어는 그 위에서 파생된다(§16.3).
"""

from dataclasses import dataclass

from logistics_agent_service.domain.enums import OrderStatus, ShipmentStatus

# 진행 중(살아있는) 배송 상태. 종료 상태는 DELIVERED / CANCELLED.
_LIVE_STATUSES = frozenset(
    {
        ShipmentStatus.HUB_WAITING,
        ShipmentStatus.HUB_MOVING,
        ShipmentStatus.HUB_ARRIVED,
        ShipmentStatus.COMPANY_MOVING,
    }
)


def _parse(status: str) -> ShipmentStatus | None:
    try:
        return ShipmentStatus(status)
    except ValueError:
        return None


@dataclass(frozen=True)
class ShipmentSetSummary:
    """배송 목록을 카테고리별 개수로 요약한 값 객체(§16.3).

    알 수 없는 상태(계약 drift)는 unknown으로 격리해 정합성 판정에서 오판하지 않게 한다.
    """

    live: int
    delivered: int
    cancelled: int
    unknown: int

    @property
    def total(self) -> int:
        return self.live + self.delivered + self.cancelled + self.unknown

    @property
    def all_cancelled(self) -> bool:
        """비어 있지 않고 모든 배송이 CANCELLED(알 수 없는 상태가 섞이면 False)."""
        return self.total > 0 and self.cancelled == self.total

    @property
    def has_live(self) -> bool:
        """진행 중(살아있는) 배송이 하나라도 있으면 True."""
        return self.live > 0

    @property
    def has_non_delivered(self) -> bool:
        """DELIVERED가 아닌 '알려진' 배송이 하나라도 있으면 True(취소도 미완료로 본다)."""
        return self.live > 0 or self.cancelled > 0

    @property
    def partially_cancelled(self) -> bool:
        """일부만 취소(전부 취소는 아님)이고 나머지 배송이 생존/배달됐으면 True.

        전부 취소(all_cancelled)와 구분되는 혼합 케이스. 알 수 없는 상태만 남은
        경우는 '나머지 생존'으로 보지 않는다(보수적).
        """
        return self.cancelled > 0 and (self.live + self.delivered) > 0

    @property
    def partially_delivered(self) -> bool:
        """일부는 배달(DELIVERED)됐고 나머지에 미완료가 섞였으면 True(부분 배송)."""
        return self.delivered > 0 and self.has_non_delivered


def summarize(shipment_statuses: list[str]) -> ShipmentSetSummary:
    """배송 상태 목록을 카테고리별 개수로 요약한다(순수 함수)."""
    live = delivered = cancelled = unknown = 0
    for status in shipment_statuses:
        parsed = _parse(status)
        if parsed in _LIVE_STATUSES:
            live += 1
        elif parsed is ShipmentStatus.DELIVERED:
            delivered += 1
        elif parsed is ShipmentStatus.CANCELLED:
            cancelled += 1
        else:
            unknown += 1
    return ShipmentSetSummary(
        live=live, delivered=delivered, cancelled=cancelled, unknown=unknown
    )


def has_live_shipment(shipment_statuses: list[str]) -> bool:
    """진행 중(살아있는) 배송이 하나라도 있으면 True.

    주문 CANCELLED인데 배송이 진행 중인 orphan 배송 판정용(B). 알 수 없는 상태는
    살아있다고 보지 않는다(보수적).
    """
    return summarize(shipment_statuses).has_live


def is_orphan_shipment(
    order_status: OrderStatus | None, shipment_statuses: list[str]
) -> bool:
    """주문이 CANCELLED인데 살아있는 배송이 남아 있으면 True(§16.3 규칙 B).

    recovery 실행 직전 재검증에도 쓴다(§16.4 T5b): 제안↔승인 사이 상태가 바뀌어
    orphan이 해소됐으면 실제 배송 취소를 하지 않기 위함이다.
    """
    return order_status is OrderStatus.CANCELLED and has_live_shipment(shipment_statuses)


def all_cancelled(shipment_statuses: list[str]) -> bool:
    """비어 있지 않고 모든 배송이 CANCELLED이면 True.

    주문 CONFIRMED인데 배송이 전부 취소된 정합성 이상 판정용(A). 알 수 없는 상태가
    섞이면 False(명확한 케이스만 확정).
    """
    return summarize(shipment_statuses).all_cancelled


def has_non_delivered(shipment_statuses: list[str]) -> bool:
    """DELIVERED가 아닌 '알려진' 배송 상태가 하나라도 있으면 True.

    주문 COMPLETED인데 배송이 미완료인 정합성 이상 판정용(C). 취소된 배송도
    '미완료(=배송 완료 아님)'로 본다. 알 수 없는 상태는 무시(보수적).
    """
    return summarize(shipment_statuses).has_non_delivered
