from logistics_agent_service.application.dto import ScanSummary
from logistics_agent_service.application.port.diagnosed_order_port import (
    DiagnosedOrderPort,
)
from logistics_agent_service.application.port.order_scan_port import OrderScanPort
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.domain.enums import OrderStatus


class ScheduledScanService:
    """주기 스캔 유스케이스(design §16.2).

    상태별 고장 후보 orderId를 열거해, 아직 진단하지 않은 건만 기존 진단
    코어(§5)로 선제 진단한다. 스캔은 진단·기록만 하며 write/recovery는 하지
    않는다(read-only, §10).

    중복 방지: 이미 진단 이력이 있는 orderId는 skip한다(단순 존재 여부).
    한 스캔 안에서 여러 상태에 중복 등장하는 orderId도 한 번만 진단한다.
    """

    def __init__(
        self,
        scan_port: OrderScanPort,
        diagnosed_port: DiagnosedOrderPort,
        diagnosis_service: DiagnosisService,
        scan_statuses: list[OrderStatus],
    ) -> None:
        self._scan_port = scan_port
        self._diagnosed_port = diagnosed_port
        self._diagnosis_service = diagnosis_service
        self._scan_statuses = scan_statuses

    def run_once(self) -> ScanSummary:
        seen = set(self._diagnosed_port.list_diagnosed_order_ids())
        summary = ScanSummary()
        for status in self._scan_statuses:
            for order_id in self._scan_port.list_order_ids_by_status(status):
                summary.scanned += 1
                if order_id in seen:
                    summary.skipped += 1
                    continue
                result = self._diagnosis_service.diagnose_scanned(str(order_id))
                seen.add(order_id)
                summary.diagnosed += 1
                if result.diagnosis_id is not None:
                    summary.diagnosis_ids.append(result.diagnosis_id)
        return summary
