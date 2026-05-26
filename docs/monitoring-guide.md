# 모니터링 가이드

## 개요

whereIsMyParcel은 Prometheus, Grafana, Loki, Zipkin, Sentry를 조합하여 메트릭 수집, 로그 집계, 분산 트레이싱, 에러 추적을 통합 관리한다.

```
서비스 → Prometheus (메트릭 스크래핑)
             ↓
           Grafana (대시보드 시각화)

서비스 → Loki (로그 푸시 via Loki4j)
             ↓
           Grafana (로그 조회)

서비스 → Zipkin (트레이스 전송)

서비스 → Sentry (에러 이벤트 전송 + Slack 알림)
```

---

## 인프라 구성

`infra/docker-compose.yml`에 모니터링 스택이 정의되어 있다.

| 컨테이너    | 이미지                     | 포트  | 역할                      |
|------------|--------------------------|-------|--------------------------|
| prometheus | prom/prometheus:latest    | 9090  | 메트릭 수집 및 저장          |
| grafana    | grafana/grafana:latest    | 3000  | 메트릭·로그 시각화 대시보드    |
| loki       | grafana/loki:latest       | 3100  | 로그 집계 저장소             |
| zipkin     | openzipkin/zipkin:3       | 9411  | 분산 트레이싱 수집 및 조회    |
| sentry     | sentry.io (외부 SaaS)     | -     | 에러 추적 및 알림            |

### 로컬 실행

```bash
# user-service 로컬 개발 시 최소 구성
docker compose -f infra/docker-compose.yml up -d postgres keycloak loki

# 모니터링 전체 스택
docker compose -f infra/docker-compose.yml up -d prometheus grafana loki zipkin

# 전체 인프라 실행
docker compose -f infra/docker-compose.yml up -d
```

---

## Prometheus 메트릭 수집

### 설정 파일

`infra/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8081']
  # ... 나머지 서비스 동일한 패턴
```

- 스크래핑 주기: **15초**
- 엔드포인트: 각 서비스의 `/actuator/prometheus`
- Docker 컨테이너에서 호스트 서비스에 접근하기 위해 `host.docker.internal` 사용

### 서비스별 포트

| 서비스             | 포트  |
|-------------------|-------|
| api-gateway        | 8000  |
| user-service       | 8081  |
| hub-service        | 8082  |
| company-service    | 8083  |
| order-service      | 8084  |
| shipment-service   | 8085  |
| ai-slack-service   | 8086  |

### 서비스 측 설정

각 서비스의 `application.yaml`에 actuator 엔드포인트를 노출한다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
```

### 의존성 (루트 `build.gradle`)

```groovy
implementation 'io.micrometer:micrometer-registry-prometheus'
```

---

## Grafana 대시보드

### 접속 정보

- URL: `http://localhost:3000`
- 기본 계정: `admin / admin`

### 데이터소스

`infra/grafana/datasources/datasource.yml`에 Prometheus와 Loki가 자동 프로비저닝된다.

```yaml
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
    isDefault: true

  - name: Loki
    type: loki
    url: http://loki:3100
```

---

## Loki 로그 수집

Loki4j Logback Appender를 사용하여 서비스 로그를 Loki로 직접 푸시한다.

### 설정 파일

`services/user-service/src/main/resources/logback-spring.xml`

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>${LOKI_URL:-http://localhost:3100}/loki/api/v1/push</url>
    </http>
    <format>
        <label>
            <pattern>app=${appName},level=%level,host=${HOSTNAME}</pattern>
        </label>
        <message>
            <pattern>level=%level traceId=%X{traceId} spanId=%X{spanId} userId=%X{userId} username=%X{username} message="%msg"%n%ex</pattern>
        </message>
    </format>
</appender>
```

### 로그 레이블

Loki에 저장될 때 다음 레이블로 필터링할 수 있다.

| 레이블   | 예시             | 설명        |
|---------|----------------|------------|
| `app`   | `user-service` | 서비스 이름  |
| `level` | `INFO`, `ERROR` | 로그 레벨   |
| `host`  | `hostname`      | 호스트명    |

### 로그 메시지 형식

```
level=INFO traceId=abc123 spanId=def456 userId=uuid username=user01 message="요청 처리 완료"
```

- `traceId`, `spanId`: Zipkin MDC에서 자동 주입
- `userId`, `username`: `MdcLoggingFilter`가 게이트웨이 헤더(`X-User-Id`, `X-Username`)에서 읽어 주입

### 의존성 (루트 `build.gradle`)

```groovy
implementation 'com.github.loki4j:loki-logback-appender:1.5.2'
```

### 환경변수

| 변수        | 기본값                   | 설명          |
|------------|------------------------|--------------|
| `LOKI_URL` | `http://localhost:3100` | Loki 서버 URL |

---

## Zipkin 분산 트레이싱

서비스 간 요청 흐름을 추적하기 위해 Micrometer Tracing + Brave + Zipkin 조합을 사용한다.

### 접속 정보

- URL: `http://localhost:9411`

### 서비스 측 설정

`services/user-service/src/main/resources/application.yaml`

```yaml
management:
  tracing:
    sampling:
      probability: 1.0   # 100% 샘플링 (개발 환경)
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_URL:http://localhost:9411}/api/v2/spans
```

> 운영 환경에서는 `probability`를 `0.1` ~ `0.3` 수준으로 낮추는 것을 권장한다.

### 의존성 (루트 `build.gradle`)

```groovy
implementation 'io.micrometer:micrometer-tracing-bridge-brave'
implementation 'io.zipkin.reporter2:zipkin-reporter-brave'
```

### 환경변수

| 변수               | 기본값                   | 설명             |
|-------------------|------------------------|----------------|
| `ZIPKIN_ENDPOINT` | `http://localhost:9411` | Zipkin 서버 URL |

---

## Sentry 에러 추적

5xx 예외를 실시간으로 캡처하여 Sentry 대시보드 및 Slack으로 알림을 보낸다.

### 아키텍처

```
요청
 └─ MdcLoggingFilter (Order 1)   : X-User-Id, X-Username → MDC
 └─ SentryTraceFilter (Order 2)  : MDC(traceId, userId, username) → Sentry 태그
 └─ Controller / Service
 └─ GlobalExceptionHandler        : Sentry.captureException() — uri, method 태그 포함
```

- `BusinessException` (4xx): Sentry 이벤트 미발행, WARN 로그만 기록
- `Exception` (5xx): Sentry 이벤트 발행, 태그(`uri`, `method`, `traceId`, `userId`) 포함
- `SentryAppender`: 이벤트 발행 비활성화(`OFF`), INFO 이상 로그는 Breadcrumb으로만 기록

### 서비스 측 설정

```yaml
sentry:
  dsn: ${SENTRY_DSN:}
  environment: ${SPRING_PROFILES_ACTIVE:local}
  traces-sample-rate: 1.0
  send-default-pii: false
```

- `SENTRY_DSN` 미설정 시 Sentry 자동 비활성화

### 의존성 (루트 `build.gradle`)

```groovy
implementation 'io.sentry:sentry-spring-boot-starter-jakarta:7.22.0'
implementation 'io.sentry:sentry-logback:7.22.0'
```

### Slack 알림 연동

코드 변경 없이 Sentry 대시보드에서 설정한다.

1. 프로젝트 → **Settings** → **Integrations** → Slack 연동
2. **Alerts** → **Create Alert Rule**
3. 조건: `An issue is first seen` 또는 `occurs more than N times`
4. Action: **Send a Slack message** → 채널 지정

### 환경변수

| 변수          | 설명                                    |
|--------------|----------------------------------------|
| `SENTRY_DSN` | Sentry 프로젝트 DSN (sentry.io에서 발급) |

---

## 환경변수 요약

`.env` 파일 또는 Docker Compose `environment` 블록에서 설정한다.

| 변수               | 설명                    |
|-------------------|------------------------|
| `ZIPKIN_ENDPOINT` | Zipkin 트레이스 수신 URL  |
| `LOKI_URL`        | Loki 로그 푸시 URL       |
| `SENTRY_DSN`      | Sentry 에러 수집 DSN     |
