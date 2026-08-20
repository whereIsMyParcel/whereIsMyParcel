from logistics_agent_service.domain.enums import FailureStep

# order-service OrderCreateSaga가 실패 시 남기는 ERROR 로그 문구 → FailureStep 매핑.
# 판정 출처는 Order 상태(§7.1)이고, 로그는 실패가 일어난 "단계"만 좁힌다(§5, design §16.1).
#
# 우선순위: 보상 실패(orphan 잔존) 시그니처를 원 실패보다 앞에 둔다.
# COMPENSATION_FAILED에서는 어떤 리소스가 남았는지(잔여 예약/배송)가 더 actionable하므로,
# 원 실패 라인과 보상 실패 라인이 함께 있으면 보상 실패 쪽으로 단계를 특정한다.
_SIGNATURES: list[tuple[str, FailureStep]] = [
    # 보상 실패(잔여 orphan) — 우선
    ("재고 원복 실패", FailureStep.INVENTORY_RESERVATION),  # 잔여 예약 재고
    ("배송 취소 보상 실패", FailureStep.SHIPMENT_CREATION),  # 잔여 배송
    # 원 실패
    ("재고 예약 실패", FailureStep.INVENTORY_RESERVATION),
    ("주문 확정 저장 실패", FailureStep.ORDER_CREATION),
    ("배송 생성 실패", FailureStep.SHIPMENT_CREATION),
]


def classify_failure_step(log_lines: list[str] | None) -> FailureStep:
    """saga ERROR 로그 라인에서 실패 단계를 특정한다.

    로그가 없거나 알려진 시그니처와 매칭되지 않으면 UNKNOWN으로 강등한다(부작용 없음).
    """
    if not log_lines:
        return FailureStep.UNKNOWN
    for needle, step in _SIGNATURES:
        if any(needle in line for line in log_lines):
            return step
    return FailureStep.UNKNOWN
