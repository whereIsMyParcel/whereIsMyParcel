from logistics_agent_service.application.dto import OrderContext, ReportResult
from logistics_agent_service.domain.models import Diagnosis


class StubReportGenerator:
    """S1 템플릿 리포트 생성기(ReportGeneratorPort 구현).

    LLM을 사용하지 않고 진단 결과를 그대로 문자열로 포맷한다. S4에서 Gemini 구현으로 교체한다.
    LLM을 쓰지 않으므로 trace는 None이다.
    """

    def generate(
        self, diagnosis: Diagnosis, order_context: OrderContext | None
    ) -> ReportResult:
        if order_context is not None:
            status = order_context.order_status.value if order_context.order_status else "미상"
            order_line = f"- 주문: {order_context.order_number} (상태 {status})"
        else:
            order_line = "- 주문: 식별 불가"

        if diagnosis.recommended_actions:
            actions = "\n".join(
                f"  - [{action.risk_level.value}] {action.action_type}: {action.description}"
                for action in diagnosis.recommended_actions
            )
        else:
            actions = "  - 없음"

        report = (
            "## 진단 리포트 (S1 템플릿)\n"
            f"{order_line}\n"
            f"- 진단 상태: {diagnosis.diagnosis_status.value}\n"
            f"- 보상 상태: {diagnosis.compensation_status.value}\n"
            f"- 실패 단계: {diagnosis.failed_step.value}\n"
            f"- 신뢰도: {diagnosis.confidence:.2f}\n"
            f"- 요약: {diagnosis.summary}\n"
            "- 권장 조치:\n"
            f"{actions}\n"
        )
        return ReportResult(report=report, trace=None)
