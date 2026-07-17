FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

COPY services/logistics-agent-service/pyproject.toml ./
COPY services/logistics-agent-service/README.md ./
COPY services/logistics-agent-service/src ./src

RUN pip install --no-cache-dir uv \
    && uv pip install --system .

EXPOSE 8090

CMD ["uvicorn", "logistics_agent_service.main:app", "--host", "0.0.0.0", "--port", "8090"]
