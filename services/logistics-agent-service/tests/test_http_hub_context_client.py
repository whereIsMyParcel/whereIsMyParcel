from uuid import uuid4

import httpx

from logistics_agent_service.infrastructure.client.http_hub_context_client import (
    HttpHubContextClient,
)


def _client(handler) -> httpx.Client:
    return httpx.Client(
        transport=httpx.MockTransport(handler),
        base_url="http://hub-service",
    )


def test_route_exists_true() -> None:
    origin, dest = uuid4(), uuid4()

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/internal/v1/hub-routes/shortest-path"
        assert request.url.params["originHubId"] == str(origin)
        return httpx.Response(200, json={"success": True, "data": {"routes": []}})

    assert HttpHubContextClient(_client(handler)).route_exists(origin, dest) is True


def test_route_not_found_is_false() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(404, json={"success": False, "data": None})

    assert HttpHubContextClient(_client(handler)).route_exists(uuid4(), uuid4()) is False


def test_business_failure_is_false() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"success": False, "data": None})

    assert HttpHubContextClient(_client(handler)).route_exists(uuid4(), uuid4()) is False


def test_server_error_is_unavailable() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, json={"success": False})

    assert HttpHubContextClient(_client(handler)).route_exists(uuid4(), uuid4()) is None


def test_transport_error_is_unavailable() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("boom")

    assert HttpHubContextClient(_client(handler)).route_exists(uuid4(), uuid4()) is None
