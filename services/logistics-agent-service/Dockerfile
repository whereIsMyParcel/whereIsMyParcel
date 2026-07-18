FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV UV_COMPILE_BYTECODE=1
ENV UV_LINK_MODE=copy

WORKDIR /app

RUN pip install --no-cache-dir uv

# 1) 의존성만 먼저 설치 → 소스가 바뀌어도 이 레이어는 캐시 재사용
COPY services/logistics-agent-service/pyproject.toml services/logistics-agent-service/uv.lock services/logistics-agent-service/README.md ./
RUN uv sync --locked --no-dev --no-install-project

# 2) 소스 복사 후 프로젝트 자체만 추가 설치
COPY services/logistics-agent-service/src ./src
RUN uv sync --locked --no-dev

ENV PATH="/app/.venv/bin:$PATH"

EXPOSE 8090

CMD ["uvicorn", "logistics_agent_service.main:app", "--host", "0.0.0.0", "--port", "8090"]
