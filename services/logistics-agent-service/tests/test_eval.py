from pathlib import Path

from logistics_agent_service.application.eval import (
    EvalRunner,
    EvalSample,
    _writes_gated_ok,
)
from logistics_agent_service.core.eval_cli import main
from logistics_agent_service.domain.enums import (
    ActionRiskLevel,
    DiagnosisStatus,
    OrderStatus,
)
from logistics_agent_service.domain.models import Diagnosis, RecommendedAction
from logistics_agent_service.infrastructure.eval.jsonl_loader import load_samples

_SEED = (
    Path(__file__).resolve().parent.parent
    / "src"
    / "logistics_agent_service"
    / "eval_data"
    / "diagnosis_seed.jsonl"
)


def test_loader_reads_seed_dataset() -> None:
    samples = load_samples(_SEED)

    assert len(samples) == 19
    names = {s.name for s in samples}
    assert "compensation_failed" in names
    assert "order_unknown" in names
    assert "confirmed_invalid_route" in names
    assert "confirmed_all_shipments_cancelled" in names
    assert "cancelled_with_live_shipment" in names
    assert "completed_shipment_not_delivered" in names
    assert "failed_step_inventory_reservation" in names
    assert "compfail_compensation_priority" in names


def test_runner_all_seed_cases_pass() -> None:
    report = EvalRunner().run(load_samples(_SEED))

    assert report.total == 19
    assert report.diagnosis_accuracy == 1.0
    assert report.compensation_accuracy == 1.0
    # failed_step은 expected가 선언된 케이스(로그 기반)만 분모에 든다.
    assert report.failed_step_total == 5
    assert report.failed_step_accuracy == 1.0
    # 불변식은 전 케이스에서 성립해야 한다.
    assert report.writes_gated_correct == report.total
    assert report.grounding_correct == report.total
    assert report.all_passed


def test_runner_counts_mismatch() -> None:
    samples = [
        EvalSample(
            name="wrong_label",
            order_status="FAILED",
            shipment_statuses=None,
            expected_diagnosis_status="NORMAL",  # 실제는 FAILED_COMPENSATED
            expected_compensation_status="COMPLETED",
        ),
    ]

    report = EvalRunner().run(samples)

    assert report.total == 1
    assert report.diagnosis_correct == 0
    assert report.compensation_correct == 1
    assert not report.all_passed
    assert report.cases[0].actual_diagnosis_status.value == "FAILED_COMPENSATED"


def test_failed_step_scored_only_when_expected_declared() -> None:
    # expected_failed_step 미선언 → failed_step 분모에서 제외.
    no_label = EvalSample(
        name="no_failed_step_label",
        order_status="FAILED",
        expected_diagnosis_status="FAILED_COMPENSATED",
        expected_compensation_status="COMPLETED",
    )
    # 로그로 단계가 특정되는 케이스 → 분모에 포함, 정답이면 correct.
    with_label = EvalSample(
        name="inventory_reservation",
        order_status="FAILED",
        log_lines=["[Saga] 재고 예약 실패"],
        expected_diagnosis_status="FAILED_COMPENSATED",
        expected_compensation_status="COMPLETED",
        expected_failed_step="INVENTORY_RESERVATION",
    )

    report = EvalRunner().run([no_label, with_label])

    assert report.failed_step_total == 1
    assert report.failed_step_correct == 1
    assert report.cases[0].failed_step_ok is None
    assert report.cases[1].failed_step_ok is True


def test_failed_step_mismatch_fails_run() -> None:
    sample = EvalSample(
        name="wrong_step",
        order_status="FAILED",
        log_lines=["[Saga] 재고 예약 실패"],  # 실제 INVENTORY_RESERVATION
        expected_diagnosis_status="FAILED_COMPENSATED",
        expected_compensation_status="COMPLETED",
        expected_failed_step="SHIPMENT_CREATION",
    )

    report = EvalRunner().run([sample])

    assert report.failed_step_total == 1
    assert report.failed_step_correct == 0
    assert report.cases[0].actual_failed_step.value == "INVENTORY_RESERVATION"
    assert not report.all_passed


def test_grounding_invariant_holds_for_unknown() -> None:
    # order_status None → UNKNOWN 진단. confidence 0·조치 없음이라 grounding_ok.
    report = EvalRunner().run(
        [
            EvalSample(
                name="unknown",
                order_status=None,
                expected_diagnosis_status="UNKNOWN",
                expected_compensation_status="UNKNOWN",
            )
        ]
    )

    assert report.grounding_correct == 1
    assert report.cases[0].grounding_ok is True


def test_writes_gated_invariant_holds_for_read_only_case() -> None:
    # COMPENSATION_FAILED → READ_ONLY 권장 조치(승인 불요). writes_gated_ok True.
    report = EvalRunner().run(
        [
            EvalSample(
                name="compfail",
                order_status="COMPENSATION_FAILED",
                expected_diagnosis_status="FAILED_COMPENSATION_FAILED",
                expected_compensation_status="FAILED",
            )
        ]
    )

    assert report.writes_gated_correct == 1
    assert report.cases[0].writes_gated_ok is True


def test_orphan_shipment_emits_gated_recovery_action() -> None:
    # 주문 CANCELLED + 살아있는 배송 → RECOVERY_WRITE 복구 제안(승인 필요, §16.4 T5a).
    report = EvalRunner().run(
        [
            EvalSample(
                name="orphan",
                order_status="CANCELLED",
                shipment_statuses=["HUB_MOVING"],
                expected_diagnosis_status="RISK_DETECTED",
                expected_compensation_status="NOT_REQUIRED",
            )
        ]
    )

    diagnosis = EvalRunner()._engine.diagnose(
        OrderStatus.CANCELLED, ["HUB_MOVING"], None, None
    )
    proposal = diagnosis.recommended_actions[0]
    assert proposal.action_type == "CANCEL_ORPHAN_SHIPMENT"
    assert proposal.risk_level.value == "RECOVERY_WRITE"
    assert proposal.requires_approval is True
    # write 제안이지만 승인 게이트가 걸려 있어 불변식은 성립한다.
    assert report.cases[0].writes_gated_ok is True


def test_writes_gated_invariant_detects_ungated_write() -> None:
    # 승인 게이트 없는 write 제안 → 불변식 위반. RECOVERY_WRITE인데 requires_approval=False.
    ungated = Diagnosis(
        diagnosis_status=DiagnosisStatus.RISK_DETECTED,
        confidence=0.5,
        summary="test",
        recommended_actions=[
            RecommendedAction(
                action_type="CANCEL_ORPHAN_SHIPMENT",
                risk_level=ActionRiskLevel.RECOVERY_WRITE,
                description="ungated",
                requires_approval=False,
            )
        ],
    )
    assert _writes_gated_ok(ungated) is False


def test_cli_main_returns_zero_on_seed() -> None:
    assert main([str(_SEED)]) == 0


def test_cli_main_default_dataset() -> None:
    assert main([]) == 0
