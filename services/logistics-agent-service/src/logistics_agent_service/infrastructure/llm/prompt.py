from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.domain.models import Diagnosis

# 프롬프트 템플릿 버전. 변경 시 올려 trace(agent_llm_trace.prompt_version)로 추적한다.
PROMPT_VERSION = "v1"

_SYSTEM = (
    "당신은 물류 운영 진단 어시스턴트입니다. 아래 구조화된 진단 결과를 바탕으로 "
    "운영자가 즉시 이해하고 조치할 수 있는 한국어 리포트를 Markdown으로 작성하세요. "
    "진단 상태·실패 단계·보상 상태는 이미 규칙으로 판정된 값이므로 바꾸지 말고, "
    "근거(evidence)에 기반해 상황과 권장 조치를 간결하게 서술하세요. "
    "정보가 부족하면 단정하지 말고 확인이 필요하다고 명시하세요."
)


def build_report_prompt(diagnosis: Diagnosis, order_context: OrderContext | None) -> str:
    """규칙 엔진 진단 결과를 Gemini 리포트 생성용 프롬프트로 구성한다(순수 함수)."""
    if order_context is not None:
        status = order_context.order_status.value if order_context.order_status else "미상"
        order_line = f"- 주문번호: {order_context.order_number} (상태: {status})"
    else:
        order_line = "- 주문: 식별 불가"

    evidence_lines = (
        "\n".join(
            f"  - [{item.source_service}] {item.tool_name}: {item.result}"
            for item in diagnosis.evidence
        )
        or "  - 없음"
    )
    action_lines = (
        "\n".join(
            f"  - [{action.risk_level.value}] {action.action_type}: {action.description}"
            for action in diagnosis.recommended_actions
        )
        or "  - 없음"
    )

    return (
        f"{_SYSTEM}\n\n"
        "## 진단 결과\n"
        f"{order_line}\n"
        f"- 진단 상태: {diagnosis.diagnosis_status.value}\n"
        f"- 심각도: {diagnosis.severity.value}\n"
        f"- 실패 단계: {diagnosis.failed_step.value}\n"
        f"- 보상 상태: {diagnosis.compensation_status.value}\n"
        f"- 신뢰도: {diagnosis.confidence:.2f}\n"
        f"- 요약: {diagnosis.summary}\n"
        "- 근거:\n"
        f"{evidence_lines}\n"
        "- 권장 조치 후보:\n"
        f"{action_lines}\n"
    )
