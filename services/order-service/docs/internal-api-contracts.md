# Order Service Internal API Contracts

이 문서는 Order Service가 다른 서비스와 통신할 때 사용하는 내부 API 계약을 정리합니다. 이번 주문 생성 Saga 트랜잭션 경계 분리 작업에서 내부 API의 request/response 계약은 변경하지 않았습니다. 변경 범위는 Order Service 내부의 상태 저장 구조와 트랜잭션 경계입니다.

---

## 1. 설계 원칙

Order Service는 외부 서비스와 동기 Feign 방식으로 통신합니다. 내부 API 호출은 다음 원칙을 따릅니다.

- 외부 서비스 호출은 Order DB 트랜잭션 밖에서 수행합니다.
- 호출 결과에 따른 Order 상태 저장은 `OrderCreationStateService`의 짧은 트랜잭션으로 처리합니다.
- 내부 API 응답은 `ApiResponse<T>` 형태를 사용합니다.
- 실패 응답, null 응답, data 누락은 Saga 실패 또는 보상 실패로 처리합니다.

---

## 2. Company Service

### 2.1 상품/SKU 검증

| 항목 | 내용 |
|---|---|
| 목적 | 주문 상품 옵션이 유효한지 검증하고 주문 스냅샷 정보를 가져옴 |
| 호출 시점 | 주문 저장 전 |
| 트랜잭션 | Order DB 트랜잭션 밖 |

```text
GET /internal/v1/products/valid
```

응답 데이터는 주문 상품 스냅샷과 Saga item context를 만드는 데 사용합니다.

### 2.2 재고 예약

| 항목 | 내용 |
|---|---|
| 목적 | 주문 상품 수량만큼 재고를 예약 |
| 호출 시점 | PENDING 주문 저장 후 |
| 트랜잭션 | Order DB 트랜잭션 밖 |
| 성공 후 처리 | `markStockReserved(orderId)` |
| 실패 후 처리 | `markFailed(orderId)` |

```text
POST /internal/v1/inventories/reserve
```

재고 예약 성공 결과는 `OrderCreateSagaContext`의 reservations에 저장합니다. 이후 배송 생성 실패 시 보상 요청에 사용합니다.

### 2.3 재고 예약 취소

| 항목 | 내용 |
|---|---|
| 목적 | 이미 성공한 재고 예약을 취소 |
| 호출 시점 | 배송 생성 실패 후 |
| 트랜잭션 | Order DB 트랜잭션 밖 |
| 성공 후 처리 | `markFailed(orderId)` |
| 실패 후 처리 | `markCompensationFailed(orderId)` |

```text
POST /internal/v1/inventories/cancel
```

보상 실패 시 주문 상태는 `COMPENSATION_FAILED`가 됩니다.

---

## 3. Shipment Service

### 3.1 배송 생성

| 항목 | 내용 |
|---|---|
| 목적 | 주문에 대한 배송 생성 |
| 호출 시점 | 재고 예약 성공 후 |
| 트랜잭션 | Order DB 트랜잭션 밖 |
| 성공 후 처리 | `markConfirmed(orderId)` |
| 실패 후 처리 | 재고 예약 취소 보상 |

```text
POST /internal/v1/shipments
```

배송 생성 요청에는 `OrderCreateSagaContext`에 저장된 수령인 정보, 주소 정보, 주문 상품 정보가 사용됩니다. `OrderCreateSaga`는 더 이상 `Order` 엔티티에서 배송 요청 필드를 꺼내지 않습니다.

### 3.2 배송 취소

| 항목 | 내용 |
|---|---|
| 목적 | 주문 취소 시 생성된 배송을 취소 |
| 호출 시점 | `CONFIRMED` 주문 취소 시 |
| 실패 후 처리 | 재고 예약 취소를 진행하지 않고 예외 발생 |

```text
POST /internal/v1/shipments/cancel
```

---

## 4. AI Slack Service

### 4.1 AI 분석 요청

| 항목 | 내용 |
|---|---|
| 목적 | 주문 확정 후 최종 출고 상한 분석 요청 |
| 호출 시점 | 주문 생성 Saga 완료 후 DB 재조회 결과가 `CONFIRMED`일 때 |
| 실패 처리 | 주문 생성 성공을 롤백하지 않음 |

```text
POST /internal/v1/ai-slack/analysis-requests
```

AI 분석 요청은 주문 생성 Saga 내부 작업이 아니라 후처리입니다. 따라서 `FAILED`, `COMPENSATION_FAILED` 상태에서는 호출하지 않습니다.

---

## 5. 트랜잭션 경계와 내부 API 호출

현재 주문 생성의 트랜잭션 경계는 다음과 같습니다.

```text
Company 상품 검증 API
  -> 트랜잭션 없음

Order PENDING 저장
  -> 짧은 트랜잭션

Company 재고 예약 API
  -> 트랜잭션 없음

Order STOCK_RESERVED 저장
  -> 짧은 트랜잭션

Shipment 배송 생성 API
  -> 트랜잭션 없음

Order CONFIRMED 저장
  -> 짧은 트랜잭션

Company 재고 예약 취소 API
  -> 트랜잭션 없음

Order FAILED 또는 COMPENSATION_FAILED 저장
  -> 짧은 트랜잭션
```

이 구조는 외부 API 지연으로 인해 Order DB 커넥션이 장시간 점유되는 문제를 줄입니다.

---

## 6. 실패 처리 요약

| 실패 지점 | 보상 | 최종 주문 상태 |
|---|---|---|
| 상품 검증 실패 | 없음 | 주문 미생성 |
| 재고 예약 실패 | 없음 | `FAILED` |
| 배송 생성 실패 | 재고 예약 취소 성공 | `FAILED` |
| 배송 생성 실패 | 재고 예약 취소 실패 | `COMPENSATION_FAILED` |
| AI 분석 요청 실패 | 없음 | `CONFIRMED` 유지 |

---

## 7. 계약 변경 시 체크리스트

내부 API 계약을 변경할 때는 다음을 확인해야 합니다.

- FeignClient method signature
- request DTO 필드
- response DTO 필드
- `ApiResponse<T>` success/data 처리
- Saga 실패/보상 실패 분기
- 관련 테스트
- 문서 갱신
