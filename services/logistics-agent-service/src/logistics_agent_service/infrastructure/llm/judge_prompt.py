from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.domain.models import Diagnosis

# judge 프롬프트 버전. 변경 시 올려 결과 비교의 기준을 추적한다.
JUDGE_PROMPT_VERSION = "v1"

_SYSTEM = (
    "당신은 물류 운영 진단 리포트를 평가하는 엄정한 심사관입니다. "
    "아래 '규칙 판정(ground truth)'은 결정적 규칙 엔진이 확정한 값이며 정답입니다. "
    "'리포트'는 이 판정을 근거로 LLM이 작성한 운영자용 서술입니다. "
    "리포트가 규칙 판정과 근거에 충실한지(faithfulness), 운영자가 읽기 좋은지"
    "(readability)를 평가하세요. 규칙 판정과 다른 진단/원인을 지어내거나, 근거에 "
    "없는 사실을 단정하면 hallucination=true 입니다. "
    "반드시 아래 JSON 객체 하나만 출력하세요(코드펜스·설명 금지):\n"
    '{"faithfulness": <1-5 정수>, "readability": <1-5 정수>, '
    '"hallucination": <true|false>, "reason": "<한국어 한두 문장>"}'
)


def build_judge_prompt(
    report: str, diagnosis: Diagnosis, order_context: OrderContext | None
) -> str:
    """규칙 판정(ground truth) + 리포트를 judge 채점용 프롬프트로 구성한다(순수 함수)."""
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

    return (
        f"{_SYSTEM}\n\n"
        "## 규칙 판정(ground truth)\n"
        f"{order_line}\n"
        f"- 진단 상태: {diagnosis.diagnosis_status.value}\n"
        f"- 실패 단계: {diagnosis.failed_step.value}\n"
        f"- 보상 상태: {diagnosis.compensation_status.value}\n"
        f"- 요약: {diagnosis.summary}\n"
        "- 근거:\n"
        f"{evidence_lines}\n\n"
        "## 리포트\n"
        f"{report}\n"
    )
