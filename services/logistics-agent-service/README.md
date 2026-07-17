# logistics-agent-service

`logistics-agent-service` is a Python FastAPI service for logistics operations diagnosis.

The service is intended to diagnose order and shipment incidents using internal service APIs, deterministic diagnosis rules, and Gemini-generated operator reports.

## Scope

Initial bootstrap scope:

- FastAPI application skeleton
- Health check endpoint
- uv-based Python project metadata
- Dockerfile draft

Out of current scope:

- LangGraph workflow implementation
- Gemini API integration
- database persistence
- internal API clients
- Docker Compose integration
- Python CI workflow

## Local Run

```bash
uv sync
uv run uvicorn logistics_agent_service.main:app --reload --host 0.0.0.0 --port 8090
```

## Health Check

```bash
curl http://localhost:8090/health
```
