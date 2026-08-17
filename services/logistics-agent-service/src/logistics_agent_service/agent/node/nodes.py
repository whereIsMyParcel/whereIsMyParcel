import re
import time
from collections.abc import Callable
from typing import TypeVar

from logistics_agent_service.agent.state import DiagnosisState
from logistics_agent_service.application.dto import (
    DiagnosisResult,
    ShipmentInfo,
    ToolCallRecord,
)
from logistics_agent_service.application.port.diagnosis_repository_port import (
    DiagnosisRepositoryPort,
)
from logistics_agent_service.application.port.hub_context_port import HubContextPort
from logistics_agent_service.application.port.order_context_port import OrderContextPort
from logistics_agent_service.application.port.report_generator_port import (
    ReportGeneratorPort,
)
from logistics_agent_service.application.port.shipment_context_port import (
    ShipmentContextPort,
)
from logistics_agent_service.domain.enums import TriggerType
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine

_ORDER_NUMBER = re.compile(r"ORD-\d{8}-[A-Za-z0-9]+")
_UUID = re.compile(
    r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
)

_T = TypeVar("_T")


class DiagnosisNodes:
    """LangGraph 노드 모음.

    노드는 HTTP client, DB, LLM adapter를 직접 호출하지 않고 application port와
    domain 규칙만 사용한다(design §17.4).
    """

    def __init__(
        self,
        order_port: OrderContextPort,
        shipment_port: ShipmentContextPort,
        hub_port: HubContextPort,
        rule_engine: RuleBasedDiagnosisEngine,
        report_port: ReportGeneratorPort,
        repository: DiagnosisRepositoryPort,
    ) -> None:
        self._order_port = order_port
        self._shipment_port = shipment_port
        self._hub_port = hub_port
        self._rule_engine = rule_engine
        self._report_port = report_port
        self._repository = repository

    def normalize_input(self, state: DiagnosisState) -> DiagnosisState:
        message = state.get("message") or ""
        return {"message": message.strip()}

    def resolve_order(self, state: DiagnosisState) -> DiagnosisState:
        # incident 등으로 식별자가 이미 주어졌으면 정규식 추출을 건너뛴다.
        given = state.get("order_identifier")
        if given:
            return {"order_identifier": given}
        message = state.get("message") or ""
        match = _ORDER_NUMBER.search(message) or _UUID.search(message)
        return {"order_identifier": match.group(0) if match else None}

    def collect_context(self, state: DiagnosisState) -> DiagnosisState:
        identifier = state.get("order_identifier")
        tool_calls: list[ToolCallRecord] = []
        if not identifier:
            return {
                "order_context": None,
                "shipment_statuses": None,
                "route_ok": None,
                "tool_calls": tool_calls,
            }

        order_context = self._timed(
            tool_calls,
            "get_order_context",
            {"identifier": identifier},
            lambda: self._order_port.get_order_context(identifier),
            lambda v: {
                "order_status": v.order_status.value if v.order_status else None
            },
        )
        if order_context is None:
            return {
                "order_context": None,
                "shipment_statuses": None,
                "route_ok": None,
                "tool_calls": tool_calls,
            }

        shipments = self._timed(
            tool_calls,
            "get_shipments",
            {"order_id": str(order_context.order_id)},
            lambda: self._shipment_port.get_shipments(order_context.order_id),
            lambda v: {"count": len(v), "statuses": [s.status for s in v]},
        )
        shipment_statuses = (
            [shipment.status for shipment in shipments]
            if shipments is not None
            else None
        )
        route_ok = self._check_routes(shipments, tool_calls)
        return {
            "order_context": order_context,
            "shipment_statuses": shipment_statuses,
            "route_ok": route_ok,
            "tool_calls": tool_calls,
        }

    def _timed(
        self,
        tool_calls: list[ToolCallRecord],
        tool_name: str,
        input_payload: dict,
        call: Callable[[], _T],
        summarize: Callable[[_T], dict],
    ) -> _T:
        """port 호출 latency·성공여부를 기록하며 실행한다(노드는 port만 호출, §17.4).

        결과가 None이면 조회 실패/강등으로 보고 success=False, output=None으로 남긴다.
        error_code/error_message는 port가 HTTP 상태를 감춰 채우지 않는다(nullable).
        """
        start = time.perf_counter()
        value = call()
        latency_ms = int((time.perf_counter() - start) * 1000)
        tool_calls.append(
            ToolCallRecord(
                tool_name=tool_name,
                input=input_payload,
                output=summarize(value) if value is not None else None,
                success=value is not None,
                latency_ms=latency_ms,
            )
        )
        return value

    def _check_routes(
        self, shipments: list[ShipmentInfo] | None, tool_calls: list[ToolCallRecord]
    ) -> bool | None:
        """배송 허브 쌍으로 경로 유효성을 확인한다.

        하나라도 경로 없음이면 False, 전부 유효하면 True, 확인 불가(허브 정보 없음/
        조회 실패)면 None으로 강등한다.
        """
        if not shipments:
            return None
        results: list[bool | None] = []
        for shipment in shipments:
            if shipment.origin_hub_id and shipment.destination_hub_id:
                origin, dest = shipment.origin_hub_id, shipment.destination_hub_id
                results.append(
                    self._timed(
                        tool_calls,
                        "route_exists",
                        {"origin": str(origin), "destination": str(dest)},
                        lambda o=origin, d=dest: self._hub_port.route_exists(o, d),
                        lambda v: {"route_exists": v},
                    )
                )
        if not results:
            return None
        if any(result is False for result in results):
            return False
        if all(result is True for result in results):
            return True
        return None

    def diagnose(self, state: DiagnosisState) -> DiagnosisState:
        context = state.get("order_context")
        order_status = context.order_status if context else None
        diagnosis = self._rule_engine.diagnose(
            order_status, state.get("shipment_statuses"), state.get("route_ok")
        )
        return {"diagnosis": diagnosis}

    def generate_report(self, state: DiagnosisState) -> DiagnosisState:
        report_result = self._report_port.generate(
            state["diagnosis"], state.get("order_context")
        )
        return {"report": report_result.report, "llm_trace": report_result.trace}

    def persist_result(self, state: DiagnosisState) -> DiagnosisState:
        context = state.get("order_context")
        result = DiagnosisResult(
            diagnosis=state["diagnosis"],
            report=state["report"],
            trigger_type=state.get("trigger_type", TriggerType.USER_QUERY),
            order_id=context.order_id if context else None,
            order_number=context.order_number if context else None,
            tool_calls=state.get("tool_calls", []),
            llm_trace=state.get("llm_trace"),
        )
        self._repository.save(result)
        return {"result": result}
