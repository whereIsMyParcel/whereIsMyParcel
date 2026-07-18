from fastapi.testclient import TestClient

from logistics_agent_service.main import app


def test_health() -> None:
    client = TestClient(app)

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "UP"
    assert response.json()["service"] == "logistics-agent-service"
