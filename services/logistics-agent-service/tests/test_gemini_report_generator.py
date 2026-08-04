from types import SimpleNamespace
from uuid import uuid4

from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.domain.enums import OrderStatus
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine
from logistics_agent_service.infrastructure.llm.gemini_report_generator import (
    GeminiReportGenerator,
)
from logistics_agent_service.infrastructure.llm.prompt import build_report_prompt


class _FakeModels:
    def __init__(self, text: str | None = None, error: Exception | None = None) -> None:
        self.text = text
        self.error = error
        self.calls: list[tuple[str, str]] = []

    def generate_content(self, model: str, contents: str) -> SimpleNamespace:
        self.calls.append((model, contents))
        if self.error is not None:
            raise self.error
        return SimpleNamespace(text=self.text)


class _FakeGenaiClient:
    def __init__(self, text: str | None = None, error: Exception | None = None) -> None:
        self.models = _FakeModels(text=text, error=error)


def _diagnosis():
    return RuleBasedDiagnosisEngine().diagnose(OrderStatus.COMPENSATION_FAILED, [])


def test_build_report_prompt_includes_diagnosis_fields() -> None:
    context = OrderContext(
        order_id=uuid4(),
        order_number="ORD-20260718-ABCD1234",
        order_status=OrderStatus.CONFIRMED,
    )

    prompt = build_report_prompt(_diagnosis(), context)

    assert "ORD-20260718-ABCD1234" in prompt
    assert "FAILED_COMPENSATION_FAILED" in prompt
    assert "order-service" in prompt
    assert "REVIEW_COMPENSATION_FAILURE" in prompt


def test_generate_returns_model_text() -> None:
    client = _FakeGenaiClient(text="## 운영자 리포트\n확인 필요")

    report = GeminiReportGenerator(client, "gemini-2.0-flash").generate(_diagnosis(), None)

    assert report == "## 운영자 리포트\n확인 필요"
    assert client.models.calls[0][0] == "gemini-2.0-flash"
    assert "FAILED_COMPENSATION_FAILED" in client.models.calls[0][1]


def test_generate_falls_back_on_error() -> None:
    client = _FakeGenaiClient(error=RuntimeError("boom"))

    report = GeminiReportGenerator(client, "gemini-2.0-flash").generate(_diagnosis(), None)

    assert report.startswith("[AI 리포트 생성 실패]")


def test_generate_falls_back_on_empty_text() -> None:
    client = _FakeGenaiClient(text=None)

    report = GeminiReportGenerator(client, "gemini-2.0-flash").generate(_diagnosis(), None)

    assert report.startswith("[AI 리포트 없음]")
