"""주문↔배송 상태 정합성 판정 헬퍼(design §7, T2).

배송 상태 문자열을 shipment-service 계약(ShipmentStatus)으로 해석해 교차 정합성
판정에 쓰는 술어를 제공한다. 알 수 없는 상태는 보수적으로 무시한다(contract
drift에 견고 — 규칙이 오판하지 않게, §7).
"""

from logistics_agent_service.domain.enums import ShipmentStatus

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


def has_live_shipment(shipment_statuses: list[str]) -> bool:
    """진행 중(살아있는) 배송이 하나라도 있으면 True.

    주문 CANCELLED인데 배송이 진행 중인 orphan 배송 판정용(B). 알 수 없는 상태는
    살아있다고 보지 않는다(보수적).
    """
    return any(_parse(status) in _LIVE_STATUSES for status in shipment_statuses)


def all_cancelled(shipment_statuses: list[str]) -> bool:
    """비어 있지 않고 모든 배송이 CANCELLED이면 True.

    주문 CONFIRMED인데 배송이 전부 취소된 정합성 이상 판정용(A). 알 수 없는 상태가
    섞이면 False(명확한 케이스만 확정).
    """
    if not shipment_statuses:
        return False
    return all(_parse(status) is ShipmentStatus.CANCELLED for status in shipment_statuses)


def has_non_delivered(shipment_statuses: list[str]) -> bool:
    """DELIVERED가 아닌 '알려진' 배송 상태가 하나라도 있으면 True.

    주문 COMPLETED인데 배송이 미완료인 정합성 이상 판정용(C). 취소된 배송도
    '미완료(=배송 완료 아님)'로 본다. 알 수 없는 상태는 무시(보수적).
    """
    return any(
        (parsed := _parse(status)) is not None and parsed is not ShipmentStatus.DELIVERED
        for status in shipment_statuses
    )
