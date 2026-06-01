# AI Slack Service Module

주문 및 배송 데이터를 기반으로 **Gemini AI**를 활용하여 최적의 발송 시한을 분석하고, **Slack** 알림을 발송하는 서비스 모듈입니다.

---

## 🚀 서비스 개요
본 서비스는 주문, 배송, 사용자 데이터를 조합하여 AI 분석을 수행하고, 도출된 결과를 관련 서비스에 업데이트하며 수령인에게 알림을 전송하는 역할을 수행합니다.

### 주요 책임
* **AI 분석 요청:** 주문/배송 데이터를 기반으로 분석 요청 생성
* **AI 분석 수행:** Google Gemini AI를 호출하여 최적의 발송 시한 계산
* **결과 전파:** 분석 결과를 `Order Service`에 즉시 반영 (PATCH)
* **Slack 알림:** AI 결과 및 배송 정보를 사용자에게 Slack으로 전송
* **메시지 관리:** 발송 상태 관리 및 실패 시 재시도 로직(Retry) 수행

---

## 🏗️ 핵심 설계
본 모듈은 계층 간 결합도를 낮추고 각 컴포넌트의 역할을 명확히 분리하여 설계되었습니다.

| 컴포넌트 | 역할 |
| :--- | :--- |
| **AiMessageService** | AI 분석 오케스트레이터 (Feign Client 호출, AI 연동) |
| **SlackMessageService** | Slack 알림 관리 (재시도 로직 및 상태 관리) |
| **PromptGenerator** | AI 분석을 위한 동적 프롬프트 생성 |
| **AiMessageTransactionService** | 트랜잭션 경계 분리 및 DB 쓰기 작업 독립 처리 |

---

## 🔗 API 연동
### 외부 API 연동
* **Google Gemini AI**
    * **목적:** 주문·배송·사용자 정보를 기반으로 최적의 발송 시한(Deadline) 분석
    * **방식:** Spring AI `ChatModel` 활용 → 동적 프롬프트 전달 → 결과 파싱
* **Slack API**
    * **목적:** 분석 결과 및 배송 정보 알림 전송
    * **방식:** Slack SDK 사용 (발송 실패 시 최대 3회 재시도)

### 내부 API 연동 (Feign Client)
| 클라이언트 | 목적 | 주요 엔드포인트 |
| :--- | :--- | :--- |
| **OrderFeignClient** | 주문 정보 조회 및 분석 결과 업데이트 | `GET /api/v1/orders/{orderId}`, `PATCH /api/v1/orders/{orderId}/delivery-deadline` |
| **ShipmentFeignClient** | 배송 상세 정보 조회 | `GET /api/v1/shipments/by-order/{orderId}` |
| **UserFeignClient** | 사용자 정보 조회 | `GET /api/v1/users/{userId}` |

---

## 🌐 REST API Endpoints
`AiSlackController`를 통해 제공되는 주요 엔드포인트입니다.

| HTTP Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | `/api/v1/ai-slack/{aiMessageId}` | 특정 AI Slack 메시지 단건 조회 |
| GET | `/api/v1/ai-slack` | AI Slack 메시지 목록 조회 (페이징/필터링) |
| PUT | `/api/v1/ai-slack/{aiMessageId}` | 특정 AI Slack 메시지 내용 수정 |
| DELETE | `/api/v1/ai-slack/{aiMessageId}` | 특정 AI Slack 메시지 삭제 |

---

## 📂 패키지 구조
DDD(Domain-Driven Design) 원칙을 따른 표준 구조입니다.

```text
src/main/java/com/sparta/whereismyparcel/aislack
├── application     // 서비스 로직 및 오케스트레이션
├── domain          // 도메인 엔티티 및 비즈니스 규칙
├── infrastructure  // 외부 통신 (Feign Client) 및 설정
└── presentation    // REST API 엔드포인트
```

## 💡 주요 기술 사양
* **AI Engine:** Google Gemini AI (via Spring AI `ChatModel`)
* **Communication:** OpenFeign을 통한 내부 서비스 통신
* **Transaction:** 비즈니스 로직과 외부 API 호출의 트랜잭션 분리 (REQUIRES_NEW 활용)
* **Resilience:** Slack 메시지 발송 실패 시 최대 3회 재시도 정책 적용

---

## ⚠️ 변경 시 체크리스트
작업 전 아래 사항을 반드시 확인하세요:

- [ ] **AI 프롬프트 변경 시:** 응답 파싱 로직(`extractFinalDispatchDeadline`)의 포맷 호환성 확인
- [ ] **Feign Client 변경 시:** 관련 DTO 매핑 및 호출 로직 업데이트
- [ ] **Slack API 연동 변경 시:** 재시도 로직 및 영구 실패(`PERMANENT_FAILED`) 처리 검토
- [ ] **트랜잭션 변경 시:** 데이터 일관성을 위한 전파 속성(`Propagation`) 재검토
