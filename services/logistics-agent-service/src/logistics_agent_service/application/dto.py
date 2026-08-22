from uuid import UUID

from pydantic import BaseModel

from logistics_agent_service.domain.enums import OrderStatus, TriggerType
from logistics_agent_service.domain.models import Diagnosis


class OrderContext(BaseModel):
    """order-service 내부 API(GET /internal/v1/orders/{orderId})가 반환하는
    OrderAiContextResponse 중 진단에 필요한 부분만 담는다."""

    order_id: UUID
    order_number: str
    order_status: OrderStatus | None


class ShipmentInfo(BaseModel):
    """shipment-service 내부 API가 반환하는 배송 정보 중 진단에 필요한 부분."""

    status: str
    origin_hub_id: UUID | None = None
    destination_hub_id: UUID | None = None


class DiagnosisQuery(BaseModel):
    message: str
    trigger_type: TriggerType = TriggerType.USER_QUERY
    # incident처럼 orderId를 이미 아는 경우 message 정규식 추출을 건너뛴다.
    order_identifier: str | None = None
    # SYSTEM_INCIDENT 트리거의 부가 맥락(design §6.2 IncidentType, §12.1).
    incident_type: str | None = None
    source_service: str | None = None


class ToolCallRecord(BaseModel):
    """agent가 진단 중 호출한 internal API tool 1건의 관측 기록(design §12.3).

    error_code/error_message는 port가 HTTP 상태를 감춰 현재 채우지 않는다(nullable).
    success는 결과가 None이 아닌지로 근사한다(None = 조회 실패/강등).
    """

    tool_name: str
    input: dict | None = None
    output: dict | None = None
    success: bool
    error_code: str | None = None
    error_message: str | None = None
    latency_ms: int


class LlmTrace(BaseModel):
    """리포트 생성 LLM 호출 1건의 관측 기록(design §12.5).

    model/token_usage/latency는 어댑터(LLM)만 아는 정보라 ReportGeneratorPort가 표면화한다.
    """

    model: str
    prompt_version: str
    input_messages: list[dict] | None = None
    output_message: str
    token_usage: dict | None = None
    latency_ms: int


class ReportResult(BaseModel):
    """ReportGeneratorPort.generate 반환값. 리포트 본문 + (있으면) LLM trace."""

    report: str
    trace: LlmTrace | None = None


class JudgeVerdict(BaseModel):
    """LLM-as-judge가 리포트 1건을 채점한 결과(design §14, S18 opt-in eval).

    faithfulness/readability는 1~5. hallucination=True면 규칙이 판정한 근거를
    벗어나 지어낸 내용이 있다는 뜻이다. reason은 판정 사유(사람 검수용).
    judge 호출/파싱 실패는 어댑터에서 보수적 verdict(faithfulness=1,
    hallucination=True)로 강등한다(§5 유지: judge는 서술 품질만 평가).
    """

    faithfulness: int
    readability: int
    hallucination: bool
    reason: str


class DiagnosisResult(BaseModel):
    diagnosis: Diagnosis
    report: str
    trigger_type: TriggerType = TriggerType.USER_QUERY
    order_id: UUID | None = None
    order_number: str | None = None
    diagnosis_id: UUID | None = None
    tool_calls: list[ToolCallRecord] = []
    llm_trace: LlmTrace | None = None
    # incident 트리거 맥락 + 원 사용자 질의(design §12.1).
    incident_type: str | None = None
    source_service: str | None = None
    user_question: str | None = None


class ScanSummary(BaseModel):
    """scheduled scan 1회 실행 결과 요약(design §16.2).

    scanned: 열거된 후보 수(중복 상태 포함), diagnosed: 새로 진단한 수,
    skipped: 이미 진단 이력이 있어 건너뛴 수.
    """

    scanned: int = 0
    diagnosed: int = 0
    skipped: int = 0
    diagnosis_ids: list[UUID] = []
