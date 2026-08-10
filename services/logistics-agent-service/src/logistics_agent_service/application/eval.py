from pydantic import BaseModel

from logistics_agent_service.domain.enums import (
    CompensationStatus,
    DiagnosisStatus,
    OrderStatus,
)
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine


class EvalSample(BaseModel):
    """라벨링된 진단 케이스: 입력(order/shipment) → 기대 진단."""

    name: str
    order_status: OrderStatus | None = None
    shipment_statuses: list[str] | None = None
    route_ok: bool | None = None
    expected_diagnosis_status: DiagnosisStatus
    expected_compensation_status: CompensationStatus


class EvalCaseResult(BaseModel):
    name: str
    diagnosis_ok: bool
    compensation_ok: bool
    actual_diagnosis_status: DiagnosisStatus
    actual_compensation_status: CompensationStatus


class EvalReport(BaseModel):
    total: int
    diagnosis_correct: int
    compensation_correct: int
    cases: list[EvalCaseResult]

    @property
    def diagnosis_accuracy(self) -> float:
        return self.diagnosis_correct / self.total if self.total else 0.0

    @property
    def compensation_accuracy(self) -> float:
        return self.compensation_correct / self.total if self.total else 0.0

    @property
    def all_passed(self) -> bool:
        return self.total > 0 and self.diagnosis_correct == self.total and (
            self.compensation_correct == self.total
        )


class EvalRunner:
    """규칙 엔진을 라벨셋에 돌려 정확도를 집계한다(design §14).

    LLM/DB/외부 호출 없이 순수 규칙 엔진만 평가하므로 CI에서도 실행 가능하다.
    """

    def __init__(self, engine: RuleBasedDiagnosisEngine | None = None) -> None:
        self._engine = engine or RuleBasedDiagnosisEngine()

    def run(self, samples: list[EvalSample]) -> EvalReport:
        cases: list[EvalCaseResult] = []
        diagnosis_correct = 0
        compensation_correct = 0

        for sample in samples:
            diagnosis = self._engine.diagnose(
                sample.order_status, sample.shipment_statuses, sample.route_ok
            )
            diagnosis_ok = diagnosis.diagnosis_status == sample.expected_diagnosis_status
            compensation_ok = (
                diagnosis.compensation_status == sample.expected_compensation_status
            )
            diagnosis_correct += int(diagnosis_ok)
            compensation_correct += int(compensation_ok)
            cases.append(
                EvalCaseResult(
                    name=sample.name,
                    diagnosis_ok=diagnosis_ok,
                    compensation_ok=compensation_ok,
                    actual_diagnosis_status=diagnosis.diagnosis_status,
                    actual_compensation_status=diagnosis.compensation_status,
                )
            )

        return EvalReport(
            total=len(samples),
            diagnosis_correct=diagnosis_correct,
            compensation_correct=compensation_correct,
            cases=cases,
        )
