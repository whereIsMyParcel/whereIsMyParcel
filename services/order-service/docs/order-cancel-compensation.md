# Order Cancel Compensation

이 문서는 Order Service의 주문 취소 흐름과 상태별 보상 처리 방식을 설명합니다.

주문 취소는 주문 생성 Saga와 비슷하게 외부 서비스와 연동되지만, 구현상 별도 Saga 클래스로 분리하지 않았습니다. 생성은 여러 서비스의 성공을 조합해 `CONFIRMED`를 만드는 분산 확정 흐름이고, 취소는 이미 생성된 주문의 외부 리소스를 정리하고 `CANCELLED`로 종료하는 정리성 유스케이스입니다.

---

## 1. 주문 취소의 기본 원칙

주문 취소는 현재 주문 상태에 따라 필요한 외부 보상 범위가 달라집니다.

| 상태 | 의미 | 취소 처리 |
|---|---|---|
| `PENDING` | 아직 재고 예약 전 또는 생성 초기 | 외부 보상 없이 취소 |
| `STOCK_RESERVED` | 재고 예약 성공 | 재고 예약 취소 후 취소 |
| `CONFIRMED` | 배송 생성까지 성공 | 배송 취소 후 재고 예약 취소, 이후 취소 |

`COMPLETED`, `FAILED`, `COMPENSATION_FAILED`, `CANCELLED` 등은 일반 사용자 취소 흐름과 구분됩니다.

---

## 2. 왜 OrderCancelSaga를 만들지 않았는가

주문 생성은 재고 예약과 배송 생성을 모두 성공시켜 `CONFIRMED`를 만드는 분산 확정 흐름입니다. 반면 주문 취소는 이미 생성된 주문의 현재 상태를 기준으로 필요한 외부 리소스를 정리하고 `CANCELLED`로 종료하는 흐름입니다.

따라서 현재 구현에서는 주문 생성만 `OrderCreateSaga`로 분리하고, 주문 취소는 `cancelOrder()` 유스케이스 내부에서 상태별 분기로 처리합니다.

다만 취소 실패 재시도, 보상 이력 저장, 비동기 이벤트 처리가 필요해진다면 `OrderCancelSaga`로 분리할 수 있습니다.

---

## 3. 상태별 취소 흐름

### 3.1 PENDING 취소

```text
PENDING
  -> cancel()
  -> CANCELLED
```

아직 외부 재고 예약이나 배송 생성이 완료되지 않은 상태이므로 외부 보상이 필요하지 않습니다.

### 3.2 STOCK_RESERVED 취소

```text
STOCK_RESERVED
  -> Company Service 재고 예약 취소
  -> cancel()
  -> CANCELLED
```

재고 예약이 성공한 상태이므로 Company Service에 재고 예약 취소를 요청한 뒤 주문을 취소합니다.

### 3.3 CONFIRMED 취소

```text
CONFIRMED
  -> Shipment Service 배송 취소
  -> Company Service 재고 예약 취소
  -> cancel()
  -> CANCELLED
```

배송 생성까지 완료된 상태이므로 배송 취소를 먼저 수행하고, 이후 재고 예약을 취소합니다.

---

## 4. MASTER 권한 취소

MASTER는 다른 사용자의 주문도 취소할 수 있습니다. 다만 외부 보상 요청에는 실제 주문 소유자 ID를 사용합니다.

예:

```text
MASTER가 owner의 주문 취소 요청
  -> Shipment/Company 보상 호출은 ownerId 기준
```

이는 외부 서비스가 사용자 기준으로 권한 또는 데이터 범위를 확인할 수 있기 때문입니다.

---

## 5. 보상 실패 처리

취소 중 외부 보상이 실패하면 주문 상태를 변경하지 않고 예외를 발생시킵니다.

예:

```text
STOCK_RESERVED
  -> 재고 예약 취소 실패
  -> SagaCompensationFailedException
  -> 주문 상태 STOCK_RESERVED 유지
```

```text
CONFIRMED
  -> 배송 취소 실패
  -> SagaCompensationFailedException
  -> 재고 예약 취소 요청하지 않음
  -> 주문 상태 CONFIRMED 유지
```

취소 보상 실패는 현재 주문 생성 Saga의 `COMPENSATION_FAILED` 상태와 별도로 다룹니다. `COMPENSATION_FAILED`는 주문 생성 중 재고 예약 보상 실패를 추적하기 위해 도입된 상태입니다.

---

## 6. 생성 Saga의 COMPENSATION_FAILED와의 관계

`COMPENSATION_FAILED`는 다음 상황을 표현합니다.

```text
주문 생성 중
재고 예약 성공
-> 배송 생성 실패
-> 재고 예약 취소 실패
-> COMPENSATION_FAILED
```

취소 보상 실패는 이미 존재하는 주문을 취소하는 과정에서 발생하는 별도 유스케이스입니다. 향후 운영 요구가 커지면 취소 보상 실패도 별도 상태나 재처리 큐로 관리할 수 있습니다.

---

## 7. 테스트 포인트

- `PENDING` 주문 취소 시 외부 보상 미호출
- `STOCK_RESERVED` 주문 취소 시 재고 예약 취소 호출
- `CONFIRMED` 주문 취소 시 배송 취소 후 재고 예약 취소 호출
- 재고 예약 취소 실패 시 주문 상태 유지
- 배송 취소 실패 시 재고 예약 취소 미호출
- MASTER 취소 시 실제 주문 소유자 ID로 외부 보상 호출
- 취소 불가능 상태에서 `InvalidOrderStatusException` 발생
