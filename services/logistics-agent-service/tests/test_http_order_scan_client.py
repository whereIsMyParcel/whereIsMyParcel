from uuid import uuid4

import httpx

from logistics_agent_service.domain.enums import OrderStatus
from logistics_agent_service.infrastructure.client.http_order_scan_client import (
    HttpOrderScanClient,
)


def _client(handler) -> httpx.Client:
    return httpx.Client(
        transport=httpx.MockTransport(handler),
        base_url="http://order-service",
    )


def test_maps_order_ids_and_sends_status_param() -> None:
    ids = [uuid4(), uuid4()]

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/internal/v1/orders"
        assert request.url.params["status"] == "COMPENSATION_FAILED"
        return httpx.Response(
            200,
            json={"success": True, "data": {"orderIds": [str(i) for i in ids]}},
        )

    result = HttpOrderScanClient(_client(handler)).list_order_ids_by_status(
        OrderStatus.COMPENSATION_FAILED
    )

    assert result == ids


def test_empty_list() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"success": True, "data": {"orderIds": []}})

    result = HttpOrderScanClient(_client(handler)).list_order_ids_by_status(
        OrderStatus.FAILED
    )

    assert result == []


def test_business_failure_is_empty() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"success": False, "data": None})

    result = HttpOrderScanClient(_client(handler)).list_order_ids_by_status(
        OrderStatus.FAILED
    )

    assert result == []


def test_server_error_is_empty() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, json={"success": False})

    result = HttpOrderScanClient(_client(handler)).list_order_ids_by_status(
        OrderStatus.FAILED
    )

    assert result == []


def test_transport_error_is_empty() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("boom")

    result = HttpOrderScanClient(_client(handler)).list_order_ids_by_status(
        OrderStatus.FAILED
    )

    assert result == []


def test_skips_unparseable_ids() -> None:
    good = uuid4()

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200,
            json={"success": True, "data": {"orderIds": [str(good), "not-a-uuid"]}},
        )

    result = HttpOrderScanClient(_client(handler)).list_order_ids_by_status(
        OrderStatus.FAILED
    )

    assert result == [good]


def test_forwards_system_headers() -> None:
    seen: dict[str, str] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen.update(request.headers)
        return httpx.Response(200, json={"success": True, "data": {"orderIds": []}})

    client = httpx.Client(
        transport=httpx.MockTransport(handler),
        base_url="http://order-service",
        headers={
            "X-User-Id": "00000000-0000-0000-0000-000000000001",
            "X-User-Role": "MASTER",
        },
    )
    HttpOrderScanClient(client).list_order_ids_by_status(OrderStatus.FAILED)

    assert seen["x-user-id"] == "00000000-0000-0000-0000-000000000001"
    assert seen["x-user-role"] == "MASTER"
