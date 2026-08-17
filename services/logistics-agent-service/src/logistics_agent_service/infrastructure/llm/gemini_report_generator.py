import logging
import time
from typing import Any

from google import genai

from logistics_agent_service.application.dto import (
    LlmTrace,
    OrderContext,
    ReportResult,
)
from logistics_agent_service.domain.models import Diagnosis
from logistics_agent_service.infrastructure.llm.prompt import (
    PROMPT_VERSION,
    build_report_prompt,
)

logger = logging.getLogger(__name__)


def _extract_token_usage(response: Any) -> dict | None:
    usage = getattr(response, "usage_metadata", None)
    if usage is None:
        return None
    return {
        "prompt_tokens": getattr(usage, "prompt_token_count", None),
        "candidates_tokens": getattr(usage, "candidates_token_count", None),
        "total_tokens": getattr(usage, "total_token_count", None),
    }


class GeminiReportGenerator:
    """ReportGeneratorPort의 Gemini 구현.

    규칙 엔진이 판정한 진단을 근거로 운영자용 리포트(prose)만 생성한다(design §5:
    판정은 규칙, LLM은 서술). 호출 실패 시 crash 대신 폴백 리포트를 반환한다.
    모든 호출은 LlmTrace(model/token/latency)로 관측 기록을 남긴다(design §12.5).
    """

    def __init__(self, client: genai.Client, model: str) -> None:
        self._client = client
        self._model = model

    def generate(
        self, diagnosis: Diagnosis, order_context: OrderContext | None
    ) -> ReportResult:
        prompt = build_report_prompt(diagnosis, order_context)
        input_messages = [{"role": "user", "content": prompt}]
        start = time.perf_counter()
        try:
            response = self._client.models.generate_content(
                model=self._model,
                contents=prompt,
            )
        except Exception as exc:
            latency_ms = int((time.perf_counter() - start) * 1000)
            logger.warning("Gemini 리포트 생성 실패, 폴백 사용: %s", exc)
            report = f"[AI 리포트 생성 실패] {diagnosis.summary}"
            return ReportResult(
                report=report,
                trace=self._trace(input_messages, report, None, latency_ms),
            )

        latency_ms = int((time.perf_counter() - start) * 1000)
        text = response.text
        report = text.strip() if text else f"[AI 리포트 없음] {diagnosis.summary}"
        return ReportResult(
            report=report,
            trace=self._trace(
                input_messages, report, _extract_token_usage(response), latency_ms
            ),
        )

    def _trace(
        self,
        input_messages: list[dict],
        output_message: str,
        token_usage: dict | None,
        latency_ms: int,
    ) -> LlmTrace:
        return LlmTrace(
            model=self._model,
            prompt_version=PROMPT_VERSION,
            input_messages=input_messages,
            output_message=output_message,
            token_usage=token_usage,
            latency_ms=latency_ms,
        )
