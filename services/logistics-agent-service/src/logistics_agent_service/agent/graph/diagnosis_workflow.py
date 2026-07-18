from langgraph.graph import END, START, StateGraph

from logistics_agent_service.agent.node.nodes import DiagnosisNodes
from logistics_agent_service.agent.state import DiagnosisState
from logistics_agent_service.application.dto import DiagnosisQuery, DiagnosisResult
from logistics_agent_service.application.port.diagnosis_repository_port import (
    DiagnosisRepositoryPort,
)
from logistics_agent_service.application.port.order_context_port import OrderContextPort
from logistics_agent_service.application.port.report_generator_port import (
    ReportGeneratorPort,
)
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine


class LangGraphDiagnosisWorkflow:
    """DiagnosisWorkflowPort의 LangGraph 구현.

    normalize_input -> resolve_order -> collect_context -> diagnose
    -> generate_report -> persist_result
    """

    def __init__(
        self,
        order_port: OrderContextPort,
        report_port: ReportGeneratorPort,
        repository: DiagnosisRepositoryPort,
    ) -> None:
        self._nodes = DiagnosisNodes(
            order_port=order_port,
            rule_engine=RuleBasedDiagnosisEngine(),
            report_port=report_port,
            repository=repository,
        )
        self._graph = self._build_graph()

    def _build_graph(self):
        builder = StateGraph(DiagnosisState)
        builder.add_node("normalize_input", self._nodes.normalize_input)
        builder.add_node("resolve_order", self._nodes.resolve_order)
        builder.add_node("collect_context", self._nodes.collect_context)
        builder.add_node("diagnose", self._nodes.diagnose)
        builder.add_node("generate_report", self._nodes.generate_report)
        builder.add_node("persist_result", self._nodes.persist_result)

        builder.add_edge(START, "normalize_input")
        builder.add_edge("normalize_input", "resolve_order")
        builder.add_edge("resolve_order", "collect_context")
        builder.add_edge("collect_context", "diagnose")
        builder.add_edge("diagnose", "generate_report")
        builder.add_edge("generate_report", "persist_result")
        builder.add_edge("persist_result", END)
        return builder.compile()

    def run(self, query: DiagnosisQuery) -> DiagnosisResult:
        final_state = self._graph.invoke(
            {"message": query.message, "trigger_type": query.trigger_type}
        )
        return final_state["result"]
