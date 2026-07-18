# logistics-agent-service

`logistics-agent-service` is a Python FastAPI service for logistics operations diagnosis.

The service diagnoses order and shipment incidents using internal service APIs, deterministic diagnosis rules, and Gemini-generated operator reports.

## Scope

Implemented:

- FastAPI application skeleton + health check
- uv-based Python project metadata + Dockerfile
- Diagnosis walking skeleton (S1): LangGraph workflow, rule-based engine, `POST /api/v1/agent/diagnoses/query`
- Layer-boundary enforcement (import-linter) + Python CI

Out of current scope (later slices):

- Real order-service HTTP client — currently an in-memory fake (S3)
- Database persistence (S2)
- Gemini report generation — currently a template stub (S4)
- Incident endpoint (S5)
- Docker Compose integration (S7)

## Local Run

```bash
uv sync
uv run uvicorn logistics_agent_service.main:app --reload --host 0.0.0.0 --port 8090
```

## Endpoints

Health check:

```bash
curl http://localhost:8090/health
```

Diagnosis query (S1):

```bash
curl -X POST http://localhost:8090/api/v1/agent/diagnoses/query \
  -H 'content-type: application/json' \
  -d '{"message": "ORD-20260718-COMPFAIL 왜 실패했어?"}'
```

## Checks

```bash
uv run ruff check .
uv run pytest
uv run lint-imports
```
