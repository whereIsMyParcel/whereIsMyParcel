# Order Create Saga

이 문서는 Order Service의 주문 생성 Saga 구현을 설명합니다. 현재 Saga는 `order create` 유스케이스에 적용되어 있으며, Order Service가 Orchestrator로서 Company Service와 Shipment Service 호출 흐름을 제어합니다.

최근 구조에서는 Long Transaction 문제를 줄이기 위해 `createOrder()` 전체 트랜잭션을 제거하고, 외부 Feign 호출과 DB 상태 저장을 분리했습니다. `OrderCreateSaga`는 더 이상 `Order` 엔티티를 직접 변경하지 않고, `OrderCreationStateService`를 통해 상태 저장을 요청합니다.

---

## 목차

1. Saga 적용 배경
2. 실패 시나리오
3. Orchestration 선택 이유
4. 직접 구현한 이유
5. 현재 구현 구조
6. 상세 흐름
7. 보상 트랜잭션
8. 예외 처리
9. 트랜잭션 경계
10. 테스트 전략
11. 한계와 향후 확장

---

## 1. Saga 적용 배경

주문 생성은 단일 서비스의 DB insert로 끝나지 않습니다.

```text
주문 요청
  -> 상품/SKU 검증
  -> 주문 저장
  -> 재고 예약
  -> 배송 생성
  -> 주문 확정
```

| 단계 | 담당 서비스 | 상태 변경 여부 |
|---|---|---|
| 상품/SKU 검증 | Company Service | 조회 |
| 주문 저장 | Order Service | 쓰기 |
| 재고 예약 | Company Service | 쓰기 |
| 배송 생성 | Shipment Service | 쓰기 |
| 주문 확정 | Order Service | 쓰기 |

각 서비스는 독립된 DB와 트랜잭션 경계를 가집니다. 따라서 Order Service의 로컬 트랜잭션만으로 전체 작업을 원자적으로 보장할 수 없습니다.

Saga는 이런 분산 트랜잭션 상황에서 각 단계의 성공/실패를 명시적으로 제어하고, 실패 시 이미 성공한 외부 작업을 보상하기 위해 사용합니다.

---

## 2. 실패 시나리오

### 2.1 상품 검증 실패

```text
상품 검증 실패
-> 주문 저장 안 함
-> 보상 없음
```

상품 검증은 주문 저장 이전에 수행되는 조회성 작업입니다. 아직 외부 상태 변경이 없으므로 보상 트랜잭션이 필요하지 않습니다.

### 2.2 재고 예약 실패

```text
PENDING 저장
-> 재고 예약 실패
-> FAILED 저장
```

재고 예약이 실패했으므로 Company Service에 성공한 외부 작업이 없습니다. 따라서 보상 없이 `FAILED`로 종료합니다.

### 2.3 배송 생성 실패 + 보상 성공

```text
PENDING 저장
-> 재고 예약 성공
-> STOCK_RESERVED 저장
-> 배송 생성 실패
-> 재고 예약 취소 성공
-> FAILED 저장
```

배송이 생성되지 않았고, 이미 성공한 재고 예약도 취소해 정합성을 회복했으므로 최종 상태는 `FAILED`입니다.

### 2.4 배송 생성 실패 + 보상 실패

```text
PENDING 저장
-> 재고 예약 성공
-> STOCK_RESERVED 저장
-> 배송 생성 실패
-> 재고 예약 취소 실패
-> COMPENSATION_FAILED 저장
```

이 경우 Company Service에 예약 재고가 남아 있을 수 있습니다. 따라서 일반 `FAILED`가 아니라 `COMPENSATION_FAILED`로 분리해 운영자가 추적할 수 있게 합니다.

### 2.5 배송 생성 성공 후 주문 확정 저장 실패 + 보상 성공

```text
PENDING 저장
-> 재고 예약 성공
-> STOCK_RESERVED 저장
-> 배송 생성 성공
-> markConfirmed(orderId) 실패
-> 배송 취소 보상 성공
-> 재고 예약 취소 보상 성공
-> FAILED 저장
```

이 경우 Shipment Service에는 이미 배송이 생성되었고, Company Service에는 재고 예약도 반영된 상태입니다. 따라서 배송 취소 보상과 재고 예약 취소 보상을 모두 수행한 뒤 `FAILED`로 정리합니다.

### 2.6 배송 생성 성공 후 주문 확정 저장 실패 + 보상 실패

```text
PENDING 저장
-> 재고 예약 성공
-> STOCK_RESERVED 저장
-> 배송 생성 성공
-> markConfirmed(orderId) 실패
-> 배송 취소 또는 재고 예약 취소 보상 실패
-> COMPENSATION_FAILED 저장
```

이 경우 외부 서비스에 이미 반영된 작업이 남아 있을 수 있습니다. 따라서 `COMPENSATION_FAILED`로 분리해 운영자 확인 또는 재보상 대상으로 관리합니다.

---

## 3. Orchestration 선택 이유

### 3.1 Choreography를 선택하지 않은 이유

Choreography는 각 서비스가 이벤트를 구독하고 다음 이벤트를 발행하는 방식입니다.

```text
OrderCreatedEvent
  -> StockReservedEvent
  -> ShipmentCreatedEvent
  -> OrderConfirmedEvent
```

이 방식은 서비스 간 결합도를 낮출 수 있지만, 현재 주문 생성 유스케이스에서는 다음 부담이 있었습니다.

- 실패 흐름이 여러 서비스로 분산됨
- 보상 순서를 한눈에 추적하기 어려움
- 메시지 브로커, 이벤트 스키마, 중복 처리, 재시도, DLQ, Outbox 같은 설계가 필요함
- 프로젝트 범위 대비 인프라 도입 비용이 큼

### 3.2 Orchestration을 선택한 이유

현재 주문 생성의 시작점과 최종 상태 결정 책임은 Order Service에 있습니다. Order Service가 `CONFIRMED`, `FAILED`, `COMPENSATION_FAILED`를 최종적으로 판단해야 합니다.

- 주문 생성의 최종 상태는 Order Service가 결정
- 참여 서비스가 Company Service와 Shipment Service로 제한적
- 실패 지점과 보상 흐름이 명확
- 장애 추적과 디버깅이 쉬움
- 현재 범위에서는 동기 Feign 기반 명시적 흐름이 더 단순함

---

## 4. 직접 구현한 이유

현재 구현은 범용 Saga framework나 workflow engine을 사용하지 않습니다.

- Saga 적용 대상이 주문 생성 하나로 제한적
- 단계가 재고 예약, 배송 생성, 보상 처리로 비교적 단순함
- 범용 라이브러리 도입 시 학습 비용과 설정 비용이 큼
- 팀이 Saga 흐름과 보상 처리를 직접 이해하는 것이 프로젝트 목적에 더 적합함
- 향후 이벤트 기반 전환을 고려하더라도 현재는 작은 Orchestrator로 충분함

현재 구조는 직접 구현이지만, 다음 요소를 분리해 향후 확장 가능성을 남깁니다.

- `OrderCreateSaga`: 외부 호출 흐름 제어
- `OrderCreateSagaContext`: Saga 실행 데이터와 중간 결과 보관
- `OrderCreationStateService`: 상태 저장 트랜잭션 분리

---

## 5. 현재 구현 구조

### 5.1 OrderService

`OrderService.createOrder()`는 전체 흐름을 조율합니다.

- 상품/SKU 검증 요청
- `OrderCreateSagaContext` 생성
- `PENDING` 주문 저장 요청
- Saga 실행
- 최종 주문 재조회
- `CONFIRMED` 상태인 경우 AI 분석 요청
- 응답 반환

중요한 점은 `createOrder()`가 전체 트랜잭션을 가지지 않는다는 것입니다. 읽기 메서드에는 개별적으로 `@Transactional(readOnly = true)`를 선언하고, 주문 생성 흐름은 비트랜잭션으로 실행되도록 분리합니다.

### 5.2 OrderCreationStateService

`OrderCreationStateService`는 주문 생성 Saga 중 DB write를 담당합니다.

```java
@Transactional
UUID createPendingOrder(OrderCreateSagaContext context)

@Transactional
void markStockReserved(UUID orderId)

@Transactional
void markConfirmed(UUID orderId)

@Transactional
void markFailed(UUID orderId)

@Transactional
void markCompensationFailed(UUID orderId)

@Transactional(readOnly = true)
Order getOrder(UUID orderId)
```

각 메서드는 독립적인 짧은 트랜잭션으로 실행됩니다. 외부 Feign 호출은 이 트랜잭션 안에 포함되지 않습니다.

### 5.3 OrderCreateSaga

`OrderCreateSaga`는 외부 호출과 보상 흐름을 담당합니다.

```java
public void execute(OrderCreateSagaContext context)
```

이전처럼 `Order` 엔티티를 인자로 받지 않습니다. 상태 변경은 모두 `OrderCreationStateService` 호출로 위임합니다.

### 5.4 OrderCreateSagaContext

| 필드 | 용도 |
|---|---|
| `orderId` | Saga 대상 주문 ID |
| `userId` | 내부 API 호출 사용자 식별자 |
| `companyMemberId` | 주문 저장 정보 |
| `orderNumber` | 주문 번호 |
| `recipientName` | 배송 생성 요청 |
| `recipientPhone` | 배송 생성 요청 |
| `zipCode` | 배송 생성 요청 |
| `address` | 배송 생성 요청 |
| `addressDetail` | 배송 생성 요청 |
| `requestMemo` | 주문 저장 정보 |
| `requestedDeliveryAt` | 주문 저장 정보 |
| `items` | 주문 상품/Saga item 스냅샷 |
| `reservations` | 재고 예약 성공 결과 |
| `shipmentIds` | 배송 생성 성공 결과 |

---

## 6. 상세 흐름

### 6.1 정상 흐름

```text
1. 상품 검증
2. SagaContext 생성
3. PENDING 주문 저장
4. context.applyOrderId(orderId)
5. 재고 예약 요청
6. 재고 예약 성공 결과 context에 저장
7. markStockReserved(orderId)
8. 배송 생성 요청
9. 배송 생성 성공 결과 context에 저장
10. markConfirmed(orderId)
11. 주문 재조회
12. CONFIRMED이면 AI 분석 요청
13. OrderCreateResponse 반환
```

### 6.2 재고 예약 실패

```text
1. PENDING 주문 저장
2. 재고 예약 요청
3. 재고 예약 실패
4. markFailed(orderId)
5. SagaFailedException
6. OrderService에서 예외 로그 기록
7. 주문 재조회 후 FAILED 응답 반환
```

### 6.3 배송 생성 실패 + 보상 성공

```text
1. PENDING 주문 저장
2. 재고 예약 성공
3. markStockReserved(orderId)
4. 배송 생성 실패
5. 재고 예약 취소 보상 요청
6. 보상 성공
7. markFailed(orderId)
8. SagaFailedException
9. 주문 재조회 후 FAILED 응답 반환
```

### 6.4 배송 생성 실패 + 보상 실패

```text
1. PENDING 주문 저장
2. 재고 예약 성공
3. markStockReserved(orderId)
4. 배송 생성 실패
5. 재고 예약 취소 보상 요청
6. 보상 실패
7. markCompensationFailed(orderId)
8. SagaCompensationFailedException
9. 주문 재조회 후 COMPENSATION_FAILED 응답 반환
```

### 6.5 배송 생성 성공 후 주문 확정 저장 실패 + 보상 성공

```text
1. PENDING 주문 저장
2. 재고 예약 성공
3. markStockReserved(orderId)
4. 배송 생성 성공
5. 배송 생성 결과 context에 저장
6. markConfirmed(orderId) 실패
7. 배송 취소 보상 요청
8. 재고 예약 취소 보상 요청
9. 보상 성공
10. markFailed(orderId)
11. SagaFailedException
12. 주문 재조회 후 FAILED 응답 반환
```

### 6.6 배송 생성 성공 후 주문 확정 저장 실패 + 보상 실패

```text
1. PENDING 주문 저장
2. 재고 예약 성공
3. markStockReserved(orderId)
4. 배송 생성 성공
5. 배송 생성 결과 context에 저장
6. markConfirmed(orderId) 실패
7. 배송 취소 또는 재고 예약 취소 보상 실패
8. markCompensationFailed(orderId)
9. SagaCompensationFailedException
10. 주문 재조회 후 COMPENSATION_FAILED 응답 반환
```

---

## 7. 보상 트랜잭션

현재 주문 생성 Saga에서 구현된 보상은 다음 두 가지입니다.

| 보상 대상 | 호출 조건 | 내부 API |
|---|---|---|
| 재고 예약 취소 | 재고 예약 성공 이후 Saga 실패 | `POST /internal/v1/inventories/cancel` |
| 배송 취소 | 배송 생성 성공 이후 주문 확정 저장 실패 | `POST /internal/v1/shipments/cancel` |

배송 생성 자체가 실패한 경우에는 Shipment Service에 성공한 배송 작업이 없다고 보고, 이미 성공한 Company Service 재고 예약만 취소합니다.

반면 배송 생성은 성공했지만 `markConfirmed(orderId)`가 실패한 경우에는 Shipment Service에 배송이 이미 생성되었으므로 배송 취소 보상 후 재고 예약 취소 보상을 수행합니다.

보상 응답이 실패하거나 예외가 발생하면 `SagaCompensationFailedException`을 던지고, 주문 상태를 `COMPENSATION_FAILED`로 저장합니다.

---

## 8. 예외 처리

### 8.1 SagaFailedException

일반 Saga 실패를 의미합니다.

발생 예:

- 재고 예약 실패
- 배송 생성 실패 후 재고 예약 취소 보상 성공
- 배송 생성 성공 후 주문 확정 저장 실패, 이후 배송 취소와 재고 예약 취소 보상 성공

최종 주문 상태는 `FAILED`입니다.

### 8.2 SagaCompensationFailedException

외부 작업 보상 실패를 의미합니다.

발생 예:

- 재고 예약 성공
- 배송 생성 실패
- 재고 예약 취소 실패

또는:

- 재고 예약 성공
- 배송 생성 성공
- 주문 확정 저장 실패
- 배송 취소 또는 재고 예약 취소 보상 실패

최종 주문 상태는 `COMPENSATION_FAILED`입니다.

### 8.3 OrderService의 예외 처리

`OrderService.createOrder()`는 Saga 예외를 catch하고 로그를 남깁니다. 이후 `orderId`로 주문을 재조회해 최종 상태 기준으로 응답합니다.

```text
catch SagaCompensationFailedException
  -> 로그 기록
  -> getOrder(orderId)
  -> COMPENSATION_FAILED 응답

catch SagaFailedException
  -> 로그 기록
  -> getOrder(orderId)
  -> FAILED 응답
```

이 구조는 주문 생성 요청 자체가 실패 예외로 끝나는 대신, 생성 시도와 최종 상태를 클라이언트가 확인할 수 있게 합니다.

---

## 9. 트랜잭션 경계

### 9.1 이전 구조의 문제

이전 구조에서는 `createOrder()` 전체가 하나의 트랜잭션이었고, 그 안에서 Feign 호출이 수행될 수 있었습니다.

```text
@Transactional createOrder()
  -> SKU 검증 Feign
  -> orderRepository.save(order)
  -> 재고 예약 Feign
  -> 배송 생성 Feign
  -> finally orderRepository.save(order)
```

이 경우 외부 서비스 응답이 지연되면 Order DB 트랜잭션과 커넥션이 오래 유지됩니다.

### 9.2 현재 구조

```text
SKU 검증 Feign
  -> 트랜잭션 없음

PENDING 주문 저장
  -> 짧은 트랜잭션

재고 예약 Feign
  -> 트랜잭션 없음

STOCK_RESERVED 저장
  -> 짧은 트랜잭션

배송 생성 Feign
  -> 트랜잭션 없음

CONFIRMED 저장
  -> 짧은 트랜잭션

재고 예약 취소 보상 Feign
  -> 트랜잭션 없음

배송 취소 보상 Feign
  -> 트랜잭션 없음

FAILED 또는 COMPENSATION_FAILED 저장
  -> 짧은 트랜잭션
```

### 9.3 기대 효과

- Feign 호출 동안 Order DB 커넥션을 장시간 점유하지 않음
- 상태 저장 시점이 명확해짐
- 보상 실패를 별도 상태로 추적 가능
- 향후 Outbox/Event 기반 전환 시 상태 전이 지점이 명확함

---

## 10. 테스트 전략

### 10.1 OrderCreateSagaTest

Saga 테스트는 더 이상 `Order` 엔티티 상태를 직접 검증하지 않습니다. 대신 `OrderCreationStateService` 호출과 보상 API 호출 여부를 검증합니다.

검증 예:

- 성공 시 `markStockReserved(orderId)` 후 `markConfirmed(orderId)` 호출
- 재고 예약 실패 시 `markFailed(orderId)` 호출
- 배송 생성 실패 + 보상 성공 시 `markFailed(orderId)` 호출
- 배송 생성 실패 + 보상 실패 시 `markCompensationFailed(orderId)` 호출
- 배송 생성 성공 후 `markConfirmed(orderId)` 실패 시 배송 취소 보상과 재고 예약 취소 보상 호출
- 배송 생성 성공 후 `markConfirmed(orderId)` 실패 및 보상 실패 시 `markCompensationFailed(orderId)` 호출
- 성공 케이스에서 보상 API 미호출

### 10.2 OrderServiceTest

OrderService 테스트는 흐름 조율을 검증합니다.

- `createPendingOrder(context)` 호출
- `context.applyOrderId(orderId)` 이후 `orderCreateSaga.execute(context)` 호출
- 응답 생성 전 `getOrder(orderId)` 호출
- `CONFIRMED` 상태에서만 AI Trigger 호출
- `FAILED`, `COMPENSATION_FAILED` 상태에서는 AI Trigger 미호출

### 10.3 Order 도메인 테스트

- `STOCK_RESERVED -> COMPENSATION_FAILED` 가능
- `PENDING -> COMPENSATION_FAILED` 불가
- `COMPENSATION_FAILED` 이후 다른 상태 전이 불가

---

## 11. 한계와 향후 확장

### 11.1 보상 실패 재시도

`COMPENSATION_FAILED` 상태는 운영자 확인 또는 재보상 대상입니다. 향후 다음 기능을 추가할 수 있습니다.

- 관리자 보상 실패 주문 조회
- 보상 재시도 API
- 재시도 횟수 기록
- 마지막 실패 사유 기록
- 보상 성공 시 `FAILED`로 정리

### 11.2 보상 이력 저장

현재는 최종 상태만 Order row에 남습니다. 향후 보상 이력을 별도 테이블로 관리하면 장애 분석과 운영 추적에 도움이 됩니다.

### 11.3 이벤트 기반 전환

현재는 동기 Feign 기반 Orchestration입니다. 향후 트래픽 증가와 장애 격리 요구가 커지면 다음 구조로 확장할 수 있습니다.

- Outbox 패턴
- 이벤트 기반 재고 예약 요청
- 이벤트 기반 배송 생성 요청
- 보상 이벤트
- DLQ 기반 재처리
