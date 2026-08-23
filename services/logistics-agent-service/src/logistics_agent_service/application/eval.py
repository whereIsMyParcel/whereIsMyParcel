from pydantic import BaseModel

from logistics_agent_service.domain.enums import (
    ActionRiskLevel,
    CompensationStatus,
    DiagnosisStatus,
    FailureStep,
    OrderStatus,
)
from logistics_agent_service.domain.models import Diagnosis
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine


class EvalSample(BaseModel):
    """라벨링된 진단 케이스: 입력(order/shipment/log) → 기대 진단.

    expected_failed_step은 옵션이다. 선언된 케이스만 failed_step 정확도 분모에 든다
    (log_lines로 단계가 특정되는 FAILED/COMPENSATION_FAILED 케이스용, §16.1).
    """

    name: str
    order_status: OrderStatus | None = None
    shipment_statuses: list[str] | None = None
    route_ok: bool | None = None
    log_lines: list[str] | None = None
    expected_diagnosis_status: DiagnosisStatus
    expected_compensation_status: CompensationStatus
    expected_failed_step: FailureStep | None = None


class EvalCaseResult(BaseModel):
    name: str
    diagnosis_ok: bool
    compensation_ok: bool
    # expected_failed_step이 없으면 평가하지 않음(None).
    failed_step_ok: bool | None
    # 불변식(§10·§16.4 write 승인게이트, §14 근거 없는 단정)은 모든 케이스에서 측정한다.
    writes_gated_ok: bool
    grounding_ok: bool
    actual_diagnosis_status: DiagnosisStatus
    actual_compensation_status: CompensationStatus
    actual_failed_step: FailureStep


class EvalReport(BaseModel):
    total: int
    diagnosis_correct: int
    compensation_correct: int
    # failed_step은 expected가 선언된 케이스만 대상으로 한다(total과 분모가 다름).
    failed_step_total: int
    failed_step_correct: int
    writes_gated_correct: int
    grounding_correct: int
    cases: list[EvalCaseResult]

    @property
    def diagnosis_accuracy(self) -> float:
        return self.diagnosis_correct / self.total if self.total else 0.0

    @property
    def compensation_accuracy(self) -> float:
        return self.compensation_correct / self.total if self.total else 0.0

    @property
    def failed_step_accuracy(self) -> float:
        return (
            self.failed_step_correct / self.failed_step_total
            if self.failed_step_total
            else 1.0
        )

    @property
    def all_passed(self) -> bool:
        return (
            self.total > 0
            and self.diagnosis_correct == self.total
            and self.compensation_correct == self.total
            and self.failed_step_correct == self.failed_step_total
            and self.writes_gated_correct == self.total
            and self.grounding_correct == self.total
        )


def _writes_gated_ok(diagnosis: Diagnosis) -> bool:
    """write 조치가 승인 게이트로 보호되는가(§10·§16.4 불변식).

    read-only 레인이 recovery로 확장되며 S17의 "모든 조치 READ_ONLY" 불변식을
    "write는 승인 게이트"로 진화시킨다(T5a). 안전 보장은 동일하다: 승인 없는
    write 실행이 제안조차 되지 않는다.
    - READ_ONLY 조치는 자동 허용이라 승인을 요구하면 안 된다(requires_approval=False).
    - 비 READ_ONLY(RECOVERY_WRITE 등) 조치는 반드시 승인을 요구해야 한다.
    권장 조치가 없으면 자명히 True.
    """
    for action in diagnosis.recommended_actions:
        if action.risk_level is ActionRiskLevel.READ_ONLY:
            if action.requires_approval:
                return False
        elif not action.requires_approval:
            return False
    return True


def _grounding_ok(diagnosis: Diagnosis) -> bool:
    """근거 없는 단정을 하지 않는가(§14).

    - 어떤 진단이든 summary는 비어있지 않아야 한다.
    - UNKNOWN 진단은 확신(confidence>0)하거나 조치를 권하지 않아야 한다.
    """
    if not diagnosis.summary.strip():
        return False
    if diagnosis.diagnosis_status is DiagnosisStatus.UNKNOWN:
        return diagnosis.confidence == 0.0 and not diagnosis.recommended_actions
    return True


class EvalRunner:
    """규칙 엔진을 라벨셋에 돌려 정확도·불변식을 집계한다(design §14).

    LLM/DB/외부 호출 없이 순수 규칙 엔진만 평가하므로 CI에서도 실행 가능하다.
    측정 축: diagnosisStatus / compensationStatus / failedStep 정확도 +
    write-승인게이트·grounding 불변식. LLM 리포트 품질(readability 등)은 별도 lane(S18).
    """

    def __init__(self, engine: RuleBasedDiagnosisEngine | None = None) -> None:
        self._engine = engine or RuleBasedDiagnosisEngine()

    def run(self, samples: list[EvalSample]) -> EvalReport:
        cases: list[EvalCaseResult] = []
        diagnosis_correct = 0
        compensation_correct = 0
        failed_step_total = 0
        failed_step_correct = 0
        writes_gated_correct = 0
        grounding_correct = 0

        for sample in samples:
            diagnosis = self._engine.diagnose(
                sample.order_status,
                sample.shipment_statuses,
                sample.route_ok,
                sample.log_lines,
            )
            diagnosis_ok = diagnosis.diagnosis_status == sample.expected_diagnosis_status
            compensation_ok = (
                diagnosis.compensation_status == sample.expected_compensation_status
            )
            diagnosis_correct += int(diagnosis_ok)
            compensation_correct += int(compensation_ok)

            failed_step_ok: bool | None = None
            if sample.expected_failed_step is not None:
                failed_step_total += 1
                failed_step_ok = diagnosis.failed_step == sample.expected_failed_step
                failed_step_correct += int(failed_step_ok)

            writes_gated_ok = _writes_gated_ok(diagnosis)
            grounding_ok = _grounding_ok(diagnosis)
            writes_gated_correct += int(writes_gated_ok)
            grounding_correct += int(grounding_ok)

            cases.append(
                EvalCaseResult(
                    name=sample.name,
                    diagnosis_ok=diagnosis_ok,
                    compensation_ok=compensation_ok,
                    failed_step_ok=failed_step_ok,
                    writes_gated_ok=writes_gated_ok,
                    grounding_ok=grounding_ok,
                    actual_diagnosis_status=diagnosis.diagnosis_status,
                    actual_compensation_status=diagnosis.compensation_status,
                    actual_failed_step=diagnosis.failed_step,
                )
            )

        return EvalReport(
            total=len(samples),
            diagnosis_correct=diagnosis_correct,
            compensation_correct=compensation_correct,
            failed_step_total=failed_step_total,
            failed_step_correct=failed_step_correct,
            writes_gated_correct=writes_gated_correct,
            grounding_correct=grounding_correct,
            cases=cases,
        )
