from logistics_agent_service.domain.enums import OrderStatus
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine
from logistics_agent_service.domain.shipment_consistency import (
    all_cancelled,
    has_live_shipment,
    has_non_delivered,
)

_engine = RuleBasedDiagnosisEngine()


# --- 도메인 헬퍼 ---


def test_has_live_shipment_detects_live_statuses() -> None:
    assert has_live_shipment(["HUB_WAITING"])
    assert has_live_shipment(["DELIVERED", "COMPANY_MOVING"])
    assert not has_live_shipment(["DELIVERED", "CANCELLED"])
    # 알 수 없는 상태는 살아있다고 보지 않는다(보수적).
    assert not has_live_shipment(["IN_TRANSIT"])


def test_all_cancelled_requires_nonempty_and_all_cancelled() -> None:
    assert all_cancelled(["CANCELLED"])
    assert all_cancelled(["CANCELLED", "CANCELLED"])
    assert not all_cancelled([])
    assert not all_cancelled(["CANCELLED", "HUB_MOVING"])
    # 알 수 없는 상태가 섞이면 '전부 취소'로 확정하지 않는다.
    assert not all_cancelled(["CANCELLED", "IN_TRANSIT"])


def test_has_non_delivered_ignores_unknown() -> None:
    assert has_non_delivered(["HUB_MOVING"])
    assert has_non_delivered(["CANCELLED"])
    assert not has_non_delivered(["DELIVERED"])
    assert not has_non_delivered(["DELIVERED", "DELIVERED"])
    # 알 수 없는 상태만 있으면 미완료로 확정하지 않는다(보수적).
    assert not has_non_delivered(["IN_TRANSIT"])


# --- 규칙 A: CONFIRMED + 배송 전부 취소 ---


def test_confirmed_all_cancelled_is_risk() -> None:
    diagnosis = _engine.diagnose(OrderStatus.CONFIRMED, ["CANCELLED"], route_ok=True)

    assert diagnosis.diagnosis_status.value == "RISK_DETECTED"
    assert diagnosis.compensation_status.value == "NOT_REQUIRED"
    assert diagnosis.recommended_actions[0].action_type == "CHECK_SHIPMENT_CANCELLATION"
    assert diagnosis.recommended_actions[0].risk_level.value == "READ_ONLY"


def test_confirmed_partial_cancelled_is_not_risk_a() -> None:
    # 일부만 취소면 A 미해당(혼합 케이스는 후속). 살아있는 배송 + 경로 정상 → NORMAL.
    diagnosis = _engine.diagnose(
        OrderStatus.CONFIRMED, ["CANCELLED", "HUB_MOVING"], route_ok=True
    )

    assert diagnosis.diagnosis_status.value == "NORMAL"


def test_confirmed_missing_shipment_still_takes_precedence() -> None:
    # 배송 없음(개수 0)은 A보다 먼저 판정된다.
    diagnosis = _engine.diagnose(OrderStatus.CONFIRMED, [], route_ok=True)

    assert diagnosis.diagnosis_status.value == "MANUAL_INTERVENTION_REQUIRED"


# --- 규칙 B: CANCELLED + 진행 중 배송 ---


def test_cancelled_with_live_shipment_is_risk() -> None:
    diagnosis = _engine.diagnose(OrderStatus.CANCELLED, ["HUB_MOVING"])

    assert diagnosis.diagnosis_status.value == "RISK_DETECTED"
    assert diagnosis.recommended_actions[0].action_type == "CHECK_ORPHAN_SHIPMENT"


def test_cancelled_with_terminal_shipment_is_normal() -> None:
    diagnosis = _engine.diagnose(OrderStatus.CANCELLED, ["CANCELLED"])

    assert diagnosis.diagnosis_status.value == "NORMAL"


def test_cancelled_without_shipment_is_normal() -> None:
    diagnosis = _engine.diagnose(OrderStatus.CANCELLED, None)

    assert diagnosis.diagnosis_status.value == "NORMAL"


# --- 규칙 C: COMPLETED + 배송 미완료 ---


def test_completed_with_undelivered_shipment_is_risk() -> None:
    diagnosis = _engine.diagnose(OrderStatus.COMPLETED, ["HUB_MOVING"])

    assert diagnosis.diagnosis_status.value == "RISK_DETECTED"
    assert diagnosis.recommended_actions[0].action_type == "CHECK_INCOMPLETE_SHIPMENT"


def test_completed_all_delivered_is_normal() -> None:
    diagnosis = _engine.diagnose(OrderStatus.COMPLETED, ["DELIVERED"])

    assert diagnosis.diagnosis_status.value == "NORMAL"


def test_completed_cancelled_shipment_is_risk() -> None:
    # 취소된 배송도 '완료 아님'으로 본다.
    diagnosis = _engine.diagnose(OrderStatus.COMPLETED, ["CANCELLED"])

    assert diagnosis.diagnosis_status.value == "RISK_DETECTED"


# --- 강등: 알 수 없는 상태는 오판하지 않는다 ---


def test_unknown_shipment_status_does_not_trigger_consistency_rules() -> None:
    assert _engine.diagnose(OrderStatus.CANCELLED, ["IN_TRANSIT"]).diagnosis_status.value == (
        "NORMAL"
    )
    assert _engine.diagnose(OrderStatus.COMPLETED, ["IN_TRANSIT"]).diagnosis_status.value == (
        "NORMAL"
    )
    # CONFIRMED + 알 수 없는 상태 + 경로 정상 → 여전히 NORMAL.
    assert _engine.diagnose(
        OrderStatus.CONFIRMED, ["IN_TRANSIT"], route_ok=True
    ).diagnosis_status.value == "NORMAL"
