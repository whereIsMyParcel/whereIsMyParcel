FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV UV_COMPILE_BYTECODE=1
ENV UV_LINK_MODE=copy

WORKDIR /app

RUN pip install --no-cache-dir uv

# 커밋된 uv.lock을 그대로 사용해 재현 가능한 설치 (dev 그룹 제외)
COPY services/logistics-agent-service/pyproject.toml services/logistics-agent-service/uv.lock services/logistics-agent-service/README.md ./
COPY services/logistics-agent-service/src ./src

RUN uv sync --locked --no-dev

ENV PATH="/app/.venv/bin:$PATH"

EXPOSE 8090

CMD ["uvicorn", "logistics_agent_service.main:app", "--host", "0.0.0.0", "--port", "8090"]
