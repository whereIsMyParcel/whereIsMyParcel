from uuid import uuid4

import httpx

from logistics_agent_service.infrastructure.client.http_order_context_client import (
    HttpOrderContextClient,
)


def _client(handler) -> httpx.Client:
    return httpx.Client(
        transport=httpx.MockTransport(handler),
        base_url="http://order-service",
    )


def test_maps_order_ai_context_response() -> None:
    order_id = uuid4()

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == f"/internal/v1/orders/{order_id}"
        return httpx.Response(
            200,
            json={
                "success": True,
                "status": 200,
                "message": "OK",
                "data": {
                    "orderId": str(order_id),
                    "orderNumber": "ORD-20260718-ABCD1234",
                    "orderStatus": "COMPENSATION_FAILED",
                },
            },
        )

    context = HttpOrderContextClient(_client(handler)).get_order_context(str(order_id))

    assert context is not None
    assert context.order_id == order_id
    assert context.order_number == "ORD-20260718-ABCD1234"
    assert context.order_status.value == "COMPENSATION_FAILED"


def test_unknown_order_status_maps_to_none() -> None:
    order_id = uuid4()

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200,
            json={
                "success": True,
                "data": {
                    "orderId": str(order_id),
                    "orderNumber": "ORD-20260718-ABCD1234",
                    "orderStatus": "WARP_SPEED",
                },
            },
        )

    context = HttpOrderContextClient(_client(handler)).get_order_context(str(order_id))

    assert context is not None
    assert context.order_id == order_id
    assert context.order_number == "ORD-20260718-ABCD1234"
    assert context.order_status is None


def test_returns_none_on_404() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(404, json={"success": False, "data": None})

    assert HttpOrderContextClient(_client(handler)).get_order_context(str(uuid4())) is None


def test_returns_none_on_business_failure() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"success": False, "data": None})

    assert HttpOrderContextClient(_client(handler)).get_order_context(str(uuid4())) is None


def test_server_error_is_none() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, json={"success": False})

    assert HttpOrderContextClient(_client(handler)).get_order_context(str(uuid4())) is None


def test_transport_error_is_none() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("boom")

    assert HttpOrderContextClient(_client(handler)).get_order_context(str(uuid4())) is None


def test_non_uuid_identifier_makes_no_request() -> None:
    calls: list[httpx.Request] = []

    def handler(request: httpx.Request) -> httpx.Response:
        calls.append(request)
        return httpx.Response(200, json={"success": True, "data": {}})

    result = HttpOrderContextClient(_client(handler)).get_order_context("ORD-20260718-ABCD1234")

    assert result is None
    assert calls == []


def test_forwards_system_headers() -> None:
    order_id = uuid4()
    seen: dict[str, str] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen.update(request.headers)
        return httpx.Response(
            200,
            json={
                "success": True,
                "data": {
                    "orderId": str(order_id),
                    "orderNumber": "ORD-20260718-ABCD1234",
                    "orderStatus": "CONFIRMED",
                },
            },
        )

    client = httpx.Client(
        transport=httpx.MockTransport(handler),
        base_url="http://order-service",
        headers={
            "X-User-Id": "00000000-0000-0000-0000-000000000001",
            "X-User-Role": "MASTER",
        },
    )
    HttpOrderContextClient(client).get_order_context(str(order_id))

    assert seen["x-user-id"] == "00000000-0000-0000-0000-000000000001"
    assert seen["x-user-role"] == "MASTER"
