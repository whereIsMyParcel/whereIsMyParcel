# Order Service README

`Order Service`는 WhereIsMyParcel 프로젝트에서 주문의 생성, 조회, 수정, 취소, 삭제, 배송 완료 반영, AI 출고 상한 업데이트를 담당합니다. 단순 CRUD 서비스가 아니라 주문 생명주기와 분산 트랜잭션 흐름의 중심에 있는 서비스입니다.

주문 생성 과정에서는 Company Service의 상품 검증·재고 예약, Shipment Service의 배송 생성이 함께 발생합니다. 이 작업들은 하나의 로컬 DB 트랜잭션으로 묶을 수 없기 때문에 Order Service는 주문 생성 Saga의 Orchestrator 역할을 수행합니다.

최근 주문 생성 Saga는 Long Transaction 문제를 줄이기 위해 구조가 변경되었습니다. `OrderService.createOrder()` 전체를 하나의 `@Transactional`로 묶지 않고, 외부 Feign 호출은 트랜잭션 밖에서 수행하며, 주문 상태 저장만 `OrderCreationStateService`의 짧은 트랜잭션으로 처리합니다. 또한 보상 실패는 단순 `FAILED`가 아니라 `COMPENSATION_FAILED` 상태로 분리해 추적합니다.

---

## 목차

1. 서비스 책임
2. 핵심 설계 요약
3. 패키지 구조
4. 주문 생성 흐름
5. 주문 상태 흐름
6. 주문 생성 Saga 요약
7. 주문 취소 및 보상
8. AI 연동
9. 내부 API 연동
10. 예외 및 장애 처리
11. 테스트 전략
12. 관련 문서
13. 변경 시 체크리스트

---

## 1. 서비스 책임

Order Service는 다음 책임을 가집니다.

- 주문 생성 요청 처리
- 주문 상품 스냅샷 저장
- 주문 상태 머신 관리
- 주문 생성 Saga Orchestration
- 주문 취소 시 상태별 보상 처리
- 배송 완료 이벤트 또는 내부 API 호출에 따른 주문 완료 처리
- AI Slack Service에 주문 분석 요청 트리거
- AI가 계산한 최종 출고 상한 반영
- 주문 검색, 상세 조회, 수정, 삭제

Order Service가 직접 담당하지 않는 책임은 다음과 같습니다.

- 실제 상품 정보의 원천 관리: Company Service
- 실제 재고 수량 및 예약 처리: Company Service
- 배송 생성과 배송 경로 관리: Shipment Service
- AI 분석 및 Slack 알림: AI Slack Service
- 인증/인가 필터링: API Gateway 및 Security 계층

Order Service는 외부 서비스의 결과를 바탕으로 “주문이 현재 어떤 상태인가”를 일관되게 관리합니다.

---

## 2. 핵심 설계 요약

### 2.1 주문 상태 머신

`Order` Aggregate Root는 주문 상태 전이를 도메인 메서드로 제한합니다. 서비스 계층은 상태 값을 직접 대입하지 않고 `reserveStock()`, `confirm()`, `fail()`, `failCompensation()`, `cancel()`, `complete()` 같은 도메인 메서드를 통해 상태를 변경합니다.

현재 주문 상태는 다음과 같습니다.

```java
public enum OrderStatus {
    PENDING,
    STOCK_RESERVED,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    FAILED,
    COMPENSATION_FAILED
}
```

`COMPENSATION_FAILED`는 주문 생성 Saga에서 보상 처리까지 실패한 상태입니다. 재고 예약은 성공했지만 배송 생성에 실패했고, 재고 예약 취소 보상까지 실패한 경우에 사용합니다. 이 상태는 단순 실패가 아니라 운영자 확인 또는 재보상 대상이 될 수 있는 상태입니다.

### 2.2 주문 생성 Saga Orchestration

주문 생성은 Order Service 단독 작업이 아닙니다.

1. Company Service에서 상품/SKU 검증
2. Order Service에서 `PENDING` 주문 저장
3. Company Service에서 재고 예약
4. Shipment Service에서 배송 생성
5. Order Service에서 `CONFIRMED` 또는 실패 상태 저장
6. 성공 시 AI Slack Service 분석 요청

이 중 재고 예약과 배송 생성은 외부 서비스의 상태를 바꿉니다. 따라서 일부 단계가 실패하면 이미 성공한 외부 작업을 되돌리는 보상이 필요합니다.

Order Service는 Choreography 방식으로 이벤트를 흩뿌리기보다, 주문 생성의 최종 상태를 직접 결정해야 하므로 Orchestration 방식을 선택했습니다. 현재 구현에서는 `OrderCreateSaga`가 외부 호출 흐름을 제어하고, `OrderCreationStateService`가 주문 상태 저장을 담당합니다.

### 2.3 트랜잭션 경계 분리

이전 구조에서는 `createOrder()` 전체가 하나의 트랜잭션으로 묶여 있었고, 그 안에서 SKU 검증, 재고 예약, 배송 생성 Feign 호출이 수행될 수 있었습니다. 이 경우 외부 서비스 응답이 지연되면 Order DB 커넥션을 오래 점유할 수 있습니다.

현재 구조는 다음 원칙을 따릅니다.

- 외부 Feign 호출은 Order DB 트랜잭션 밖에서 수행
- DB write는 `OrderCreationStateService`의 짧은 트랜잭션으로 처리
- Saga는 JPA 엔티티를 직접 변경하지 않고 상태 저장 서비스를 호출
- 응답은 최종적으로 DB에서 재조회한 주문 상태 기준으로 생성

---

## 3. 패키지 구조

```text
services/order-service/src/main/java/com/sparta/whereismyparcel/order
├── application
│   ├── saga
│   │   ├── OrderCreateSaga.java
│   │   └── OrderCreateSagaContext.java
│   └── service
│       ├── OrderService.java
│       └── OrderCreationStateService.java
├── domain
│   ├── OrderStatus.java
│   ├── entity
│   │   ├── Order.java
│   │   └── OrderItem.java
│   ├── exception
│   └── repository
├── infrastructure
│   └── client
└── presentation
    ├── controller
    └── dto
```

주요 클래스 역할은 다음과 같습니다.

| 클래스 | 책임 |
|---|---|
| `OrderService` | 주문 생성/취소/조회 등 유스케이스 조율 |
| `OrderCreationStateService` | 주문 생성 Saga 중 상태 저장을 짧은 트랜잭션으로 처리 |
| `OrderCreateSaga` | 재고 예약, 배송 생성, 보상 호출 흐름 제어 |
| `OrderCreateSagaContext` | Saga 실행에 필요한 주문/배송/상품 스냅샷과 중간 결과 보관 |
| `Order` | 주문 상태 전이와 도메인 규칙 관리 |
| `OrderItem` | 주문 당시 상품 스냅샷 저장 |

---

## 4. 주문 생성 흐름

### 4.1 전체 흐름

```text
Client
  -> OrderService.createOrder()
      -> CompanyFeignClient.validateProducts()
      -> OrderCreateSagaContext 생성
      -> OrderCreationStateService.createPendingOrder(context)
      -> context.applyOrderId(orderId)
      -> OrderCreateSaga.execute(context)
          -> CompanyFeignClient.reserveStock()
          -> OrderCreationStateService.markStockReserved(orderId)
          -> ShipmentFeignClient.createShipments()
          -> OrderCreationStateService.markConfirmed(orderId)
      -> OrderCreationStateService.getOrder(orderId)
      -> CONFIRMED이면 AI 분석 요청
      -> OrderCreateResponse 반환
```

### 4.2 상품 검증

상품 검증은 Company Service에 요청합니다. 이 단계는 주문 DB write 이전이며, 트랜잭션 밖에서 수행합니다. 검증 결과는 주문 상품 스냅샷과 Saga item context를 만드는 데 사용합니다.

### 4.3 PENDING 주문 저장

상품 검증이 성공하면 `OrderCreationStateService.createPendingOrder(context)`를 호출합니다. 이 메서드는 짧은 트랜잭션 안에서 `Order`와 `OrderItem`을 생성하고 저장한 뒤 `orderId`를 반환합니다.

### 4.4 Saga 실행

`OrderCreateSaga.execute(context)`는 `Order` 엔티티를 인자로 받지 않습니다. Saga는 다음 작업만 수행합니다.

- 재고 예약 Feign 호출
- 배송 생성 Feign 호출
- 재고 예약 취소 보상 Feign 호출
- 각 단계 결과에 따라 `OrderCreationStateService` 호출

즉 Saga는 흐름을 제어하지만, JPA 엔티티를 직접 변경하지 않습니다.

### 4.5 응답 생성

Saga 실행 후 Order Service는 `orderId`로 주문을 재조회합니다. AI Trigger 여부와 응답 상태는 메모리 객체가 아니라 DB에 커밋된 최신 주문 상태를 기준으로 판단합니다.

---

## 5. 주문 상태 흐름

주문 생성 Saga 기준 상태 흐름은 다음과 같습니다.

```text
PENDING
  ├─ 재고 예약 실패 -> FAILED
  └─ 재고 예약 성공 -> STOCK_RESERVED
        ├─ 배송 생성 성공 -> CONFIRMED
        └─ 배송 생성 실패
              ├─ 재고 예약 취소 성공 -> FAILED
              └─ 재고 예약 취소 실패 -> COMPENSATION_FAILED
```

상태별 의미는 다음과 같습니다.

| 상태 | 의미 |
|---|---|
| `PENDING` | 주문이 생성되었고 아직 재고 예약이 확정되지 않은 상태 |
| `STOCK_RESERVED` | Company Service 재고 예약이 성공한 상태 |
| `CONFIRMED` | 재고 예약과 배송 생성이 모두 성공한 상태 |
| `FAILED` | 주문 생성이 실패했지만 필요한 보상이 성공해 정합성이 회복된 상태 |
| `COMPENSATION_FAILED` | 보상 실패로 외부 서비스에 성공한 작업이 남아 있을 수 있는 상태 |
| `CANCELLED` | 사용자 또는 관리자 요청으로 취소된 상태 |
| `COMPLETED` | 배송 완료가 반영된 상태 |

`FAILED`와 `COMPENSATION_FAILED`는 반드시 구분해야 합니다. `FAILED`는 정합성이 회복된 실패에 가깝지만, `COMPENSATION_FAILED`는 재고 예약이 남아 있을 수 있으므로 운영상 확인이 필요한 상태입니다.

---

## 6. 주문 생성 Saga 요약

`OrderCreateSaga`는 주문 생성 중 외부 서비스 호출 순서를 명시적으로 제어합니다.

성공 경로:

```text
재고 예약 성공
-> markStockReserved(orderId)
-> 배송 생성 성공
-> markConfirmed(orderId)
```

재고 예약 실패:

```text
재고 예약 실패
-> markFailed(orderId)
-> SagaFailedException
```

배송 생성 실패 + 보상 성공:

```text
재고 예약 성공
-> markStockReserved(orderId)
-> 배송 생성 실패
-> 재고 예약 취소 보상 성공
-> markFailed(orderId)
-> SagaFailedException
```

배송 생성 실패 + 보상 실패:

```text
재고 예약 성공
-> markStockReserved(orderId)
-> 배송 생성 실패
-> 재고 예약 취소 보상 실패
-> markCompensationFailed(orderId)
-> SagaCompensationFailedException
```

현재 Saga는 별도의 Saga step 저장소나 범용 workflow engine을 사용하지 않습니다. 주문 생성 유스케이스의 참여 서비스와 보상 흐름이 제한적이기 때문에 직접 구현한 Orchestration 방식으로 충분하다고 판단했습니다.

---

## 7. 주문 취소 및 보상

주문 취소는 주문 생성 Saga와 별도 유스케이스입니다. 생성은 여러 서비스의 성공을 조합해 `CONFIRMED`를 만드는 분산 확정 흐름이고, 취소는 이미 생성된 주문의 외부 리소스를 정리하고 `CANCELLED`로 종료하는 정리성 유스케이스입니다.

상태별 취소 보상은 다음과 같습니다.

| 주문 상태 | 취소 처리 |
|---|---|
| `PENDING` | 외부 보상 없이 주문 취소 |
| `STOCK_RESERVED` | 재고 예약 취소 후 주문 취소 |
| `CONFIRMED` | 배송 취소 후 재고 예약 취소, 이후 주문 취소 |

현재 `COMPENSATION_FAILED`는 주문 생성 Saga의 보상 실패를 추적하기 위한 상태입니다. 주문 취소 보상 실패는 별도 정책으로 다룰 수 있으며, 추후 취소 전용 실패 상태 또는 재처리 기능으로 확장할 수 있습니다.

---

## 8. AI 연동

주문 생성 Saga가 성공해 주문이 `CONFIRMED` 상태가 되면 AI Slack Service에 분석 요청을 보냅니다.

현재 AI Trigger 조건은 다음과 같습니다.

1. Saga 실행 종료
2. `orderId`로 주문 재조회
3. DB에 저장된 주문 상태가 `CONFIRMED`인지 확인
4. `CONFIRMED`일 때만 AI 분석 요청 수행

중요한 점은 AI 요청이 주문 생성 Saga 내부에서 수행되지 않는다는 것입니다. AI 분석은 주문 확정 이후의 후처리이며, AI 요청 실패는 주문 생성 실패로 전파하지 않습니다.

---

## 9. 내부 API 연동

Order Service가 주문 생성 중 호출하는 내부 API는 다음과 같습니다.

| 대상 서비스 | API | 목적 |
|---|---|---|
| Company Service | `GET /internal/v1/products/valid` | 주문 상품/SKU 검증 |
| Company Service | `POST /internal/v1/inventories/reserve` | 재고 예약 |
| Company Service | `POST /internal/v1/inventories/cancel` | 재고 예약 취소 보상 |
| Shipment Service | `POST /internal/v1/shipments` | 배송 생성 |
| AI Slack Service | `POST /internal/v1/ai-slack/analysis-requests` | 주문 확정 후 AI 분석 요청 |

내부 API 계약은 이번 트랜잭션 경계 분리 작업에서 변경하지 않았습니다. 변경 범위는 Order Service 내부의 상태 저장 구조와 트랜잭션 경계에 한정됩니다.

---

## 10. 예외 및 장애 처리

| 예외 | 의미 |
|---|---|
| `SagaFailedException` | 주문 생성 Saga 중 일반 실패 |
| `SagaCompensationFailedException` | 보상 처리 실패 |
| `InvalidOrderStatusException` | 허용되지 않은 상태 전이 |
| `OrderNotFoundException` | 주문 조회 실패 또는 권한 없음 |

주문 생성 Saga에서 예외가 발생해도 Order Service는 최종 주문 상태를 재조회해 응답합니다. 따라서 클라이언트는 `FAILED` 또는 `COMPENSATION_FAILED` 상태를 응답으로 받을 수 있습니다.

`COMPENSATION_FAILED`는 운영자가 확인해야 할 가능성이 있는 상태입니다. 추후 다음 기능으로 확장할 수 있습니다.

- 보상 실패 주문 목록 조회
- 보상 재시도
- 재시도 횟수 기록
- 마지막 실패 사유 기록
- 관리자 수동 정리

---

## 11. 테스트 전략

중요 테스트 항목은 다음과 같습니다.

- 주문 생성 성공 시 `STOCK_RESERVED`, `CONFIRMED` 순서로 상태 저장
- 재고 예약 실패 시 `FAILED` 저장
- 배송 생성 실패 후 보상 성공 시 `FAILED` 저장
- 배송 생성 실패 후 보상 실패 시 `COMPENSATION_FAILED` 저장
- `COMPENSATION_FAILED` 상태에서 다른 상태로 전이 불가
- `createOrder()` 성공 시 AI Trigger 호출
- `FAILED` 또는 `COMPENSATION_FAILED` 응답 시 AI Trigger 미호출
- 주문 취소 상태별 보상 호출 검증

실행 예시는 다음과 같습니다.

```bash
./gradlew :services:order-service:test
```

---

## 12. 관련 문서

| 문서 | 설명 |
|---|---|
| [`docs/order-domain.md`](docs/order-domain.md) | Order 도메인 모델과 상태 전이 |
| [`docs/order-create-saga.md`](docs/order-create-saga.md) | 주문 생성 Saga Orchestration |
| [`docs/order-cancel-compensation.md`](docs/order-cancel-compensation.md) | 주문 취소와 상태별 보상 처리 |
| [`docs/internal-api-contracts.md`](docs/internal-api-contracts.md) | 내부 API 계약 |
| [`docs/order-ai-integration.md`](docs/order-ai-integration.md) | Order-AI 연계 |

---

## 13. 변경 시 체크리스트

주문 상태를 추가하거나 변경할 때:

- `OrderStatus` enum 수정
- `Order` 상태 전이 메서드 수정
- 상태 전이 테스트 추가
- 취소 가능/삭제 가능/수정 가능 상태 확인
- API 응답에서 신규 상태 노출 영향 확인

주문 생성 Saga를 변경할 때:

- 외부 Feign 호출이 Order DB 트랜잭션 안에 들어가지 않는지 확인
- 상태 저장은 `OrderCreationStateService`를 통해 수행하는지 확인
- 실패와 보상 실패를 구분하는지 확인
- `OrderCreateSagaTest`에서 상태 저장 서비스 호출 검증
- `OrderServiceTest`에서 최종 응답 상태와 AI Trigger 조건 검증

내부 API 계약을 변경할 때:

- Company/Shipment/AI Slack Service와 request/response DTO 호환성 확인
- 기존 FeignClient 시그니처 영향 확인
- 문서와 테스트 동시 수정
