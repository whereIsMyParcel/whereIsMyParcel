# logistics-agent-service

`logistics-agent-service` is a Python FastAPI service for logistics operations diagnosis.

The service diagnoses order and shipment incidents using internal service APIs, deterministic diagnosis rules, and Gemini-generated operator reports. Rules classify (failure/compensation/consistency); the LLM only writes the human-readable report. Every action is read-only.

Design: `docs/logistics-agent-service/design.md`.

## Scope

Implemented:

- FastAPI skeleton + health check, uv project metadata + Dockerfile, Docker Compose integration (S7)
- Diagnosis core: LangGraph workflow + rule-based engine (S1)
- Persistence: `agent_db` via SQLAlchemy — diagnosis / evidence / tool_call / llm_trace / action_proposal (S2, S8, S12); in-memory fallback without `DATABASE_URL`
- Internal-API tools with system headers (§8.3): order (S3), shipment (S3b), hub route (S3b2); fake fallbacks without the matching base URL
- Gemini report generation + `agent_llm_trace`; template stub without `GEMINI_API_KEY` (S4)
- Triggers sharing one diagnosis core: user query (S1), system incident (S5), scheduled scan (S15)
- Eval: JSONL dataset + accuracy / schema-adherence report (S6); diagnosis → SFT/eval dataset export (S10)
- Loki error-log tool: saga ERROR lines as diagnosis evidence, and `failed_step` refinement from them (S11, S13)
- Scheduled scan: enumerate failure candidates by order status and pre-diagnose (S14 order read API + S15 agent trigger)
- Order↔shipment status consistency rules (S16): cancelled-shipment / orphan-shipment / incomplete-shipment risks

Out of current scope (read-only lane; deferred):

- orderNumber lookup — order internal API is orderId-only
- write / recovery actions — need human-in-the-loop approval + other-team recovery write APIs
- Slack notification, operations dashboard, Zipkin trace tool (needs span orderId tagging)

## Local Run

```bash
uv sync
uv run uvicorn logistics_agent_service.main:app --reload --host 0.0.0.0 --port 8090
```

## Configuration

Environment variables (all optional; unset → local fallbacks):

- `ORDER_SERVICE_BASE_URL` / `SHIPMENT_SERVICE_BASE_URL` / `HUB_SERVICE_BASE_URL` — internal API base URLs. Unset → in-memory fake clients.
- `DATABASE_URL` — `agent_db` connection string. Unset → in-memory diagnosis store.
- `GEMINI_API_KEY` — report generation. Unset → template stub report. `GEMINI_MODEL` overrides the model.
- `LOKI_BASE_URL` — Loki query endpoint for the error-log tool. Unset → fake log client. `LOKI_SEARCH_WINDOW_MINUTES` / `LOKI_SEARCH_LIMIT` bound the search.
- `SCAN_ENABLED` — enable the in-process periodic scan scheduler (default `false`). `SCAN_INTERVAL_SECONDS` (default 300) and `SCAN_STATUSES` (default `COMPENSATION_FAILED`, `FAILED`) tune it. The manual scan endpoint works regardless.
- `INTERNAL_USER_ID` / `INTERNAL_USERNAME` / `INTERNAL_USER_ROLE` / `INTERNAL_USER_STATUS` — service-account system headers for internal API calls (§8.3).

## Endpoints

Health check:

```bash
curl http://localhost:8090/health
```

Diagnosis query (user):

```bash
curl -X POST http://localhost:8090/api/v1/agent/diagnoses/query \
  -H 'content-type: application/json' \
  -d '{"message": "ORD-20260718-COMPFAIL 왜 실패했어?"}'
```

Incident (system trigger, internal):

```bash
curl -X POST http://localhost:8090/internal/v1/agent/incidents \
  -H 'content-type: application/json' \
  -d '{"incidentType": "ORDER_FAILED", "sourceService": "order-service", "orderId": "<uuid>", "message": "Order create saga failed"}'
```

Scheduled scan — run one pass now (internal):

```bash
curl -X POST http://localhost:8090/internal/v1/agent/scans
```

## Checks

```bash
uv run ruff check .
uv run pytest
uv run lint-imports
```
