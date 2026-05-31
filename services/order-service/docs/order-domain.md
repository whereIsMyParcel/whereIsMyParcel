# Order Domain

이 문서는 Order Service의 도메인 모델, 상태 전이, 주요 도메인 규칙을 설명합니다. API 사용법보다는 `Order`와 `OrderItem`이 어떤 책임을 가지고, 주문 상태가 어떤 규칙으로 변하는지를 이해하는 데 목적이 있습니다.

---

## 1. 도메인 책임

Order 도메인은 주문의 생명주기를 관리합니다. 주문 생성 과정은 Company Service의 상품 검증·재고 예약, Shipment Service의 배송 생성과 연결되어 있지만, 주문 상태 자체는 Order Service가 책임집니다.

Order 도메인이 관리하는 핵심 질문은 다음과 같습니다.

- 주문은 어떤 상태를 가질 수 있는가?
- 각 상태에서 어떤 상태로 전이할 수 있는가?
- 주문 수정, 취소, 삭제는 어떤 상태에서 가능한가?
- 외부 서비스 실패를 주문 상태로 어떻게 표현할 것인가?

---

## 2. Aggregate Root: Order

`Order`는 주문 도메인의 Aggregate Root입니다. 주문 번호, 수령인 정보, 배송 요청 정보, 주문 상태, 주문 상품 목록을 관리합니다.

주문 상태 전이는 반드시 `Order`의 도메인 메서드를 통해 수행합니다.

```java
order.reserveStock();
order.confirm();
order.fail();
order.failCompensation();
order.cancel();
order.complete();
```

서비스 계층이나 Saga가 `orderStatus` 필드를 직접 변경하지 않는 이유는 상태 전이 규칙을 도메인 내부에 모으기 위해서입니다.

---

## 3. Entity: OrderItem

`OrderItem`은 주문 당시 상품 정보를 스냅샷으로 저장합니다. 주문 이후 상품명이나 가격이 변경되더라도 해당 주문의 상품 정보는 주문 당시 기준으로 유지되어야 합니다.

| 필드 | 설명 |
|---|---|
| `productVariantId` | 상품 옵션 ID |
| `skuCode` | SKU 코드 |
| `productNameSnapshot` | 주문 당시 상품명 |
| `productOptionSnapshot` | 주문 당시 옵션명 |
| `unitPrice` | 주문 당시 가격 |
| `quantity` | 주문 수량 |

---

## 4. Enum: OrderStatus

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

| 상태 | 설명 |
|---|---|
| `PENDING` | 주문이 생성되었고 재고 예약이 아직 확정되지 않은 상태 |
| `STOCK_RESERVED` | 재고 예약이 성공한 상태 |
| `CONFIRMED` | 재고 예약과 배송 생성, 주문 확정 상태 저장이 모두 성공한 상태 |
| `CANCELLED` | 주문이 취소된 상태 |
| `COMPLETED` | 배송 완료가 반영된 상태 |
| `FAILED` | 주문 생성이 실패했지만 필요한 보상이 완료된 상태 |
| `COMPENSATION_FAILED` | 외부 서비스에 이미 반영된 작업의 보상 처리까지 실패해 운영자 확인이 필요한 상태 |

`COMPENSATION_FAILED`는 일반 실패와 구분되는 운영 추적 상태입니다. 재고 예약 취소 실패뿐 아니라, 배송 생성 이후 주문 확정 저장 실패 상황에서 배송 취소 또는 재고 예약 취소 보상이 실패한 경우도 포함할 수 있습니다. 즉, 주문 생성 Saga 중 외부 서비스에 반영된 작업을 되돌리지 못했을 가능성이 있는 상태입니다.

---

## 5. 생성 시 기본 상태

주문이 생성되면 기본 상태는 `PENDING`입니다.

```text
Order.create(...)
  -> PENDING
```

`PENDING`은 주문 row가 생성되었지만, 아직 재고 예약과 배송 생성이 완료되지 않았음을 의미합니다.

---

## 6. 상태 전이 규칙

### 6.1 reserveStock()

```text
PENDING -> STOCK_RESERVED
```

재고 예약이 성공했을 때 호출합니다. 현재 상태가 `PENDING`이 아니면 `InvalidOrderStatusException`이 발생합니다.

### 6.2 confirm()

```text
STOCK_RESERVED -> CONFIRMED
```

배송 생성까지 성공하고 주문 확정 상태 저장까지 가능할 때 호출합니다. 현재 상태가 `STOCK_RESERVED`가 아니면 `InvalidOrderStatusException`이 발생합니다.

### 6.3 fail()

```text
PENDING        -> FAILED
STOCK_RESERVED -> FAILED
```

주문 생성 Saga 중 일반 실패가 발생했고 필요한 보상이 모두 성공했을 때 호출합니다.

- `PENDING -> FAILED`: 재고 예약 자체가 실패한 경우
- `STOCK_RESERVED -> FAILED`: 배송 생성 실패 후 재고 예약 취소 보상이 성공한 경우
- `STOCK_RESERVED -> FAILED`: 배송 생성 성공 후 주문 확정 저장 실패가 발생했지만 배송 취소와 재고 예약 취소 보상이 모두 성공한 경우

`FAILED`는 정합성이 회복된 실패 상태에 가깝습니다.

### 6.4 failCompensation()

```text
STOCK_RESERVED -> COMPENSATION_FAILED
```

주문 생성 Saga에서 외부 작업 보상 처리까지 실패했을 때 호출합니다.

예시는 다음과 같습니다.

- 배송 생성 실패 후 재고 예약 취소 보상이 실패한 경우
- 배송 생성 성공 후 주문 확정 저장이 실패했고, 이후 배송 취소 또는 재고 예약 취소 보상이 실패한 경우

`failCompensation()`은 `STOCK_RESERVED` 상태에서만 허용됩니다. `PENDING` 상태에서는 아직 보상할 외부 성공 작업이 없으므로 `COMPENSATION_FAILED`로 전이할 수 없습니다.

### 6.5 cancel()

취소 가능한 상태에서 주문을 `CANCELLED`로 변경합니다.

```text
PENDING        -> CANCELLED
STOCK_RESERVED -> CANCELLED
CONFIRMED      -> CANCELLED
```

취소 시 외부 보상은 상태에 따라 애플리케이션 서비스에서 수행합니다.

### 6.6 complete()

```text
CONFIRMED -> COMPLETED
```

배송이 완료되었을 때 호출합니다. 이미 `COMPLETED`인 주문에 대한 완료 요청은 멱등적으로 성공 처리할 수 있습니다.

---

## 7. 주문 생성 상태 머신

```text
PENDING
  ├─ 재고 예약 실패 -> FAILED
  └─ 재고 예약 성공 -> STOCK_RESERVED
        ├─ 배송 생성 실패
        │     ├─ 재고 예약 취소 성공 -> FAILED
        │     └─ 재고 예약 취소 실패 -> COMPENSATION_FAILED
        └─ 배송 생성 성공
              ├─ 주문 확정 저장 성공 -> CONFIRMED
              └─ 주문 확정 저장 실패
                    ├─ 배송 취소 + 재고 예약 취소 성공 -> FAILED
                    └─ 배송 취소 또는 재고 예약 취소 실패 -> COMPENSATION_FAILED
```

이 상태 머신의 핵심은 `FAILED`와 `COMPENSATION_FAILED`를 구분하는 것입니다.

| 실패 유형 | 상태 | 의미 |
|---|---|---|
| 재고 예약 실패 | `FAILED` | 외부 성공 작업 없음 |
| 배송 생성 실패 + 재고 예약 취소 성공 | `FAILED` | 보상 성공, 정합성 회복 |
| 배송 생성 실패 + 재고 예약 취소 실패 | `COMPENSATION_FAILED` | 재고 예약이 남아 있을 수 있음 |
| 배송 생성 성공 + 주문 확정 저장 실패 + 배송/재고 보상 성공 | `FAILED` | 보상 성공, 정합성 회복 |
| 배송 생성 성공 + 주문 확정 저장 실패 + 배송/재고 보상 실패 | `COMPENSATION_FAILED` | 배송 또는 재고 예약이 남아 있을 수 있음 |

---

## 8. 주문 수정 규칙

주문 요청 정보 수정은 `PENDING`, `STOCK_RESERVED` 상태에서만 허용합니다.

```text
PENDING        -> 수정 가능
STOCK_RESERVED -> 수정 가능
CONFIRMED      -> 수정 불가
```

`CONFIRMED` 이후에는 배송 생성까지 완료된 상태이므로 요청 메모나 희망 배송일을 변경하면 배송 정보와 불일치할 수 있습니다.

---

## 9. 주문 취소 규칙

주문 취소는 도메인 상태 전이와 외부 보상이 함께 필요합니다.

| 상태 | 외부 보상 | 최종 상태 |
|---|---|---|
| `PENDING` | 없음 | `CANCELLED` |
| `STOCK_RESERVED` | 재고 예약 취소 | `CANCELLED` |
| `CONFIRMED` | 배송 취소 후 재고 예약 취소 | `CANCELLED` |

`COMPENSATION_FAILED` 상태 주문은 현재 취소 대상이라기보다 운영자 확인 또는 재처리 대상입니다. 추후 운영 기능에서 별도 정책을 정의할 수 있습니다.

---

## 10. 주문 삭제 규칙

삭제는 비즈니스적으로 종료된 상태에서만 허용합니다.

현재 `Order.isDeletable()`이 허용하는 상태는 다음과 같습니다.

- `CANCELLED`
- `COMPLETED`
- `FAILED`

`COMPENSATION_FAILED`는 현재 코드상 삭제 불가 상태입니다. 외부 서비스 정합성이 확인되지 않은 상태이므로 관리자 확인 전 삭제를 제한하는 것이 의도된 정책입니다. 향후 운영 정책에 따라 별도 처리 절차를 정의한 후 삭제 허용 여부를 결정할 수 있습니다.

---

## 11. 최종 출고 상한 업데이트

AI Slack Service가 계산한 `finalDispatchDeadline`은 `CONFIRMED` 상태 주문에만 반영합니다.

```text
CONFIRMED -> finalDispatchDeadline 업데이트 가능
```

`PENDING`, `STOCK_RESERVED`, `FAILED`, `COMPENSATION_FAILED`, `CANCELLED`, `COMPLETED` 상태에서는 업데이트 의미가 없거나 위험합니다.

---

## 12. 예외

| 예외 | 발생 상황 |
|---|---|
| `InvalidOrderStatusException` | 허용되지 않은 상태 전이 |
| `OrderNotFoundException` | 주문 없음 또는 접근 권한 없음 |
| `SagaFailedException` | 주문 생성 Saga 일반 실패 |
| `SagaCompensationFailedException` | 외부 작업 보상 처리 실패 |

---

## 13. 테스트 포인트

상태 전이 테스트는 다음을 포함해야 합니다.

- `PENDING -> STOCK_RESERVED`
- `STOCK_RESERVED -> CONFIRMED`
- `PENDING -> FAILED`
- `STOCK_RESERVED -> FAILED`
- `STOCK_RESERVED -> COMPENSATION_FAILED`
- `PENDING -> COMPENSATION_FAILED` 실패
- `COMPENSATION_FAILED -> CONFIRMED` 실패
- `COMPENSATION_FAILED -> CANCELLED` 실패
- `COMPENSATION_FAILED -> FAILED` 실패
- `COMPENSATION_FAILED -> COMPLETED` 실패

---

## 14. 향후 확장

### 14.1 상태 변경 이력

현재는 최종 상태만 Order row에 남습니다. 향후 상태 변경 이력을 별도 테이블로 관리하면 장애 분석과 운영 추적에 도움이 됩니다.

### 14.2 보상 실패 재처리

`COMPENSATION_FAILED` 상태를 기반으로 관리자 재처리 기능을 만들 수 있습니다.

```text
GET  /admin/orders?status=COMPENSATION_FAILED
POST /admin/orders/{orderId}/compensation/retry
```

재보상 성공 시 `FAILED`로 정리하고, 실패 시 `COMPENSATION_FAILED`를 유지하면서 재시도 횟수와 마지막 실패 사유를 갱신할 수 있습니다.

### 14.3 외부 서비스 상태 정합성 점검

Order Service는 주문 상태를 관리하지만 실제 재고와 배송 상태는 외부 서비스에 있습니다. 장기적으로는 다음 점검이 필요합니다.

- 주문 상태와 재고 예약 상태 비교
- 주문 상태와 배송 상태 비교
- 보상 실패 주문 재처리
- 이벤트 기반 정합성 점검
