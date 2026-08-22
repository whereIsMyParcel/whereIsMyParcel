from pydantic import BaseModel

from logistics_agent_service.application.dto import JudgeVerdict
from logistics_agent_service.application.eval import EvalSample
from logistics_agent_service.application.port.report_generator_port import (
    ReportGeneratorPort,
)
from logistics_agent_service.application.port.report_judge_port import ReportJudgePort
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine

# 리포트 생성 실패 시 어댑터가 반환하는 폴백 리포트의 접두어(design: GeminiReportGenerator).
# 실제 LLM 생성이 일어났는지 판별하는 결정적 신호로 쓴다.
_FALLBACK_PREFIX = "[AI "


class ReportEvalCaseResult(BaseModel):
    name: str
    report: str
    # 결정적 휴리스틱 선필터(LLM 호출 0): 폴백 아님 + 최소 길이.
    not_fallback: bool
    length_ok: bool
    heuristic_ok: bool
    # 휴리스틱 통과분만 judge를 호출한다(비용 절약). 실패 시 None.
    verdict: JudgeVerdict | None
    passed: bool


class ReportEvalReport(BaseModel):
    total: int
    heuristic_passed: int
    judged: int
    passed: int
    cases: list[ReportEvalCaseResult]

    @property
    def pass_rate(self) -> float:
        return self.passed / self.total if self.total else 0.0

    @property
    def all_passed(self) -> bool:
        return self.total > 0 and self.passed == self.total


class ReportEvalRunner:
    """LLM 리포트 품질 eval(design §14, S18) — CI 게이트 아님, opt-in 레인.

    파이프라인: 규칙 엔진(순수) → 리포트 생성(포트) → 결정적 휴리스틱 선필터
    → LLM-as-judge(포트). 규칙은 여전히 판정만 하고(§5), judge는 서술 품질만 본다.

    휴리스틱을 폴백/길이 같은 값싼 신호로 보수적으로 두는 이유: 리포트는 자유형
    한국어 prose라 enum 토큰 echo 여부로 근거 충실성을 결정적으로 재기 어렵다.
    미세한 faithfulness·근거 이탈 판정은 judge에 맡기고, 휴리스틱은 명백히 깨진
    생성(폴백/빈약)만 걸러 judge 호출 낭비를 막는다.
    """

    def __init__(
        self,
        generator: ReportGeneratorPort,
        judge: ReportJudgePort,
        *,
        min_score: int = 4,
        min_length: int = 40,
        engine: RuleBasedDiagnosisEngine | None = None,
    ) -> None:
        self._generator = generator
        self._judge = judge
        self._min_score = min_score
        self._min_length = min_length
        self._engine = engine or RuleBasedDiagnosisEngine()

    def _verdict_ok(self, verdict: JudgeVerdict) -> bool:
        return (
            not verdict.hallucination
            and verdict.faithfulness >= self._min_score
            and verdict.readability >= self._min_score
        )

    def run(self, samples: list[EvalSample]) -> ReportEvalReport:
        cases: list[ReportEvalCaseResult] = []
        heuristic_passed = 0
        judged = 0
        passed = 0

        for sample in samples:
            diagnosis = self._engine.diagnose(
                sample.order_status,
                sample.shipment_statuses,
                sample.route_ok,
                sample.log_lines,
            )
            # 리포트 생성 eval은 order_context를 특정하지 않는다(seed엔 주문번호 없음).
            report = self._generator.generate(diagnosis, None).report

            not_fallback = not report.strip().startswith(_FALLBACK_PREFIX)
            length_ok = len(report.strip()) >= self._min_length
            heuristic_ok = not_fallback and length_ok

            verdict: JudgeVerdict | None = None
            case_passed = False
            if heuristic_ok:
                heuristic_passed += 1
                verdict = self._judge.judge(
                    report=report,
                    diagnosis=diagnosis,
                    order_context=None,
                )
                judged += 1
                case_passed = self._verdict_ok(verdict)
                passed += int(case_passed)

            cases.append(
                ReportEvalCaseResult(
                    name=sample.name,
                    report=report,
                    not_fallback=not_fallback,
                    length_ok=length_ok,
                    heuristic_ok=heuristic_ok,
                    verdict=verdict,
                    passed=case_passed,
                )
            )

        return ReportEvalReport(
            total=len(samples),
            heuristic_passed=heuristic_passed,
            judged=judged,
            passed=passed,
            cases=cases,
        )
