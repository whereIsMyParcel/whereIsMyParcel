from uuid import UUID

import httpx


class HttpHubContextClient:
    """HubContextPort의 hub-service HTTP 구현.

    `GET /internal/v1/hub-routes/shortest-path?originHubId=&destinationHubId=`.
    경로 있음 → True, 없음(404/business-failure) → False, 조회 불가(전송 오류·5xx)
    → None으로 강등한다.
    """

    def __init__(self, client: httpx.Client) -> None:
        self._client = client

    def route_exists(
        self, origin_hub_id: UUID, destination_hub_id: UUID
    ) -> bool | None:
        try:
            response = self._client.get(
                "/internal/v1/hub-routes/shortest-path",
                params={
                    "originHubId": str(origin_hub_id),
                    "destinationHubId": str(destination_hub_id),
                },
            )
        except httpx.HTTPError:
            return None

        if response.status_code == httpx.codes.NOT_FOUND:
            return False
        if response.is_error:
            return None

        body = response.json()
        return bool(body.get("success") and body.get("data") is not None)
