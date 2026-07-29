import logging

from google import genai

from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.domain.models import Diagnosis
from logistics_agent_service.infrastructure.llm.prompt import build_report_prompt

logger = logging.getLogger(__name__)


class GeminiReportGenerator:
    """ReportGeneratorPort의 Gemini 구현.

    규칙 엔진이 판정한 진단을 근거로 운영자용 리포트(prose)만 생성한다(design §5:
    판정은 규칙, LLM은 서술). 호출 실패 시 crash 대신 폴백 리포트를 반환한다.
    """

    def __init__(self, client: genai.Client, model: str) -> None:
        self._client = client
        self._model = model

    def generate(self, diagnosis: Diagnosis, order_context: OrderContext | None) -> str:
        prompt = build_report_prompt(diagnosis, order_context)
        try:
            response = self._client.models.generate_content(
                model=self._model,
                contents=prompt,
            )
        except Exception as exc:
            logger.warning("Gemini 리포트 생성 실패, 폴백 사용: %s", exc)
            return f"[AI 리포트 생성 실패] {diagnosis.summary}"

        text = response.text
        return text.strip() if text else f"[AI 리포트 없음] {diagnosis.summary}"
