import time

import httpx


class HttpLokiLogClient:
    """LogContextPort의 Loki HTTP 구현.

    `GET /loki/api/v1/query_range`로 최근 WARN/ERROR 스트림 중 orderId를 포함한 로그
    라인을 찾는다. LogQL: `{level=~"WARN|ERROR"} |= "<orderId>"`.
    전송 오류·5xx·파싱 실패는 None으로 강등하고, 결과 없음은 빈 목록으로 반환한다.
    """

    def __init__(
        self, client: httpx.Client, window_minutes: int = 60, limit: int = 20
    ) -> None:
        self._client = client
        self._window_minutes = window_minutes
        self._limit = limit

    def search_order_logs(self, order_id: str) -> list[str] | None:
        now_ns = time.time_ns()
        start_ns = now_ns - self._window_minutes * 60 * 1_000_000_000
        # order_id는 조회된 주문의 UUID라 LogQL 인젝션 위험이 없다(사용자 입력 아님).
        query = f'{{level=~"WARN|ERROR"}} |= "{order_id}"'
        try:
            response = self._client.get(
                "/loki/api/v1/query_range",
                params={
                    "query": query,
                    "start": str(start_ns),
                    "end": str(now_ns),
                    "limit": str(self._limit),
                    "direction": "backward",
                },
            )
        except httpx.HTTPError:
            return None

        if response.is_error:
            return None

        try:
            streams = response.json()["data"]["result"]
        except (ValueError, KeyError, TypeError):
            return None

        lines: list[str] = []
        for stream in streams:
            for entry in stream.get("values", []):
                # entry = [timestamp_ns, log_line]
                if len(entry) >= 2:
                    lines.append(entry[1])
        return lines[: self._limit]
