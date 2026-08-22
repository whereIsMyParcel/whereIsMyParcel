import sys
from pathlib import Path

from google import genai

from logistics_agent_service.application.report_eval import (
    ReportEvalReport,
    ReportEvalRunner,
)
from logistics_agent_service.core.config import get_settings
from logistics_agent_service.infrastructure.eval.jsonl_loader import load_samples
from logistics_agent_service.infrastructure.llm.gemini_report_generator import (
    GeminiReportGenerator,
)
from logistics_agent_service.infrastructure.llm.gemini_report_judge import (
    GeminiReportJudge,
)

_DEFAULT_DATASET = (
    Path(__file__).resolve().parent.parent / "eval_data" / "diagnosis_seed.jsonl"
)


def _format_report(report: ReportEvalReport) -> str:
    lines = ["== LLM 리포트 품질 Eval (opt-in) =="]
    for case in report.cases:
        mark = "OK  " if case.passed else ("FAIL" if case.heuristic_ok else "SKIP")
        if case.verdict is not None:
            v = case.verdict
            detail = (
                f"faithfulness={v.faithfulness} readability={v.readability} "
                f"hallucination={v.hallucination}"
            )
        else:
            detail = (
                f"휴리스틱 실패(not_fallback={case.not_fallback} "
                f"length_ok={case.length_ok}) → judge 생략"
            )
        lines.append(f"[{mark}] {case.name}: {detail}")
    lines.append("")
    lines.append(f"휴리스틱 통과: {report.heuristic_passed}/{report.total}")
    lines.append(f"judge 채점: {report.judged}")
    lines.append(f"종합 통과: {report.passed}/{report.total} ({report.pass_rate:.0%})")
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    args = argv if argv is not None else sys.argv[1:]
    settings = get_settings()

    # opt-in 게이트: 비결정적·과금이라 CI 밖에서만 돈다. 미설정이면 조용히 skip(초록).
    if not settings.eval_llm_enabled:
        print("EVAL_LLM_ENABLED=false → LLM 리포트 품질 eval을 건너뜁니다.")
        return 0
    if not settings.gemini_api_key:
        print("GEMINI_API_KEY 미설정 → LLM 리포트 품질 eval을 건너뜁니다.")
        return 0

    dataset = Path(args[0]) if args else _DEFAULT_DATASET
    client = genai.Client(api_key=settings.gemini_api_key)
    generator = GeminiReportGenerator(client, settings.gemini_model)
    judge = GeminiReportJudge(client, settings.eval_judge_model)
    runner = ReportEvalRunner(
        generator,
        judge,
        min_score=settings.eval_judge_min_score,
        min_length=settings.eval_report_min_length,
    )

    report = runner.run(load_samples(dataset))
    print(_format_report(report))
    return 0 if report.all_passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
