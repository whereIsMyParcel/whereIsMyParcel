from typing import Protocol

from logistics_agent_service.application.dto import JudgeVerdict, OrderContext
from logistics_agent_service.domain.models import Diagnosis


class ReportJudgePort(Protocol):
    """생성된 운영자 리포트의 품질을 채점하는 포트(design §14, S18 opt-in eval).

    규칙이 판정한 진단(diagnosis)을 ground truth로 주고, 리포트 텍스트가 그 근거에
    충실한지(faithfulness)·읽을 만한지(readability)를 JudgeVerdict로 반환한다.
    구현은 실 LLM(예: Gemini pro tier)이며 비결정적·과금이라 CI 밖 opt-in 레인에서만
    호출된다. self-preference bias를 줄이기 위해 리포트 생성 모델과 분리한다.
    """

    def judge(
        self,
        *,
        report: str,
        diagnosis: Diagnosis,
        order_context: OrderContext | None,
    ) -> JudgeVerdict: ...
