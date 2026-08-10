from typing import Protocol
from uuid import UUID


class HubContextPort(Protocol):
    """허브 간 경로 조회 포트.

    `route_exists(origin, destination)`:
    - `True`: 경로 존재
    - `False`: 경로 없음 또는 허브 무효
    - `None`: 조회 불가(서비스 오류/타임아웃 등) → 경로 판정 강등
    """

    def route_exists(
        self, origin_hub_id: UUID, destination_hub_id: UUID
    ) -> bool | None: ...
