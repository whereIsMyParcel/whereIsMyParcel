from logistics_agent_service.domain.enums import (
    CompensationStatus,
    DiagnosisStatus,
    FailureStep,
    OrderStatus,
)
from logistics_agent_service.domain.failure_signatures import classify_failure_step
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine
# order-service OrderCreateSaga가 실제로 남기는 ERROR 로그 라인 형태(맥락 포함).
_STOCK_RESERVE_FAIL = 'level=ERROR message="[Saga] 재고 예약 실패. orderId=abc"'
_SHIPMENT_CREATE_FAIL = 'level=ERROR message="[Saga] 배송 생성 실패. orderId=abc"'
_CONFIRM_SAVE_FAIL = (
    'level=ERROR message="[Saga] 배송 생성 후 주문 확정 저장 실패. orderId=abc"'
)
_SHIPMENT_CANCEL_COMP_FAIL = 'level=ERROR message="[Saga] 배송 취소 보상 실패. orderId=abc"'
_STOCK_ROLLBACK_COMP_FAIL = 'level=ERROR message="[Saga] 재고 원복 실패. orderId=abc"'


def test_signature_maps_each_saga_error_to_step() -> None:
    assert classify_failure_step([_STOCK_RESERVE_FAIL]) == FailureStep.INVENTORY_RESERVATION
    assert classify_failure_step([_SHIPMENT_CREATE_FAIL]) == FailureStep.SHIPMENT_CREATION
    assert classify_failure_step([_CONFIRM_SAVE_FAIL]) == FailureStep.ORDER_CREATION
    assert classify_failure_step([_SHIPMENT_CANCEL_COMP_FAIL]) == FailureStep.SHIPMENT_CREATION
    assert classify_failure_step([_STOCK_ROLLBACK_COMP_FAIL]) == FailureStep.INVENTORY_RESERVATION


def test_confirm_save_line_not_misread_as_shipment_creation() -> None:
    # "배송 생성 후 주문 확정 저장 실패"는 "배송 생성 실패" 시그니처와 겹치지 않아야 한다.
    assert classify_failure_step([_CONFIRM_SAVE_FAIL]) == FailureStep.ORDER_CREATION


def test_compensation_failure_takes_priority_over_original() -> None:
    # 원 실패(확정 저장)와 보상 실패(재고 원복)가 함께 있으면 orphan 쪽으로 단계를 특정한다.
    lines = [_STOCK_ROLLBACK_COMP_FAIL, _CONFIRM_SAVE_FAIL]
    assert classify_failure_step(lines) == FailureStep.INVENTORY_RESERVATION


def test_no_logs_or_no_match_degrades_to_unknown() -> None:
    assert classify_failure_step(None) == FailureStep.UNKNOWN
    assert classify_failure_step([]) == FailureStep.UNKNOWN
    assert classify_failure_step(['level=INFO message="주문 정상 처리"']) == FailureStep.UNKNOWN


def test_engine_refines_failed_step_from_logs() -> None:
    engine = RuleBasedDiagnosisEngine()

    compfail = engine.diagnose(
        OrderStatus.COMPENSATION_FAILED, None, None, [_STOCK_ROLLBACK_COMP_FAIL]
    )
    assert compfail.failed_step == FailureStep.INVENTORY_RESERVATION

    failed = engine.diagnose(OrderStatus.FAILED, [], None, [_STOCK_RESERVE_FAIL])
    assert failed.failed_step == FailureStep.INVENTORY_RESERVATION


def test_logs_do_not_change_status_only_failed_step() -> None:
    engine = RuleBasedDiagnosisEngine()
    result = engine.diagnose(
        OrderStatus.COMPENSATION_FAILED, None, None, [_SHIPMENT_CANCEL_COMP_FAIL]
    )
    # 판정(§7.1)은 그대로, 단계만 좁혀진다.
    assert result.diagnosis_status == DiagnosisStatus.FAILED_COMPENSATION_FAILED
    assert result.compensation_status == CompensationStatus.FAILED
    assert result.failed_step == FailureStep.SHIPMENT_CREATION


def test_no_logs_keeps_prior_unknown_behavior() -> None:
    # 회귀: 로그 없이 호출하던 기존 경로는 여전히 UNKNOWN 단계를 낸다.
    engine = RuleBasedDiagnosisEngine()
    assert engine.diagnose(OrderStatus.FAILED, []).failed_step == FailureStep.UNKNOWN
    assert (
        engine.diagnose(OrderStatus.COMPENSATION_FAILED, None).failed_step
        == FailureStep.UNKNOWN
    )
