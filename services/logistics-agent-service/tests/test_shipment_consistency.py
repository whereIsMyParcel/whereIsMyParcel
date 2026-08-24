import pytest

from logistics_agent_service.domain.enums import OrderStatus
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine
from logistics_agent_service.domain.shipment_consistency import (
    all_cancelled,
    has_live_shipment,
    has_non_delivered,
    summarize,
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


def test_confirmed_partial_cancelled_is_soft_risk() -> None:
    # 일부만 취소 + 나머지 생존 → 부분 취소 소프트 RISK(§16.3). 경로 정상이어도
    # 정합성 이상이 우선한다.
    diagnosis = _engine.diagnose(
        OrderStatus.CONFIRMED, ["CANCELLED", "HUB_MOVING"], route_ok=True
    )

    assert diagnosis.diagnosis_status.value == "RISK_DETECTED"
    assert diagnosis.recommended_actions[0].action_type == "CHECK_SHIPMENT_CANCELLATION"
    # 전부 취소(0.7)보다 약한 신호.
    assert diagnosis.confidence == 0.6


def test_confirmed_partial_cancelled_with_delivered_is_soft_risk() -> None:
    # 일부 취소 + 나머지 배달 완료 → 부분 취소로 잡힌다.
    diagnosis = _engine.diagnose(
        OrderStatus.CONFIRMED, ["CANCELLED", "DELIVERED"], route_ok=True
    )

    assert diagnosis.diagnosis_status.value == "RISK_DETECTED"
    assert diagnosis.confidence == 0.6


def test_confirmed_missing_shipment_still_takes_precedence() -> None:
    # 배송 없음(개수 0)은 A보다 먼저 판정된다.
    diagnosis = _engine.diagnose(OrderStatus.CONFIRMED, [], route_ok=True)

    assert diagnosis.diagnosis_status.value == "MANUAL_INTERVENTION_REQUIRED"


# --- 규칙 B: CANCELLED + 진행 중 배송 ---


def test_cancelled_with_live_shipment_is_risk() -> None:
    diagnosis = _engine.diagnose(OrderStatus.CANCELLED, ["HUB_MOVING"])

    assert diagnosis.diagnosis_status.value == "RISK_DETECTED"
    # orphan 배송은 승인 기반 복구 제안(RECOVERY_WRITE, 승인 필요)을 낸다(§16.4 T5a).
    action = diagnosis.recommended_actions[0]
    assert action.action_type == "CANCEL_ORPHAN_SHIPMENT"
    assert action.risk_level.value == "RECOVERY_WRITE"
    assert action.requires_approval is True


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
    # 취소된 배송도 '완료 아님'으로 본다. 배달 0 + 취소만 → 명백 이상(0.6).
    diagnosis = _engine.diagnose(OrderStatus.COMPLETED, ["CANCELLED"])

    assert diagnosis.diagnosis_status.value == "RISK_DETECTED"
    assert diagnosis.confidence == 0.6


def test_completed_with_live_shipment_is_strong_risk() -> None:
    # 완료인데 진행 중 배송 잔존 → 강한 신호(0.7).
    diagnosis = _engine.diagnose(OrderStatus.COMPLETED, ["DELIVERED", "HUB_MOVING"])

    assert diagnosis.diagnosis_status.value == "RISK_DETECTED"
    assert diagnosis.recommended_actions[0].action_type == "CHECK_INCOMPLETE_SHIPMENT"
    assert diagnosis.confidence == 0.7


def test_completed_partial_cancel_is_milder_risk() -> None:
    # 일부 배달 + 일부 취소(진행 중 없음) → 정상 부분 취소 가능성, 약한 신호(0.5).
    diagnosis = _engine.diagnose(OrderStatus.COMPLETED, ["DELIVERED", "CANCELLED"])

    assert diagnosis.diagnosis_status.value == "RISK_DETECTED"
    assert diagnosis.confidence == 0.5


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


# --- ShipmentSetSummary 값 객체(§16.3) ---


def test_summarize_categorizes_and_isolates_unknown() -> None:
    s = summarize(["HUB_MOVING", "DELIVERED", "CANCELLED", "IN_TRANSIT"])
    assert (s.live, s.delivered, s.cancelled, s.unknown) == (1, 1, 1, 1)
    assert s.total == 4


@pytest.mark.parametrize(
    ("statuses", "all_c", "live", "non_deliv", "partial_c"),
    [
        (["CANCELLED"], True, False, True, False),
        (["CANCELLED", "CANCELLED"], True, False, True, False),
        (["CANCELLED", "HUB_MOVING"], False, True, True, True),
        (["CANCELLED", "DELIVERED"], False, False, True, True),
        (["DELIVERED", "DELIVERED"], False, False, False, False),
        (["DELIVERED", "HUB_MOVING"], False, True, True, False),
        # 알 수 없는 상태는 격리 → 전부 취소/부분 취소로 확정하지 않는다(보수적).
        (["CANCELLED", "IN_TRANSIT"], False, False, True, False),
        (["IN_TRANSIT"], False, False, False, False),
    ],
)
def test_summary_predicates(
    statuses: list[str],
    all_c: bool,
    live: bool,
    non_deliv: bool,
    partial_c: bool,
) -> None:
    s = summarize(statuses)
    assert s.all_cancelled is all_c
    assert s.has_live is live
    assert s.has_non_delivered is non_deliv
    assert s.partially_cancelled is partial_c
    # 자유 함수는 summary와 동일 결과여야 한다(하위호환).
    assert all_cancelled(statuses) is all_c
    assert has_live_shipment(statuses) is live
    assert has_non_delivered(statuses) is non_deliv
