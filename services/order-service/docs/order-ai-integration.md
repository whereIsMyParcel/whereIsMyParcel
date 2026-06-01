# Order AI Integration

이 문서는 Order Service와 AI Slack Service의 연계 방식을 설명합니다. Order Service는 주문 생성 Saga가 성공해 주문이 `CONFIRMED` 상태로 저장된 경우에만 AI Slack Service에 분석 요청을 보냅니다.

최근 주문 생성 Saga의 트랜잭션 경계가 분리되면서 AI Trigger 판단 기준도 명확해졌습니다. 이제 `createOrder()`는 Saga 실행 후 `orderId`로 주문을 재조회하고, DB에 커밋된 최신 상태가 `CONFIRMED`일 때만 AI 분석 요청을 수행합니다.

---

## 1. AI 연동의 목적

AI Slack Service는 주문과 배송 정보를 바탕으로 최종 출고 상한을 계산하고 Slack 알림을 처리합니다.

Order Service 관점에서 AI 연동은 주문 생성의 핵심 트랜잭션이 아니라 후처리입니다.

따라서 다음 원칙을 따릅니다.

- 주문이 `CONFIRMED`인 경우에만 AI 분석 요청
- AI 요청 실패가 주문 생성 성공을 롤백하지 않음
- AI 결과 반영은 별도 내부 API로 수행
- `FAILED`, `COMPENSATION_FAILED` 상태 주문에는 AI 요청하지 않음

---

## 2. 주문 생성 후 AI Trigger 흐름

```text
OrderService.createOrder()
  -> 상품 검증
  -> PENDING 저장
  -> OrderCreateSaga.execute(context)
  -> 주문 재조회
  -> orderStatus == CONFIRMED 확인
  -> AI Slack Service 분석 요청
  -> OrderCreateResponse 반환
```

이전에는 주문 생성 트랜잭션의 `afterCommit` 콜백이 중요한 역할을 했습니다. 현재는 주문 생성 상태 저장이 `OrderCreationStateService`의 짧은 트랜잭션으로 분리되었고, `markConfirmed(orderId)`가 완료된 뒤 주문을 재조회합니다. 따라서 AI Trigger는 DB에 저장된 최종 상태를 기준으로 판단합니다.

---

## 3. AI Trigger 조건

AI 분석 요청 조건은 다음과 같습니다.

```text
주문 상태 == CONFIRMED
```

상태별 처리:

| 주문 상태 | AI Trigger 여부 | 이유 |
|---|---|---|
| `PENDING` | 미호출 | 주문 확정 전 |
| `STOCK_RESERVED` | 미호출 | 배송 생성 완료 전 |
| `CONFIRMED` | 호출 | 재고 예약과 배송 생성 모두 성공 |
| `FAILED` | 미호출 | 주문 생성 실패 |
| `COMPENSATION_FAILED` | 미호출 | 보상 실패로 운영 확인 필요 |
| `CANCELLED` | 미호출 | 취소된 주문 |
| `COMPLETED` | 일반 생성 후 트리거 대상 아님 | 이미 완료된 주문 |

---

## 4. AI 분석 요청 API

Order Service는 AI Slack Service에 내부 API로 분석 요청을 보냅니다.

```text
POST /internal/v1/ai-slack/analysis-requests
```

요청에는 주문 ID가 포함됩니다. AI Slack Service는 이후 Order Service와 Shipment Service를 조회해 분석에 필요한 컨텍스트를 구성합니다.

AI 분석 요청 실패는 주문 생성 실패로 전파하지 않습니다. 주문은 이미 `CONFIRMED` 상태로 확정되었기 때문입니다.

---

## 5. finalDispatchDeadline 반영

AI Slack Service는 분석 결과로 최종 출고 상한을 계산한 뒤 Order Service 내부 API를 호출해 반영합니다.

```text
PATCH /internal/v1/orders/{orderId}/dispatch-deadline
```

Order Service는 주문 상태가 `CONFIRMED`일 때만 finalDispatchDeadline 업데이트를 허용합니다.

```text
CONFIRMED -> finalDispatchDeadline 업데이트 가능
```

그 외 상태에서는 `InvalidOrderStatusException`이 발생할 수 있습니다.

---

## 6. 트랜잭션 경계

### 6.1 주문 생성 Saga와 AI 요청

AI 요청은 Saga 내부 단계가 아닙니다. Saga의 책임은 재고 예약, 배송 생성, 보상 처리, 주문 상태 저장까지입니다.

AI 요청은 다음 조건이 충족된 후 수행합니다.

1. Saga 실행 종료
2. 주문 상태 재조회
3. DB 상태가 `CONFIRMED`

### 6.2 AI 요청 실패 격리

AI 요청 실패는 주문 생성 상태를 변경하지 않습니다.

```text
CONFIRMED 주문
  -> AI 요청 실패
  -> 주문 상태 CONFIRMED 유지
  -> 로그 기록
```

이 설계는 핵심 주문 생성 흐름과 부가 후처리를 분리하기 위한 것입니다.

### 6.3 AI Slack Service 내부 처리

AI Slack Service 내부에서는 Gemini, Slack 같은 외부 Provider 호출과 내부 상태 기록을 분리하는 방향을 사용합니다. 이는 외부 Provider 지연/실패가 내부 DB 트랜잭션과 강하게 결합되지 않도록 하기 위한 설계입니다.

---

## 7. 실패 시나리오

### 7.1 주문 생성 실패

```text
재고 예약 실패 또는 배송 생성 실패
-> 주문 상태 FAILED
-> AI Trigger 미호출
```

### 7.2 보상 실패

```text
재고 예약 성공
-> 배송 생성 실패
-> 재고 예약 취소 실패
-> 주문 상태 COMPENSATION_FAILED
-> AI Trigger 미호출
```

### 7.3 AI 요청 실패

```text
주문 상태 CONFIRMED
-> AI 요청 실패
-> 주문 생성 응답은 CONFIRMED 유지
-> 로그 기록
```

---

## 8. 테스트 포인트

- 주문 생성 성공 후 `CONFIRMED` 상태이면 AI Trigger 호출
- `FAILED` 상태 응답이면 AI Trigger 미호출
- `COMPENSATION_FAILED` 상태 응답이면 AI Trigger 미호출
- AI 분석 요청 실패 시에도 주문 생성 응답은 `CONFIRMED` 유지
- finalDispatchDeadline 업데이트는 `CONFIRMED` 상태에서만 허용

---

## 9. 향후 확장

현재 AI 요청은 주문 확정 후 동기 Feign 호출로 수행됩니다. 향후 확장 방향은 다음과 같습니다.

- AI 분석 요청 Outbox 저장
- 비동기 이벤트 기반 AI 분석 요청
- AI 요청 실패 재시도
- AI 분석 상태를 Order에 일부 반영
- 운영자 재분석 요청 기능
