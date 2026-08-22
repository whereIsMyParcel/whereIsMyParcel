import json
import logging
import re

from google import genai

from logistics_agent_service.application.dto import (
    JudgeVerdict,
    OrderContext,
)
from logistics_agent_service.domain.models import Diagnosis
from logistics_agent_service.infrastructure.llm.judge_prompt import build_judge_prompt

logger = logging.getLogger(__name__)

_JSON_BLOCK = re.compile(r"\{.*\}", re.DOTALL)


def _parse_verdict(text: str) -> JudgeVerdict:
    """judge 응답 텍스트에서 JSON verdict를 추출한다.

    코드펜스/잡텍스트가 섞여도 첫 JSON 객체를 뽑는다. 파싱 실패 시 보수적으로
    강등(faithfulness=1, hallucination=True)해 조용한 통과를 막는다.
    """
    match = _JSON_BLOCK.search(text or "")
    if match is None:
        logger.warning("judge 응답에서 JSON을 찾지 못함, 보수적 강등: %r", text[:200])
        return JudgeVerdict(
            faithfulness=1,
            readability=1,
            hallucination=True,
            reason="judge 응답 파싱 실패",
        )
    try:
        data = json.loads(match.group(0))
        return JudgeVerdict(
            faithfulness=int(data["faithfulness"]),
            readability=int(data["readability"]),
            hallucination=bool(data["hallucination"]),
            reason=str(data.get("reason", "")),
        )
    except (json.JSONDecodeError, KeyError, ValueError, TypeError) as exc:
        logger.warning("judge verdict 파싱 실패, 보수적 강등: %s", exc)
        return JudgeVerdict(
            faithfulness=1,
            readability=1,
            hallucination=True,
            reason=f"judge verdict 파싱 실패: {exc}",
        )


class GeminiReportJudge:
    """ReportJudgePort의 Gemini 구현(design §14, S18 opt-in eval).

    리포트 생성(flash)과 다른 상위 tier 모델로 채점해 self-preference bias를 줄인다.
    호출/파싱 실패는 crash 대신 보수적 verdict로 강등한다(§5: judge는 서술 품질만 평가).
    """

    def __init__(self, client: genai.Client, model: str) -> None:
        self._client = client
        self._model = model

    def judge(
        self,
        *,
        report: str,
        diagnosis: Diagnosis,
        order_context: OrderContext | None,
    ) -> JudgeVerdict:
        prompt = build_judge_prompt(report, diagnosis, order_context)
        try:
            response = self._client.models.generate_content(
                model=self._model,
                contents=prompt,
            )
        except Exception as exc:
            logger.warning("Gemini judge 호출 실패, 보수적 강등: %s", exc)
            return JudgeVerdict(
                faithfulness=1,
                readability=1,
                hallucination=True,
                reason=f"judge 호출 실패: {exc}",
            )
        return _parse_verdict(response.text or "")
