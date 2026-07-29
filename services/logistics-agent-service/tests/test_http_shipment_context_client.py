from uuid import uuid4

import httpx

from logistics_agent_service.infrastructure.client.http_shipment_context_client import (
    HttpShipmentContextClient,
)


def _client(handler) -> httpx.Client:
    return httpx.Client(
        transport=httpx.MockTransport(handler),
        base_url="http://shipment-service",
    )


def test_maps_shipment_statuses() -> None:
    order_id = uuid4()

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == f"/internal/v1/shipments/{order_id}"
        return httpx.Response(
            200,
            json={
                "success": True,
                "data": [
                    {"shipmentStatus": "IN_TRANSIT"},
                    {"shipmentStatus": "DELIVERED"},
                ],
            },
        )

    statuses = HttpShipmentContextClient(_client(handler)).get_shipment_statuses(order_id)

    assert statuses == ["IN_TRANSIT", "DELIVERED"]


def test_empty_list_means_no_shipments() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"success": True, "data": []})

    assert HttpShipmentContextClient(_client(handler)).get_shipment_statuses(uuid4()) == []


def test_404_means_no_shipments() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(404, json={"success": False, "data": None})

    assert HttpShipmentContextClient(_client(handler)).get_shipment_statuses(uuid4()) == []


def test_server_error_is_unavailable() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, json={"success": False})

    assert HttpShipmentContextClient(_client(handler)).get_shipment_statuses(uuid4()) is None


def test_transport_error_is_unavailable() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("boom")

    assert HttpShipmentContextClient(_client(handler)).get_shipment_statuses(uuid4()) is None


def test_business_failure_is_unavailable() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"success": False, "data": None})

    assert HttpShipmentContextClient(_client(handler)).get_shipment_statuses(uuid4()) is None
