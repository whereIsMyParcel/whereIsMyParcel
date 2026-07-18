import re

from logistics_agent_service.agent.state import DiagnosisState
from logistics_agent_service.application.dto import DiagnosisResult
from logistics_agent_service.application.port.diagnosis_repository_port import (
    DiagnosisRepositoryPort,
)
from logistics_agent_service.application.port.order_context_port import OrderContextPort
from logistics_agent_service.application.port.report_generator_port import (
    ReportGeneratorPort,
)
from logistics_agent_service.domain.enums import TriggerType
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine

_ORDER_NUMBER = re.compile(r"ORD-\d{8}-[A-Za-z0-9]+")
_UUID = re.compile(
    r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
)


class DiagnosisNodes:
    """LangGraph 노드 모음.

    노드는 HTTP client, DB, LLM adapter를 직접 호출하지 않고 application port와
    domain 규칙만 사용한다(design §17.4).
    """

    def __init__(
        self,
        order_port: OrderContextPort,
        rule_engine: RuleBasedDiagnosisEngine,
        report_port: ReportGeneratorPort,
        repository: DiagnosisRepositoryPort,
    ) -> None:
        self._order_port = order_port
        self._rule_engine = rule_engine
        self._report_port = report_port
        self._repository = repository

    def normalize_input(self, state: DiagnosisState) -> DiagnosisState:
        message = state.get("message") or ""
        return {"message": message.strip()}

    def resolve_order(self, state: DiagnosisState) -> DiagnosisState:
        message = state.get("message") or ""
        match = _ORDER_NUMBER.search(message) or _UUID.search(message)
        return {"order_identifier": match.group(0) if match else None}

    def collect_context(self, state: DiagnosisState) -> DiagnosisState:
        identifier = state.get("order_identifier")
        if not identifier:
            return {"order_context": None}
        return {"order_context": self._order_port.get_order_context(identifier)}

    def diagnose(self, state: DiagnosisState) -> DiagnosisState:
        context = state.get("order_context")
        order_status = context.order_status if context else None
        return {"diagnosis": self._rule_engine.diagnose(order_status)}

    def generate_report(self, state: DiagnosisState) -> DiagnosisState:
        report = self._report_port.generate(state["diagnosis"], state.get("order_context"))
        return {"report": report}

    def persist_result(self, state: DiagnosisState) -> DiagnosisState:
        context = state.get("order_context")
        result = DiagnosisResult(
            diagnosis=state["diagnosis"],
            report=state["report"],
            trigger_type=state.get("trigger_type", TriggerType.USER_QUERY),
            order_id=context.order_id if context else None,
            order_number=context.order_number if context else None,
        )
        self._repository.save(result)
        return {"result": result}
