from uuid import UUID


class FakeHubContextClient:
    """서비스 없이 실행/테스트할 때 쓰는 HubContextPort 구현.

    기본값은 경로 존재(True)라, Fake 모드에서 정상 배송이 경로 무효로 오진되지 않는다.
    경로 없음/조회 불가 시나리오는 route_exists 값으로 주입한다.
    """

    def __init__(self, route_exists: bool | None = True) -> None:
        self._route_exists = route_exists

    def route_exists(
        self, origin_hub_id: UUID, destination_hub_id: UUID
    ) -> bool | None:
        return self._route_exists
