"""진단 이력 → SFT/eval 데이터셋 export 모델(design §13).

`agent_db`에 영속된 진단을 학습/평가용 JSONL 샘플로 직렬화한다. 우리가 실제로
영속하는 값만 채우고(§13은 목표 스키마), 미보유 필드는 정직하게 null/빈값으로 둔다.
"""

import json
from datetime import datetime
from uuid import UUID

from pydantic import BaseModel

_SFT_SYSTEM = "You are a logistics operations diagnosis assistant."
_TASK = "order_failure_diagnosis_report"


class PersistedEvidence(BaseModel):
    source_service: str
    tool_name: str
    result: str


class PersistedToolCall(BaseModel):
    tool_name: str
    input: dict | None = None
    output: dict | None = None
    success: bool
    latency_ms: int


class PersistedDiagnosis(BaseModel):
    """agent_db 진단 1건의 읽기 모델(reader 포트가 반환)."""

    diagnosis_id: UUID
    trigger_type: str
    incident_type: str | None = None
    source_service: str | None = None
    user_question: str | None = None
    order_id: UUID | None = None
    order_number: str | None = None
    diagnosis_status: str
    failed_step: str
    compensation_status: str
    confidence: float
    summary: str
    report: str
    created_at: datetime
    evidence: list[PersistedEvidence] = []
    tool_calls: list[PersistedToolCall] = []
    model_used: str | None = None


class DatasetSample(BaseModel):
    """design §13 라벨 데이터셋 샘플(JSONL 1줄)."""

    id: str
    task: str
    input: dict
    label: dict
    metadata: dict

    @classmethod
    def from_persisted(cls, index: int, d: PersistedDiagnosis) -> "DatasetSample":
        return cls(
            id=f"diag-{index:06d}",
            task=_TASK,
            input={
                "userQuestion": d.user_question or "",
                "orderContext": _tool_output(d, "get_order_context"),
                "shipmentContext": _tool_output(d, "get_shipments"),
                "hubContext": {
                    "routeChecks": [
                        tc.output
                        for tc in d.tool_calls
                        if tc.tool_name == "route_exists" and tc.output is not None
                    ]
                },
                "inventoryContext": {},  # 현재 재고 tool 미수집(§18 후속)
                "toolEvidence": [
                    {
                        "sourceService": e.source_service,
                        "toolName": e.tool_name,
                        "result": e.result,
                    }
                    for e in d.evidence
                ],
            },
            label={
                "diagnosisStatus": d.diagnosis_status,
                "failedStep": d.failed_step,
                "compensationStatus": d.compensation_status,
                "responsibleService": None,  # 미영속(§13 목표 스키마)
                "severity": None,  # 미영속
                "report": d.report,
            },
            metadata={
                "source": "runtime",
                "modelUsedForDraft": d.model_used,
                "reviewedByHuman": False,
                "createdAt": d.created_at.isoformat(),
            },
        )

    def to_sft_messages(self) -> dict:
        """design §13 SFT chat 포맷으로 변환한다."""
        return {
            "messages": [
                {"role": "system", "content": _SFT_SYSTEM},
                {"role": "user", "content": json.dumps(self.input, ensure_ascii=False)},
                {
                    "role": "assistant",
                    "content": json.dumps(self.label, ensure_ascii=False),
                },
            ]
        }


def _tool_output(d: PersistedDiagnosis, tool_name: str) -> dict:
    for tc in d.tool_calls:
        if tc.tool_name == tool_name and tc.output is not None:
            return tc.output
    return {}
