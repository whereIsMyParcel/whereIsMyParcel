from pathlib import Path

from logistics_agent_service.application.eval import EvalRunner, EvalSample
from logistics_agent_service.core.eval_cli import main
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

    assert len(samples) == 11
    names = {s.name for s in samples}
    assert "compensation_failed" in names
    assert "order_unknown" in names
    assert "confirmed_invalid_route" in names


def test_runner_all_seed_cases_pass() -> None:
    report = EvalRunner().run(load_samples(_SEED))

    assert report.total == 11
    assert report.diagnosis_accuracy == 1.0
    assert report.compensation_accuracy == 1.0
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


def test_cli_main_returns_zero_on_seed() -> None:
    assert main([str(_SEED)]) == 0


def test_cli_main_default_dataset() -> None:
    assert main([]) == 0
