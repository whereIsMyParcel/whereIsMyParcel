import re

from logistics_agent_service.agent.state import DiagnosisState
from logistics_agent_service.application.dto import DiagnosisResult, ShipmentInfo
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
        if not identifier:
            return {"order_context": None, "shipment_statuses": None, "route_ok": None}

        order_context = self._order_port.get_order_context(identifier)
        if order_context is None:
            return {"order_context": None, "shipment_statuses": None, "route_ok": None}

        shipments = self._shipment_port.get_shipments(order_context.order_id)
        shipment_statuses = (
            [shipment.status for shipment in shipments]
            if shipments is not None
            else None
        )
        route_ok = self._check_routes(shipments)
        return {
            "order_context": order_context,
            "shipment_statuses": shipment_statuses,
            "route_ok": route_ok,
        }

    def _check_routes(self, shipments: list[ShipmentInfo] | None) -> bool | None:
        """배송 허브 쌍으로 경로 유효성을 확인한다.

        하나라도 경로 없음이면 False, 전부 유효하면 True, 확인 불가(허브 정보 없음/
        조회 실패)면 None으로 강등한다.
        """
        if not shipments:
            return None
        results: list[bool | None] = []
        for shipment in shipments:
            if shipment.origin_hub_id and shipment.destination_hub_id:
                results.append(
                    self._hub_port.route_exists(
                        shipment.origin_hub_id, shipment.destination_hub_id
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
