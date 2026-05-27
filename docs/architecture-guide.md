# 개발팀 온보딩 및 아키텍처 가이드

> 로컬 세팅을 마친 팀원들이 "이 프로젝트가 어떻게 돌아가는가"를 빠르게 이해하기 위한 문서입니다.

---

## 🚀 1. 프로젝트 인프라 개요

### 왜 MSA인가?

이 프로젝트는 **마이크로서비스 아키텍처(MSA)** 로 구성되어 있습니다. MSA는 하나의 거대한 앱 대신, 기능별로 독립된 작은 서비스들이 서로 협력하는 방식입니다.

```
모놀리식 (기존 방식)        MSA (우리 방식)
┌──────────────────┐        ┌──────┐ ┌──────┐ ┌──────┐
│                  │        │ user │ │ hub  │ │order │
│  모든 기능이     │   →    └──────┘ └──────┘ └──────┘
│  하나의 앱에     │           서비스들이 API로 통신
│                  │
└──────────────────┘
```

**팀 프로젝트에서 MSA를 쓰는 이유:**
- 각자 담당 서비스를 독립적으로 개발하고 배포할 수 있습니다
- 한 서비스에 버그가 생겨도 전체가 죽지 않습니다
- 서비스별로 기술 스택이나 DB 스키마를 독립적으로 관리합니다

### 왜 Docker Compose인가?

MSA는 서비스가 많아서 로컬에서 세팅이 복잡합니다. Docker Compose는 **"이 서비스들을 이 순서로, 이 설정으로 한번에 띄워줘"** 를 한 파일로 정의합니다.

```bash
# infra/ 디렉토리에서 실행
cd infra
docker compose up -d
```

팀원 누구든 이 명령 하나로 동일한 환경을 갖게 됩니다.

---

## 🔄 2. 전체 트래픽 흐름

### 로그인 흐름

```
① 클라이언트 → Keycloak
   POST /realms/sparta-logistics/protocol/openid-connect/token
   (아이디/비밀번호 전송)

② Keycloak → 클라이언트
   { "access_token": "eyJhbGci..." }
   (JWT 토큰 발급)
```

### API 요청 흐름

```
③ 클라이언트 → API Gateway (:8000)
   GET /api/orders/123
   Authorization: Bearer eyJhbGci...

④ API Gateway → Keycloak JWKS 검증
   "이 토큰 진짜야?" → 서명 검증 (토큰 위변조 확인)

⑤ API Gateway → Eureka (:8761)
   "order-service 어디있어?" → 주소 조회

⑥ API Gateway → order-service
   원본 요청 + X-User-* 헤더 주입
   X-User-Id: "abc-123-uuid"
   X-User-Role: "COMPANY_MANAGER"
   X-Username: "testuser01"
   X-User-Status: "APPROVED"

⑦ order-service → PostgreSQL
   비즈니스 로직 처리 + DB 저장

⑧ order-service → 클라이언트
   응답 반환
```

### 핵심 포인트

> **도메인 서비스(user, hub, order...)는 JWT를 직접 검증하지 않습니다.**
>
> Gateway가 이미 검증을 마치고 `X-User-*` 헤더로 사용자 정보를 전달하므로, 각 서비스는 이 헤더만 신뢰하면 됩니다. 내부 Docker 네트워크에서만 통신하므로 헤더 위조 위험이 없습니다.

```java
// 도메인 서비스에서 사용자 정보 꺼내는 방법
String userId = request.getHeader("X-User-Id");
String role   = request.getHeader("X-User-Role");
```

### 서비스 기동 순서

컨테이너들은 의존 관계에 따라 순서가 보장됩니다.

```
postgres (healthy)
    └→ keycloak (healthy)
    
eureka-server (healthy)
    └→ config-server (healthy)
        └→ api-gateway, user-service, hub-service ...
```

---

## 🛠️ 3. 핵심 인프라 컴포넌트 역할

### 🏢 API Gateway (`:8000`)

**비유: 건물 출입 안내 데스크**

모든 외부 요청이 반드시 거쳐야 하는 단일 진입점입니다.

| 역할 | 설명 |
|------|------|
| JWT 검증 | Bearer 토큰 서명·만료 확인 |
| 헤더 주입 | `X-User-Id`, `X-User-Role` 등 사용자 정보 주입 |
| 라우팅 | URL 패턴에 따라 적절한 서비스로 전달 |
| 퍼블릭 경로 허용 | `/api/users/signup` 등 인증 없이 접근 가능한 경로 관리 |

### 🔑 Keycloak (`:8080`)

**비유: 신분증 발급소 + 검문소**

로그인 처리와 JWT 발급을 전담합니다. user-service가 Keycloak Admin API를 통해 사용자를 생성/승인합니다.

```
회원가입 → user-service → Keycloak 계정 생성 (enabled=false, 로그인 불가)
MASTER 승인 → user-service → Keycloak enabled=true → 로그인 가능
```

Realm `sparta-logistics`에 우리 프로젝트 설정이 담겨 있으며, 컨테이너 기동 시 `realm-export.json`으로 자동 세팅됩니다.

### 📋 Eureka Server (`:8761`)

**비유: 서비스 전화번호부**

각 서비스는 시작할 때 Eureka에 자신의 위치(IP:포트)를 등록합니다. Gateway나 다른 서비스가 특정 서비스를 호출할 때 IP를 하드코딩하는 대신 Eureka에 물어봅니다.

```
user-service 시작 → "나 172.18.0.5:8081이야" → Eureka 등록
api-gateway → "user-service 어디있어?" → Eureka 조회 → 172.18.0.5:8081
```

브라우저에서 `http://localhost:8761` 접속하면 등록된 서비스 목록을 확인할 수 있습니다.

### ⚙️ Config Server (`:8888`)

**비유: 팀 공용 설정 파일 관리자**

각 서비스의 설정(포트, DB URL, JPA 설정 등)을 별도 Git 저장소(config-hub)에서 중앙 관리합니다.

```
서비스 시작
  → Eureka에서 config-server 위치 탐색
  → Config Server에서 내 설정 파일 수신
  → 설정 적용 후 본격 시작
```

덕분에 각 서비스의 `application.yaml`은 Config Server를 찾는 방법만 담고 있습니다. 실제 설정은 전부 config-hub에 있습니다.

### 🔍 Zipkin (`:9411`)

**비유: 분산 환경 블랙박스 CCTV**

하나의 요청이 여러 서비스를 거칠 때, 어느 서비스에서 얼마나 걸렸는지 추적합니다.

```
요청 → gateway(5ms) → order-service(20ms) → company-service(15ms)
                                총 40ms 소요, 병목은 order-service
```

`http://localhost:9411` 에서 요청 흐름과 지연 시간을 시각화해서 확인할 수 있습니다.

### ⚡ Redis (`:6379`)

**비유: 빠른 접근을 위한 메모리 쪽지**

DB까지 가기엔 무거운 데이터를 메모리에 잠깐 저장해둡니다.

| 캐시 대상 | TTL | 담당 서비스 |
|-----------|-----|------------|
| 사용자 인증 컨텍스트 | 5분 | api-gateway, user-service |
| 허브 정보 | 1시간 | hub-service |
| 허브 간 이동 경로 | 6시간 | hub-service |
| 배송 담당자 배정 순번 | - | shipment-service |

### 🗄️ PostgreSQL (`:5432`)

**비유: 서비스별 전용 서류 보관함**

단일 PostgreSQL 인스턴스에 서비스별 스키마를 분리합니다. 물리적으로는 같은 DB지만 논리적으로는 완전히 분리됩니다.

```
sparta_logistics (DB)
├── user_db        → user-service 전용
├── hub_db         → hub-service 전용
├── company_db     → company-service 전용
├── order_db       → order-service 전용
├── shipment_db    → shipment-service 전용
└── notification_db → ai-slack-service 전용
```

**서비스 간 DB 직접 접근은 금지입니다.** order-service가 user 정보가 필요하면 user-service API를 FeignClient로 호출해야 합니다.

---

## 📌 개발 시 자주 쓰는 것들

```bash
# 내 서비스만 재빌드해서 올리기
docker compose up -d --build hub-service

# 전체 로그 보기
docker compose logs -f

# 특정 서비스 로그만 보기
docker compose logs -f user-service

# DB 스키마 확인
docker exec -it postgres psql -U sparta -d sparta_logistics -c "\dn"
```
