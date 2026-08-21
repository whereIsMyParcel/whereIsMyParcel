from fastapi import APIRouter, Depends

from logistics_agent_service.application.dto import ScanSummary
from logistics_agent_service.application.service.scheduled_scan_service import (
    ScheduledScanService,
)
from logistics_agent_service.presentation.dependencies import (
    get_scheduled_scan_service,
)

router = APIRouter(prefix="/internal/v1/agent", tags=["scan"])


@router.post("/scans", response_model=ScanSummary)
def run_scan(
    service: ScheduledScanService = Depends(get_scheduled_scan_service),
) -> ScanSummary:
    """scheduled scan 1회 수동 실행(§16.2).

    주기 스케줄러와 같은 코어(ScheduledScanService)를 공유한다. 외부 cron/k8s
    CronJob이 in-process 스케줄러 대신 이 endpoint를 호출할 수도 있다. read-only.
    """
    return service.run_once()
