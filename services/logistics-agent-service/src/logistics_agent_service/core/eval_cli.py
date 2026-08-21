import sys
from pathlib import Path

from logistics_agent_service.application.eval import EvalReport, EvalRunner
from logistics_agent_service.infrastructure.eval.jsonl_loader import load_samples

_DEFAULT_DATASET = (
    Path(__file__).resolve().parent.parent / "eval_data" / "diagnosis_seed.jsonl"
)


def _case_ok(case) -> bool:
    return (
        case.diagnosis_ok
        and case.compensation_ok
        and case.failed_step_ok is not False
        and case.read_only_ok
        and case.grounding_ok
    )


def _format_report(report: EvalReport) -> str:
    lines = ["== 진단 규칙 Eval =="]
    for case in report.cases:
        mark = "OK " if _case_ok(case) else "FAIL"
        lines.append(
            f"[{mark}] {case.name}: "
            f"diagnosis={case.actual_diagnosis_status.value} "
            f"compensation={case.actual_compensation_status.value} "
            f"failed_step={case.actual_failed_step.value}"
        )
    lines.append("")
    lines.append(
        f"diagnosisStatus 정확도: {report.diagnosis_correct}/{report.total} "
        f"({report.diagnosis_accuracy:.0%})"
    )
    lines.append(
        f"compensationStatus 정확도: {report.compensation_correct}/{report.total} "
        f"({report.compensation_accuracy:.0%})"
    )
    lines.append(
        f"failedStep 정확도: {report.failed_step_correct}/{report.failed_step_total} "
        f"({report.failed_step_accuracy:.0%})"
    )
    lines.append(
        f"read-only 불변식: {report.read_only_correct}/{report.total}"
    )
    lines.append(
        f"grounding 불변식: {report.grounding_correct}/{report.total}"
    )
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    args = argv if argv is not None else sys.argv[1:]
    dataset = Path(args[0]) if args else _DEFAULT_DATASET

    report = EvalRunner().run(load_samples(dataset))
    print(_format_report(report))
    return 0 if report.all_passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
