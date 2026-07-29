# logistics-agent-service

`logistics-agent-service` is a Python FastAPI service for logistics operations diagnosis.

The service diagnoses order and shipment incidents using internal service APIs, deterministic diagnosis rules, and Gemini-generated operator reports.

## Scope

Implemented:

- FastAPI application skeleton + health check
- uv-based Python project metadata + Dockerfile
- Diagnosis walking skeleton (S1): LangGraph workflow, rule-based engine, `POST /api/v1/agent/diagnoses/query`
- Layer-boundary enforcement (import-linter) + Python CI
- Diagnosis result persistence (S2): `agent_db` via SQLAlchemy (in-memory fallback without `DATABASE_URL`)
- Real order-service HTTP client (S3): `GET /internal/v1/orders/{orderId}` + system headers (fake fallback without `ORDER_SERVICE_BASE_URL`)

Out of current scope (later slices):

- shipment/hub internal clients + `FailureStep` rule expansion (S3b)
- orderNumber lookup — order internal API is orderId-only
- Gemini report generation — currently a template stub (S4)
- Incident endpoint (S5)
- Docker Compose integration (S7)

## Local Run

```bash
uv sync
uv run uvicorn logistics_agent_service.main:app --reload --host 0.0.0.0 --port 8090
```

## Configuration

Environment variables (all optional; unset → local fallbacks):

- `ORDER_SERVICE_BASE_URL` — order-service base URL. Unset → in-memory fake order client.
- `DATABASE_URL` — `agent_db` connection string. Unset → in-memory diagnosis store.
- `GEMINI_API_KEY` — reserved for report generation (S4).

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
