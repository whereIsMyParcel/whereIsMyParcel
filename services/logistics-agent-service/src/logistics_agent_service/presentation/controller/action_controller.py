from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException

from logistics_agent_service.application.dto import RecoveryResult
from logistics_agent_service.application.service.recovery_service import (
    ActionNotFoundError,
    RecoveryService,
    UnsupportedActionError,
)
from logistics_agent_service.presentation.dependencies import get_recovery_service

router = APIRouter(prefix="/internal/v1/agent", tags=["actions"])


@router.post("/actions/{action_id}/approve", response_model=RecoveryResult)
def approve_action(
    action_id: UUID,
    service: RecoveryService = Depends(get_recovery_service),
) -> RecoveryResult:
    """조치 제안 승인 + 실행(design §16.4 T5b).

    승인은 곧바로 실행을 트리거한다(동기). 실행 직전 orphan 재검증을 통과할 때만
    실제 배송 취소가 일어난다. 이미 처리된 제안은 멱등적으로 현재 상태를 반환한다.

    주의: 승인 주체(운영자) 인증은 아직 구현하지 않았다(현 internal permitAll 정책,
    §8.2). 승인자 검증은 후속 인증 인프라에서 붙인다.
    """
    try:
        return service.approve_and_execute(action_id)
    except ActionNotFoundError as exc:
        raise HTTPException(status_code=404, detail="조치 제안을 찾을 수 없습니다.") from exc
    except UnsupportedActionError as exc:
        raise HTTPException(
            status_code=422, detail=f"실행 가능한 recovery 조치가 아닙니다: {exc}"
        ) from exc
