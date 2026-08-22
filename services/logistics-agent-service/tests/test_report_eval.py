from types import SimpleNamespace

from logistics_agent_service.application.dto import (
    JudgeVerdict,
    OrderContext,
    ReportResult,
)
from logistics_agent_service.application.eval import EvalSample
from logistics_agent_service.application.report_eval import ReportEvalRunner
from logistics_agent_service.core import report_eval_cli
from logistics_agent_service.domain.models import Diagnosis

_GOOD_REPORT = (
    "## 진단 요약\n주문이 실패했고 보상은 완료되었습니다. 추가 조치는 확인이 필요합니다."
)


class _FakeGenerator:
    def __init__(self, report: str) -> None:
        self._report = report

    def generate(
        self, diagnosis: Diagnosis, order_context: OrderContext | None
    ) -> ReportResult:
        return ReportResult(report=self._report)


class _FakeJudge:
    def __init__(self, verdict: JudgeVerdict) -> None:
        self._verdict = verdict
        self.calls = 0

    def judge(
        self,
        *,
        report: str,
        diagnosis: Diagnosis,
        order_context: OrderContext | None,
    ) -> JudgeVerdict:
        self.calls += 1
        return self._verdict


_SAMPLE = EvalSample(
    name="failed_compensated",
    order_status="FAILED",
    expected_diagnosis_status="FAILED_COMPENSATED",
    expected_compensation_status="COMPLETED",
)


def _runner(report: str, verdict: JudgeVerdict) -> tuple[ReportEvalRunner, _FakeJudge]:
    judge = _FakeJudge(verdict)
    runner = ReportEvalRunner(_FakeGenerator(report), judge, min_score=4, min_length=40)
    return runner, judge


def test_good_report_high_verdict_passes() -> None:
    verdict = JudgeVerdict(
        faithfulness=5, readability=5, hallucination=False, reason="ok"
    )
    runner, judge = _runner(_GOOD_REPORT, verdict)

    report = runner.run([_SAMPLE])

    assert report.total == 1
    assert report.heuristic_passed == 1
    assert report.judged == 1
    assert judge.calls == 1
    assert report.passed == 1
    assert report.all_passed


def test_fallback_report_skips_judge() -> None:
    # 폴백 접두어 → 휴리스틱 실패, judge 호출 안 함(비용 절약), 케이스 실패.
    verdict = JudgeVerdict(
        faithfulness=5, readability=5, hallucination=False, reason="ok"
    )
    runner, judge = _runner("[AI 리포트 없음] 요약 텍스트", verdict)

    report = runner.run([_SAMPLE])

    assert report.heuristic_passed == 0
    assert report.judged == 0
    assert judge.calls == 0
    assert report.cases[0].not_fallback is False
    assert report.cases[0].verdict is None
    assert not report.all_passed


def test_short_report_fails_heuristic() -> None:
    verdict = JudgeVerdict(
        faithfulness=5, readability=5, hallucination=False, reason="ok"
    )
    runner, judge = _runner("짧음", verdict)

    report = runner.run([_SAMPLE])

    assert report.cases[0].length_ok is False
    assert judge.calls == 0
    assert not report.all_passed


def test_low_faithfulness_fails() -> None:
    verdict = JudgeVerdict(
        faithfulness=2, readability=5, hallucination=False, reason="근거 이탈"
    )
    runner, _ = _runner(_GOOD_REPORT, verdict)

    report = runner.run([_SAMPLE])

    assert report.heuristic_passed == 1
    assert report.judged == 1
    assert report.passed == 0
    assert not report.all_passed


def test_hallucination_fails_despite_high_scores() -> None:
    verdict = JudgeVerdict(
        faithfulness=5, readability=5, hallucination=True, reason="지어냄"
    )
    runner, _ = _runner(_GOOD_REPORT, verdict)

    report = runner.run([_SAMPLE])

    assert report.passed == 0
    assert not report.all_passed


def test_cli_skips_when_disabled(monkeypatch) -> None:
    # opt-in 게이트 off → Gemini를 건드리지 않고 즉시 초록(exit 0).
    monkeypatch.setattr(
        report_eval_cli,
        "get_settings",
        lambda: SimpleNamespace(eval_llm_enabled=False),
    )
    assert report_eval_cli.main([]) == 0


def test_cli_skips_when_no_key(monkeypatch) -> None:
    monkeypatch.setattr(
        report_eval_cli,
        "get_settings",
        lambda: SimpleNamespace(eval_llm_enabled=True, gemini_api_key=None),
    )
    assert report_eval_cli.main([]) == 0
