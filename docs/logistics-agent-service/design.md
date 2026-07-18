# logistics-agent-service 설계 가이드

## 1. 목표

`logistics-agent-service`는 WhereIsMyParcel의 운영 진단용 AI Agent 서비스입니다.

기존 `ai-slack-service`는 주문 생성 이후 납기 계산과 Slack 알림을 담당하는 업무 자동화 서비스로 유지합니다. 새 서비스는 운영자가 자연어로 주문/배송 실패 원인을 질의하거나, 시스템 incident가 발생했을 때 자동으로 진단을 시작하는 별도 Agent 서버로 둡니다.

핵심 목표는 다음과 같습니다.

- 주문/배송 실패 원인 진단
- 실패 단계와 보상 트랜잭션 상태 분류
- 근거 데이터 기반 incident report 생성
- 운영자 조치 제안
- 진단 결과, tool call trace, LLM 입출력, eval/SFT dataset 저장

## 2. 확정 기술 스택

- Runtime: Python
- API Server: FastAPI
- Agent Orchestration: LangGraph
- LLM: Gemini API
- Database: PostgreSQL `agent_db` schema
- 초기 액션 범위: read-only 진단 및 결과 저장

Spring AI도 기존 Spring MSA와의 통합성은 좋지만, LangGraph의 state graph, persistence, human-in-the-loop 패턴이 incident 진단 및 승인 기반 복구 흐름과 더 잘 맞으므로 Python + FastAPI + LangGraph를 우선 선택합니다.

## 3. 기존 ai-slack-service와 책임 분리

### ai-slack-service

- 주문 생성 이후 `orderId` 기반 분석 요청 수신
- 납기/출고 상한 계산
- `Order.finalDispatchDeadline` 업데이트
- 배송 담당자 Slack 알림

### logistics-agent-service

- 운영자 자연어 질의 수신
- 시스템 incident 수신
- 여러 서비스의 internal API를 tool로 호출
- 실패 유형과 보상 상태 진단
- 운영자용 진단 리포트 생성
- 후속 액션 후보 생성
- trace/eval/SFT dataset 저장

## 4. Trigger Flow

### 4.1 User Query Flow

운영자가 자연어로 질문하는 흐름입니다.

예시 입력:

```json
{
  "message": "ORD-20260717-ABCD 주문 왜 실패했어?"
}
```

흐름:

```text
User
-> POST /api/v1/agent/diagnoses/query
-> IntentRouter
-> orderId/orderNumber 추출
-> DiagnosisWorkflow
-> ContextCollector
-> RuleBasedDiagnosisEngine
-> Gemini Report Generator
-> 결과 저장
-> API 응답
```

### 4.2 Incident Flow

시스템에서 incident가 발생했을 때 Agent가 먼저 진단을 시작하는 흐름입니다.

예시 입력:

```json
{
  "incidentType": "ORDER_FAILED",
  "sourceService": "order-service",
  "orderId": "00000000-0000-0000-0000-000000000000",
  "message": "Order create saga failed"
}
```

흐름:

```text
Source Service
-> POST /internal/v1/agent/incidents
-> IncidentNormalizer
-> DiagnosisWorkflow
-> ContextCollector
-> RuleBasedDiagnosisEngine
-> Gemini Report Generator
-> 결과 저장
-> action proposal 저장
```

MVP에서는 scheduled scan은 구현하지 않습니다. 단, 향후 `FAILED` 상태 주문이나 정합성 이상 데이터를 주기적으로 스캔할 수 있도록 trigger type은 확장 가능하게 둡니다.

## 5. Diagnosis Workflow

두 trigger는 같은 진단 core를 사용합니다.

```text
1. input normalize
2. order identifier resolve
3. context collect
4. evidence persist
5. rule-based diagnosis
6. LLM report generate
7. action proposal generate
8. diagnosis persist
9. response or notification draft return
```

LLM이 비즈니스 판단을 단독으로 수행하지 않도록 합니다. 실패 상태, 실패 단계, 보상 상태는 우선 코드 규칙으로 판정하고, LLM은 근거를 기반으로 사람이 읽기 좋은 리포트를 생성합니다.

## 6. Taxonomy

### 6.1 TriggerType

```text
USER_QUERY
SYSTEM_INCIDENT
SCHEDULED_SCAN
```

### 6.2 IncidentType

```text
ORDER_FAILED
SHIPMENT_CREATION_FAILED
INVENTORY_COMPENSATION_FAILED
AI_ANALYSIS_FAILED
SLACK_NOTIFICATION_FAILED
DATA_INCONSISTENCY_DETECTED
UNKNOWN
```

### 6.3 DiagnosisStatus

```text
NORMAL
FAILED_COMPENSATED
FAILED_COMPENSATION_FAILED
MANUAL_INTERVENTION_REQUIRED
RISK_DETECTED
UNKNOWN
```

### 6.4 FailureStep

```text
ORDER_CREATION
PRODUCT_VALIDATION
INVENTORY_RESERVATION
PRODUCT_HUB_LOOKUP
RECIPIENT_HUB_LOOKUP
HUB_ROUTE_LOOKUP
SHIPMENT_CREATION
AI_ANALYSIS
SLACK_NOTIFICATION
UNKNOWN
```

### 6.5 CompensationStatus

```text
NOT_REQUIRED
COMPLETED
FAILED
PARTIAL
UNKNOWN
```

### 6.6 ActionRiskLevel

```text
READ_ONLY
SAFE_WRITE
RECOVERY_WRITE
DANGEROUS_MANUAL
```

## 7. 초기 Rule 예시

초기 rule은 보수적으로 둡니다. 근거가 부족하면 확정적으로 말하지 않고 `UNKNOWN` 또는 `MANUAL_INTERVENTION_REQUIRED`로 분류합니다.

```text
Order.status = FAILED
+ Shipment 없음
+ Inventory reservedQuantity = 0 또는 예약 없음
=> FAILED_COMPENSATED
```

```text
Order.status = FAILED
+ Shipment 없음
+ Inventory reservedQuantity > 0
=> FAILED_COMPENSATION_FAILED
```

```text
Order.status = CONFIRMED
+ Shipment 없음
=> MANUAL_INTERVENTION_REQUIRED
```

```text
Order.status = CONFIRMED
+ Shipment 있음
+ same-hub 배송
+ ShipmentHistory 없음
=> NORMAL 또는 RISK_DETECTED
```

same-hub 배송은 배송 경로 테이블에 history가 없을 수 있으므로, shipment status와 shipment item 존재 여부를 함께 확인해야 합니다.

### 7.1 보상 상태 판정 출처 (중요)

보상 상태(`compensationStatus`)의 **1차 판정 출처는 order-service의 Order 상태 머신**이며, company의 `reservedQuantity`가 아니다.

이유: company의 `Inventory`는 `(hubId × productVariant)` 단위 **집계** `reservedQuantity`만 보유하고 orderId를 갖지 않는다. 예약 원장을 Order 쪽(OrderItem)에 둔 결정(주문 취소 재고 원복 설계) 때문에, company 집계로는 "이 주문의 보상 상태"를 알 수 없다. 동일 SKU에 동시 주문이 있으면 `reservedQuantity > 0`이 대상 주문의 보상 실패를 뜻하지 않는다(다른 정상 주문의 예약일 수 있음). 또한 원복 delta가 비멱등이라 집계값 자체가 오염될 수 있다.

따라서 다음과 같이 Order 상태로 판정한다.

```text
Order.status = FAILED
=> 예약 전 실패 또는 예약 후 보상 성공 (dangling 재고 없음)
=> compensationStatus = COMPLETED 또는 NOT_REQUIRED

Order.status = COMPENSATION_FAILED
=> 예약 성공 후 보상 실패 (dangling 재고 가능)
=> compensationStatus = FAILED, diagnosisStatus = FAILED_COMPENSATION_FAILED / MANUAL_INTERVENTION_REQUIRED

Order.status = CONFIRMED + Shipment 없음
=> MANUAL_INTERVENTION_REQUIRED
```

`Order.status`는 이미 `GET /internal/v1/orders/{orderId}`(`OrderAiContextResponse.orderStatus`)로 노출된다. 위 §7의 `reservedQuantity` 기반 규칙은 **보조 근거(evidence)로만** 사용하고 1차 판정에서는 제외한다.

주의: `FAILED`는 "예약 전 실패"와 "예약 후 보상 성공"을 구분하지 않는다. 안전성(재고 dangling 여부) 관점에선 둘 다 "없음"이라 무방하다. 굳이 구분이 필요하면 order-service가 "STOCK_RESERVED를 거쳤는지"를 노출하도록 보강한다(order 도메인 내부 변경으로 충분, 타 서비스 불필요).

## 8. Internal API 인증/헤더 정책 검토 결과

현행 서비스 코드를 기준으로 확인한 내용입니다.

### 8.1 공통 보안 필터

`GatewayHeaderAuthFilter`는 요청 헤더의 `X-User-Id`, `X-User-Role`이 모두 있으면 Spring Security 인증 객체를 생성합니다.

```text
X-User-Id -> authentication.name
X-User-Role -> ROLE_{role}
```

`FeignHeaderPropagationInterceptor`는 현재 HTTP 요청에 있는 다음 헤더를 Feign 요청에 전파합니다.

```text
X-User-Id
X-Username
X-User-Role
X-User-Status
```

단, Python 서비스는 Spring Feign interceptor 대상이 아니므로 내부 API 호출 시 필요한 헤더를 직접 넣어야 합니다.

### 8.2 서비스별 internal API 접근 정책

#### order-service

`SecurityConfig`에서 `/internal/**`을 `permitAll`로 열어둡니다. `OrderInternalController`도 별도 `@PreAuthorize`나 `@RequestHeader`를 요구하지 않습니다.

초기 Agent 호출 시 헤더 없이도 동작할 가능성이 높지만, 추적성과 일관성을 위해 system header를 포함하는 것을 권장합니다.

#### company-service

`SecurityConfig`에서 `/internal/**`을 `permitAll`로 열어둡니다. Product, Inventory, Company internal controller도 현재 별도 `@PreAuthorize`나 필수 `X-User-*` 헤더를 요구하지 않습니다.

초기 Agent 호출 시 헤더 없이도 동작할 가능성이 높습니다. 다만 감사 로그와 호출자 식별을 위해 system header를 포함하는 것을 권장합니다.

#### user-service

`SecurityConfig`에서 `/internal/**`을 `permitAll`로 열어둡니다. `InternalUserController`는 별도 `@PreAuthorize`나 필수 `X-User-*` 헤더를 요구하지 않습니다.

초기 Agent 호출 시 헤더 없이도 동작할 가능성이 높습니다.

#### shipment-service

별도 `SecurityConfig`가 확인되지 않았고, common security auto configuration이 적용될 수 있습니다. 이 경우 `/internal/**`도 기본적으로 인증이 필요합니다.

또한 `ShipmentInternalController`는 일부 internal endpoint에 `@PreAuthorize`와 `@RequestHeader("X-User-Id")`를 사용합니다.

예:

- `POST /internal/v1/shipments`: `hasAnyRole('MASTER')`, `X-User-Id` 필요
- `POST /internal/v1/shipments/cancel`: `MASTER`, `HUB_MANAGER`, `DELIVERY_MANAGER`, `X-User-Id` 필요
- `GET /internal/v1/shipments/{orderId}`: `X-User-Id` 필요
- `GET /internal/v1/shipments?orderId=...`: `X-User-Id` 필요

따라서 shipment internal API 호출 시 system header를 반드시 포함해야 합니다.

#### hub-service

`HubInternalController`는 별도 `@PreAuthorize`나 필수 `X-User-*` 헤더를 요구하지 않습니다. 다만 별도 security config가 확인되지 않았으므로 common security가 적용될 가능성이 있습니다.

Agent 호출 시 system header를 포함하는 것이 안전합니다.

#### ai-slack-service

`AiSlackInternalController`는 `POST /internal/v1/ai-slack/analysis-requests`에서 `@RequestHeader("X-User-Id")`를 요구합니다. 별도 security config가 확인되지 않았으므로 common security가 적용될 가능성도 있습니다.

Agent가 ai-slack-service를 호출하는 경우 system header를 포함해야 합니다.

### 8.3 Agent system header 권장안

서비스별 차이를 흡수하기 위해 `logistics-agent-service`는 모든 internal API 호출에 system header를 포함합니다.

```text
X-User-Id: 00000000-0000-0000-0000-000000000001
X-Username: logistics-agent-service
X-User-Role: MASTER
X-User-Status: ACTIVE
```

`SYSTEM` role은 현재 공통 role로 확정되어 있지 않으므로 MVP에서는 `MASTER`를 사용합니다. 다만 문서상 이 값은 실제 사용자를 의미하지 않고 service account 성격의 호출자입니다.

추후 개선 시 별도 `SYSTEM` role을 추가하고, internal API에서 service account를 명시적으로 허용하는 방식으로 전환할 수 있습니다.

## 9. Tool/Internal API 후보

MVP는 기존 internal API만 우선 사용합니다. 없는 데이터는 `UNKNOWN` evidence로 남기고, 별도 서비스 수정은 후속 이슈로 분리합니다.

### 9.1 Order tools

```text
get_order_ai_context(orderId)
GET /internal/v1/orders/{orderId}
```

용도:

- 주문 상태 확인
- 주문 상품 목록 확인
- 요청 납기일 확인
- 수령지 정보 확인

### 9.2 Shipment tools

```text
get_shipment_by_order_id(orderId)
GET /internal/v1/shipments/{orderId}
```

```text
get_shipment_status_by_order_id(orderId)
GET /internal/v1/shipments?orderId={orderId}
```

용도:

- 배송 생성 여부 확인
- 배송 상태 확인
- shipment row 누락 여부 판단

주의:

- `X-User-Id`, `X-User-Role` 헤더 필요
- 현재 shipment history 전용 조회 API는 확인되지 않았으므로 MVP에서는 없으면 `UNKNOWN` 처리

### 9.3 Product/Company/Inventory tools

```text
validate_product_variants(productVariantIds)
GET /internal/v1/products/valid?productVariantIds=...
```

```text
get_product_hubs(productVariantIds)
GET /internal/v1/products/search-hub?productVariantId=...
```

```text
get_recipient_hub(zipCode, address, addressDetail)
POST /internal/v1/companies/search-hub
```

```text
reserve_inventory(...)
POST /internal/v1/inventories/reserve
```

```text
cancel_inventory_reservation(...)
POST /internal/v1/inventories/cancel
```

```text
confirm_inventory(...)
POST /internal/v1/inventories/confirm
```

MVP의 Agent는 read-only 진단이 원칙이므로 reserve/cancel/confirm은 기본 tool로 실행하지 않습니다. 재현용 또는 후속 승인 액션 후보로만 문서화합니다.

현재 orderId 기반 inventory reservation 조회 API는 확인되지 않았습니다. company의 `Inventory`는 orderId 없는 집계 `reservedQuantity`만 보유하므로, 이 값으로 주문 단위 보상 상태를 판정하지 않습니다(§7.1 참조). **보상 상태 1차 판정은 order-service의 `Order.status`(`FAILED` / `COMPENSATION_FAILED`)로 하고, `reservedQuantity`는 보조 근거로만** 사용합니다. 이 방향은 예약 원장을 Order에 둔 기존 결정과 일관되며, company-service 설계 변경을 요구하지 않습니다.

주문 단위 정합성 이상 탐지(`DATA_INCONSISTENCY_DETECTED`, 예: "Order는 보상됐다는데 company엔 예약이 남음")는 company에 orderId 스코프 데이터가 없어 MVP 범위에서 제외합니다. 이는 자동 복구용 reconciliation이며 진단 코어가 아닙니다.

### 9.4 Hub tools

```text
check_hub_exists(hubId)
GET /internal/v1/hubs/{hubId}/exists
```

```text
get_shortest_path(originHubId, destinationHubId)
GET /internal/v1/hub-routes/shortest-path?originHubId=...&destinationHubId=...
```

용도:

- 출발/도착 허브 유효성 확인
- same-hub 여부 확인
- route 조회 실패 여부 확인

### 9.5 User tools

```text
get_user(userId)
GET /internal/v1/users/{userId}
```

```text
get_user_by_slack_id(slackId)
GET /internal/v1/users/by-slack/{slackId}
```

용도:

- 담당자 식별
- Slack 알림 대상 확인
- 사용자/업체 연결 확인

## 10. Action Policy

MVP에서는 실제 복구 API 호출을 하지 않습니다.

### 10.1 자동 허용

```text
READ_ONLY
- internal API 조회
- context 수집
- evidence 저장
- diagnosis 저장
- LLM report 생성
- action proposal 저장
```

### 10.2 MVP에서 보류

```text
SAFE_WRITE
- Slack 알림 실제 발송
- incident ticket 생성
```

```text
RECOVERY_WRITE
- inventory cancel 재시도
- shipment create 재시도
- ai analysis 재시도
```

### 10.3 금지

```text
DANGEROUS_MANUAL
- 주문 상태 직접 변경
- 재고 수량 직접 보정
- shipment row 수동 생성
- DB 직접 수정
```

위험 액션은 향후 human-in-the-loop 승인 플로우를 붙인 뒤에만 허용합니다.

## 11. API 응답 스키마

초기 응답은 JSON으로 고정하고, Slack 메시지나 Markdown report는 별도 필드로 둡니다.

```json
{
  "diagnosisId": "00000000-0000-0000-0000-000000000000",
  "triggerType": "USER_QUERY",
  "diagnosisStatus": "FAILED_COMPENSATED",
  "failedStep": "SHIPMENT_CREATION",
  "compensationStatus": "COMPLETED",
  "confidence": 0.86,
  "summary": "주문 생성 중 배송 생성 단계에서 실패했지만 재고 예약 보상은 완료된 것으로 판단됩니다.",
  "evidence": [
    {
      "sourceService": "order-service",
      "toolName": "get_order_ai_context",
      "result": "Order status is FAILED"
    }
  ],
  "recommendedActions": [
    {
      "actionType": "CHECK_SHIPMENT_LOG",
      "riskLevel": "READ_ONLY",
      "description": "shipment-service 로그에서 orderId 기준 에러를 확인합니다.",
      "requiresApproval": false
    }
  ],
  "report": "운영자용 Markdown report"
}
```

## 12. Persistence Schema 초안

`agent_db` schema에 아래 테이블을 둡니다.

### 12.1 agent_diagnosis

```text
diagnosis_id uuid pk
trigger_type varchar
incident_type varchar null
source_service varchar null
order_id uuid null
order_number varchar null
user_question text null
diagnosis_status varchar
failed_step varchar
compensation_status varchar
confidence numeric
summary text
report text
created_at timestamp
updated_at timestamp
```

### 12.2 agent_evidence

```text
evidence_id uuid pk
diagnosis_id uuid fk
source_service varchar
tool_name varchar
request_payload jsonb
response_status varchar
response_payload jsonb
interpreted_meaning text
created_at timestamp
```

### 12.3 agent_tool_call

```text
tool_call_id uuid pk
diagnosis_id uuid fk
tool_name varchar
input jsonb
output jsonb null
success boolean
error_code varchar null
error_message text null
latency_ms integer
created_at timestamp
```

### 12.4 agent_action_proposal

```text
action_id uuid pk
diagnosis_id uuid fk
action_type varchar
risk_level varchar
description text
requires_approval boolean
status varchar
created_at timestamp
```

### 12.5 agent_llm_trace

```text
trace_id uuid pk
diagnosis_id uuid fk
model varchar
prompt_version varchar
input_messages jsonb
output_message text
token_usage jsonb null
latency_ms integer
created_at timestamp
```

### 12.6 agent_eval_dataset

```text
sample_id uuid pk
diagnosis_id uuid fk null
task varchar
input_context jsonb
expected_diagnosis_status varchar
expected_failure_step varchar
expected_compensation_status varchar
expected_report text
source_type varchar
label_status varchar
created_at timestamp
```

## 13. Dataset Schema

SFT/eval dataset은 JSONL export를 고려해 저장합니다.

```json
{
  "id": "diag-000001",
  "task": "order_failure_diagnosis_report",
  "input": {
    "userQuestion": "이 주문 왜 실패했어?",
    "orderContext": {},
    "shipmentContext": {},
    "inventoryContext": {},
    "hubContext": {},
    "toolEvidence": []
  },
  "label": {
    "diagnosisStatus": "FAILED_COMPENSATED",
    "failedStep": "SHIPMENT_CREATION",
    "compensationStatus": "COMPLETED",
    "responsibleService": "shipment-service",
    "severity": "MEDIUM",
    "report": "..."
  },
  "metadata": {
    "source": "synthetic",
    "modelUsedForDraft": "gemini",
    "reviewedByHuman": true,
    "createdAt": "2026-07-17T00:00:00"
  }
}
```

SFT용 chat format export:

```json
{
  "messages": [
    {
      "role": "system",
      "content": "You are a logistics operations diagnosis assistant."
    },
    {
      "role": "user",
      "content": "{context json}"
    },
    {
      "role": "assistant",
      "content": "{expected diagnosis report json}"
    }
  ]
}
```

## 14. Eval 기준

초기 eval은 아래 항목을 봅니다.

```text
diagnosisStatus 정확도
failedStep 정확도
compensationStatus 정확도
필수 evidence 포함 여부
근거 없는 단정 여부
action risk level 정확도
JSON schema adherence
report readability
```

## 15. MVP 구현 순서

```text
1. logistics-agent-service FastAPI 프로젝트 생성
2. agent_db schema 및 persistence 모델 생성
3. internal API client 구현
4. system header 정책 구현
5. user query endpoint 구현
6. incident endpoint 구현
7. context collector 구현
8. rule-based diagnosis engine 구현
9. Gemini report generator 구현
10. diagnosis/evidence/tool_call/llm_trace 저장
11. 기본 eval dataset export 구현
12. Docker Compose 통합
```

## 16. 후속 확장

```text
Scheduled scan
Slack 실제 알림
Human-in-the-loop 승인 기반 recovery action
Loki/Zipkin trace 조회 tool
LangSmith 또는 자체 trace dashboard
sLLM SFT/Distillation
간단한 운영 대시보드
```

## 17. Python 서비스 아키텍처 컨벤션

`logistics-agent-service`는 Python/FastAPI/LangGraph 기반 서비스이지만, 전체 repository의 근간은 Spring Boot MSA입니다. 따라서 Python 생태계의 일반적인 `api/routes`, `services`, `schemas` 구조를 그대로 따르기보다, 기존 Java 서비스의 ArchUnit 계층 철학을 최대한 유지합니다.

단, Java ArchUnit 규칙을 Python에 1:1로 강제하지는 않습니다. Python 서비스에는 별도 architecture rule과 CI를 후속으로 둡니다.

### 17.1 설계 원칙

```text
repo 일관성
- 기존 Spring 서비스의 계층명과 책임을 최대한 유지

Python 실용성
- FastAPI, LangGraph, Pydantic, SQLAlchemy 사용 방식을 과도하게 감추지 않음

Agent 특수성
- agent 계층은 infrastructure가 아니라 workflow orchestration layer로 분리
```

### 17.2 Package Layout

```text
src/logistics_agent_service/
├─ presentation/
│  ├─ controller/
│  └─ dto/
├─ application/
│  ├─ service/
│  ├─ port/
│  └─ dto/
├─ domain/
│  ├─ model/
│  ├─ enum/
│  ├─ rule/
│  └─ exception/
├─ infrastructure/
│  ├─ client/
│  ├─ config/
│  ├─ llm/
│  └─ persistence/
├─ agent/
│  ├─ graph/
│  ├─ node/
│  └─ state/
└─ core/
```

### 17.3 기존 ArchUnit 규칙과의 매핑

```text
Spring:
presentation.controller

Python:
presentation/controller
```

```text
Spring:
presentation.dto

Python:
presentation/dto
```

```text
Spring:
application.service

Python:
application/service
```

```text
Spring:
domain.entity, domain.repository, domain.exception

Python:
domain/model, domain/repository 또는 application/port, domain/exception
```

```text
Spring:
infrastructure.client, infrastructure.config

Python:
infrastructure/client, infrastructure/config
```

`agent`는 기존 Java ArchUnit에는 없는 Python Agent 전용 계층입니다. LangGraph graph, state, node를 이 계층에 둡니다.

### 17.4 Dependency Rules

권장 의존 방향:

```text
presentation -> application
application -> domain, application.port
agent -> application, domain
infrastructure -> application.port, domain
core -> all layers
domain -> no outward dependency
```

금지 규칙:

```text
presentation은 infrastructure를 직접 호출하지 않음
presentation은 agent를 직접 호출하지 않음
application은 presentation에 의존하지 않음
domain은 application, presentation, infrastructure, agent에 의존하지 않음
agent node는 HTTP client, DB, Gemini adapter를 직접 호출하지 않음
infrastructure는 presentation, agent에 의존하지 않음
```

### 17.5 Layer Responsibilities

#### presentation

FastAPI controller와 request/response DTO를 둡니다. 요청 검증과 응답 변환만 담당하고, 진단 로직은 application service에 위임합니다.

#### application

use case와 port를 둡니다. 사용자 질의 진단, incident 처리 같은 application flow의 진입점입니다.

#### domain

진단 enum, 도메인 모델, 실패 판정 규칙, action policy를 둡니다. FastAPI, LangGraph, Gemini, DB, HTTP client에 의존하지 않습니다.

#### agent

LangGraph workflow를 둡니다. `normalize_input`, `collect_context`, `diagnose`, `generate_report`, `propose_actions`, `persist_result` 같은 node를 관리합니다.

agent는 orchestration 계층이며 infrastructure adapter를 직접 호출하지 않습니다. 필요한 외부 작업은 application service 또는 application port를 통해 수행합니다.

#### infrastructure

외부 시스템 연동을 담당합니다.

```text
HTTP internal API client
Gemini API adapter
PostgreSQL persistence adapter
configuration adapter
```

#### core

설정, 로깅, 공통 상수처럼 모든 계층에서 참조 가능한 기반 코드를 둡니다. 비즈니스 로직은 두지 않습니다.

### 17.6 Architecture CI 계획

현재 Java CI는 Gradle/ArchUnit 중심이고 Python 프로젝트는 대상이 아닙니다. Python 서비스가 본격 구현되면 별도 CI를 추가합니다.

초기 Python CI 후보:

```text
uv sync --locked
uv run ruff check .
uv run pytest
```

후속 architecture rule 검증 후보:

```text
import-linter
```

검증할 규칙:

```text
domain must not import application, presentation, infrastructure, agent
application must not import presentation
presentation must not import infrastructure or agent
agent must not import infrastructure
infrastructure must not import presentation or agent
```

## 18. 현재 제약과 후속 이슈 후보

MVP는 기존 internal API만 사용합니다. 아래는 진단 정확도를 높이기 위한 후속 후보입니다.

```text
shipment-service
- shipment history 조회 internal API
- orderId 기반 상세 shipment 조회 확장

company-service
- orderId 기반 inventory reservation 조회 API
- 재고 예약/보상 이력 조회 API

all services
- incident event 발행 API 또는 event bus 연동
- SYSTEM role/service account 명시 지원

observability
- orderId/traceId 기반 Loki 로그 조회 API
- Zipkin trace 조회 tool
```
