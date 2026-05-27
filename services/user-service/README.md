# User Service

`user-service`는 회원 가입·승인·거절·조회·목록 및 Keycloak 계정 라이프사이클을 관리하는 서비스입니다.

전체 멀티모듈 프로젝트 구조는 [Project Structure](../../docs/project-structure.md)를 참고합니다.

## 구현 현황

| 기능 | 상태 |
| --- | --- |
| 회원가입 `POST /api/v1/auth/signup` | ✅ 완료 |
| 회원 승인 `PATCH /api/v1/users/{userId}/approve` | ✅ 완료 |
| 회원 거절 `PATCH /api/v1/users/{userId}/reject` | ✅ 완료 |
| 회원 단건 조회 `GET /api/v1/users/{userId}` | ✅ 완료 |
| 회원 목록 조회 `GET /api/v1/users` | ✅ 완료 |
| 회원 수정 `PATCH /api/v1/users/{userId}` | ✅ 완료 |
| 회원 삭제 `DELETE /api/v1/users/{userId}` | ✅ 완료 |
| 내부 회원 조회 `GET /internal/v1/users/{userId}` | ✅ 완료 |
| 내부 사업자번호 조회 `GET /internal/v1/users/by-business-number/{businessNumber}` | ✅ 완료 |
| 내부 Slack ID 조회 `GET /internal/v1/users/by-slack/{slackId}` | ✅ 완료 |
| 내부 소속 회사 연결 `PATCH /internal/v1/users/{userId}/companies/{companyId}` | ✅ 완료 |
| 내부 소속 회사 해제 `DELETE /internal/v1/users/{userId}` | ✅ 완료 |
| 내부 회사 전체 회원 해제 `DELETE /internal/v1/users/companies/{companyId}` | ✅ 완료 |

---

## API 명세

### POST /api/v1/auth/signup — 회원가입

인증 불필요 (게이트웨이 공개 엔드포인트)

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `username` | `String` | ✅ | 4~10자, 소문자·숫자만 허용 |
| `name` | `String` | ✅ | 최대 50자 |
| `email` | `String` | ✅ | 이메일 형식 |
| `password` | `String` | ✅ | 8~15자, 대소문자·숫자·특수문자 포함 |
| `phone` | `String` | ❌ | `010-1234-5678` 형식 (하이픈 생략 가능) |
| `slackId` | `String` | ✅ | Slack 사용자 ID (시스템 내 고유값) |
| `role` | `UserRole` | ✅ | `MASTER` \| `HUB_MANAGER` \| `DELIVERY_MANAGER` \| `COMPANY_MANAGER` |
| `businessNumber` | `String` | ❌ | `123-45-67890` 형식, COMPANY_MANAGER만 사용 |
| `hubId` | `UUID` | ❌ | HUB_MANAGER용 허브 ID |
| `companyId` | `UUID` | ❌ | COMPANY_MANAGER용 회사 ID |

**Response** `201 Created`

```json
{
  "success": true,
  "status": 201,
  "errorCode": null,
  "message": "Created",
  "data": {
    "userId": "9e4813b2-2642-4729-ad9c-f3f7d3abcb51",
    "username": "user01",
    "name": "홍길동",
    "email": "hong@example.com",
    "role": "COMPANY_MANAGER",
    "status": "PENDING"
  }
}
```

> 가입 직후 `status = PENDING`, Keycloak 계정 `enabled = false` 상태로 생성됩니다. 관리자 승인 전까지 로그인 불가합니다.

---

### PATCH /api/v1/users/{userId}/approve — 회원 승인

`X-User-Role: MASTER` 필요

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `UUID` | 승인할 회원의 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": {
    "userId": "9e4813b2-2642-4729-ad9c-f3f7d3abcb51",
    "username": "user01",
    "status": "APPROVED"
  }
}
```

> 승인 시 Keycloak 계정이 `enabled = true`로 변경되어 로그인이 가능해집니다.
> `PENDING` 상태가 아닌 회원에게 호출하면 `USER-004` 에러가 반환됩니다.

---

### PATCH /api/v1/users/{userId}/reject — 회원 거절

`X-User-Role: MASTER` 필요

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `UUID` | 거절할 회원의 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": {
    "userId": "9e4813b2-2642-4729-ad9c-f3f7d3abcb51",
    "username": "user01",
    "status": "REJECTED"
  }
}
```

> `PENDING` 상태가 아닌 회원에게 호출하면 `USER-004` 에러가 반환됩니다.

---

### GET /api/v1/users/{userId} — 회원 단건 조회

`MASTER` 또는 본인만 조회 가능

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `UUID` | 조회할 회원의 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": {
    "userId": "9e4813b2-2642-4729-ad9c-f3f7d3abcb51",
    "username": "user01",
    "name": "홍길동",
    "email": "hong@example.com",
    "phone": "010-1234-5678",
    "role": "COMPANY_MANAGER",
    "status": "APPROVED",
    "slackId": "U012AB3CD",
    "businessNumber": "123-45-67890",
    "hubId": null,
    "companyId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
  }
}
```

---

### GET /api/v1/users — 회원 목록 조회

`X-User-Role: MASTER` 필요

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `role` | `UserRole` | ❌ | 역할 필터 (`MASTER` \| `HUB_MANAGER` \| `DELIVERY_MANAGER` \| `COMPANY_MANAGER`) |
| `status` | `UserStatus` | ❌ | 상태 필터 (`PENDING` \| `APPROVED` \| `REJECTED`) |
| `page` | `int` | ❌ | 페이지 번호 (기본값: `0`) |
| `size` | `int` | ❌ | 페이지 크기 (`10` \| `30` \| `50`, 기본값: `10`, 이외 값은 `10`으로 고정) |
| `sort` | `String` | ❌ | 정렬 기준 (`createdAt` \| `updatedAt`, 기본값: `createdAt,desc`) |

**Response** `200 OK`

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": {
    "content": [  ],
    "totalElements": 42,
    "totalPages": 5,
    "size": 10,
    "number": 0
  }
}
```

---

### PATCH /api/v1/users/{userId} — 회원 수정

`MASTER` 또는 본인만 가능

**Request Body** — 전달한 필드만 수정됨 (null이면 기존 값 유지)

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `name` | `String` | 이름 (최대 50자) |
| `phone` | `String` | 전화번호 (`010-1234-5678` 형식) |
| `slackId` | `String` | Slack 사용자 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": {
    "userId": "9e4813b2-2642-4729-ad9c-f3f7d3abcb51",
    "username": "user01",
    "name": "변경된이름",
    "email": "hong@example.com",
    "phone": "010-9999-8888",
    "role": "COMPANY_MANAGER",
    "status": "APPROVED",
    "slackId": "NEW_SLACK_ID",
    "businessNumber": "123-45-67890",
    "hubId": null,
    "companyId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
  }
}
```

---

### DELETE /api/v1/users/{userId} — 회원 삭제

`X-User-Role: MASTER` 필요

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `UUID` | 삭제할 회원의 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": null
}
```

> Soft delete 처리 (`deleted_at` 세팅) 및 Keycloak 계정 `enabled = false`로 비활성화됩니다.

---

## 내부 API (서비스 간 통신 전용)

게이트웨이를 거치지 않고 다른 서비스가 Feign으로 직접 호출하는 엔드포인트입니다. 인증 불필요 (`/internal/**` permitAll).
Swagger UI에 노출되지 않습니다 (`@Hidden`).

### GET /internal/v1/users/{userId}

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `UUID` | 조회할 회원의 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": {
    "userId": "9e4813b2-2642-4729-ad9c-f3f7d3abcb51",
    "username": "user01",
    "name": "홍길동",
    "email": "user01@example.com",
    "role": "COMPANY_MANAGER",
    "status": "APPROVED",
    "slackId": "U012AB3CD",
    "businessNumber": "123-45-67890",
    "hubId": null,
    "companyId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
  }
}
```

### GET /internal/v1/users/by-business-number/{businessNumber}

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `businessNumber` | `String` | 사업자등록번호 (`123-45-67890` 형식) |

**Response** `200 OK` — 위와 동일한 `InternalUserResponse`

> company-service가 COMPANY_MANAGER 검증 시 사용합니다.

### GET /internal/v1/users/by-slack/{slackId}

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `slackId` | `String` | Slack 사용자 ID |

**Response** `200 OK` — 위와 동일한 `InternalUserResponse`

> shipment-service가 배송담당자 등록 시 Slack ID 유효성 검증 용도로 사용합니다.
> `status`, `role` 필터링은 하지 않으며, 호출하는 서비스에서 반환된 응답의 `status`, `role`을 검증합니다.

### PATCH /internal/v1/users/{userId}/companies/{companyId} — 소속 회사 연결

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `UUID` | 대상 회원 ID |
| `companyId` | `UUID` | 연결할 회사 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": null
}
```

> company-service가 회사 생성 후 COMPANY_MANAGER의 `companyId`를 설정할 때 사용합니다.

### DELETE /internal/v1/users/{userId} — 소속 회사 해제

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `UUID` | 대상 회원 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": null
}
```

> company-service가 회원과 회사 연결을 끊을 때 사용합니다. 회원 자체는 삭제되지 않습니다.

### DELETE /internal/v1/users/companies/{companyId} — 회사 전체 회원 소속 해제

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `companyId` | `UUID` | 대상 회사 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": null
}
```

> company-service가 회사를 삭제할 때, 해당 회사에 속한 모든 회원의 `companyId`를 일괄 해제합니다.

---

## 에러 코드

| 코드 | HTTP | 설명 |
| --- | --- | --- |
| `USER-001` | `404` | 사용자를 찾을 수 없습니다 |
| `USER-002` | `409` | 이미 존재하는 사용자입니다 (username·email·사업자등록번호 중복) |
| `USER-003` | `403` | 승인되지 않은 사용자입니다 |
| `USER-004` | `400` | 승인할 수 없는 상태입니다 (PENDING이 아닌 경우) |
| `USER-005` | `409` | 이미 사용 중인 Slack ID입니다 |
| `USER-900` | `500` | Keycloak 계정 생성 실패 |

---

## 인증 구조

게이트웨이가 JWT를 검증한 뒤 아래 헤더를 주입합니다. 각 서비스는 JWT를 별도로 파싱하지 않고 헤더를 신뢰합니다.

| 헤더 | 설명 |
| --- | --- |
| `X-User-Id` | 인증된 사용자 UUID |
| `X-User-Role` | 사용자 권한 (`MASTER` 등) |
| `X-User-Status` | 승인 상태 |
| `X-Username` | 로그인 ID |

`GatewayHeaderAuthFilter` (`common` 모듈)가 헤더를 읽어 Spring Security `SecurityContext`에 세팅하고, `@PreAuthorize`로 권한을 검증합니다.

Security 기본 설정은 `common`의 `CommonSecurityAutoConfiguration`이 자동 등록합니다. user-service는 `/api/v1/auth/**` 공개 경로가 추가로 필요하므로 자체 `SecurityConfig`를 정의합니다.

---

## 모듈 구조

```
services/user-service/src/main/java/com/sparta/whereismyparcel/user/
├── application/
│   └── service/
│       └── UserService.java
├── domain/
│   ├── UserRole.java               ← domain 레벨 enum (entity 아님)
│   ├── UserStatus.java             ← domain 레벨 enum (entity 아님)
│   ├── entity/
│   │   └── User.java
│   ├── exception/
│   │   ├── UserErrorCode.java
│   │   ├── UserNotFoundException.java
│   │   ├── UserAlreadyExistsException.java
│   │   ├── SlackIdAlreadyExistsException.java
│   │   ├── InvalidApprovalStatusException.java
│   │   ├── UserNotApprovedException.java
│   │   ├── ForbiddenException.java
│   │   └── KeycloakUserCreationFailedException.java
│   └── repository/
│       └── UserRepository.java
├── infrastructure/
│   ├── config/
│   │   ├── KeycloakConfig.java
│   │   └── SecurityConfig.java
│   └── keycloak/
│       └── KeycloakAdminService.java
└── presentation/
    ├── controller/
    │   ├── UserController.java
    │   └── InternalUserController.java
    └── dto/
        ├── request/
        │   ├── SignupRequest.java
        │   └── UpdateUserRequest.java
        └── response/
            ├── SignupResponse.java
            ├── ApproveResponse.java
            ├── UserResponse.java
            └── InternalUserResponse.java
```

---

## 의존성

```groovy
implementation project(':common')
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
implementation 'org.springframework.cloud:spring-cloud-starter-config'
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9'
implementation 'org.keycloak:keycloak-admin-client:26.0.0'
```

---

## 로컬 실행

**1. 인프라 먼저 기동 (postgres + keycloak)**

```bash
cd infra
docker compose up -d postgres keycloak
```

keycloak `healthy` 상태가 될 때까지 대기합니다.

**2. user-service 실행**

```bash
./gradlew :services:user-service:bootRun
```

Eureka, Config Server 없이도 단독 실행 가능합니다 (`application.yaml` 기본값 적용).

단, Eureka URL이 Docker용 호스트명(`eureka-server`)으로 설정된 경우 IntelliJ Run Configuration에서 환경변수를 수정해야 합니다.

```
EUREKA_URL=http://localhost:8761/eureka/
```

**3. Swagger UI**

```
http://localhost:8081/swagger-ui/index.html
```

인증이 필요한 API를 테스트할 때는 **Authorize** 버튼을 클릭한 뒤 아래 값을 입력합니다.

| 헤더 | 예시 값 |
| --- | --- |
| `X-User-Id` | `00000000-0000-0000-0000-000000000001` |
| `X-User-Role` | `MASTER` |
| `X-User-Status` | `APPROVED` |

본인 조회(`GET /api/v1/users/{userId}`) 테스트 시 `X-User-Id`를 조회할 `userId`와 동일하게 설정합니다.

**4. 게이트웨이를 통한 테스트 (포트 8000)**

eureka-server → user-service → api-gateway 순으로 기동한 뒤, Keycloak에서 토큰을 발급받아 Swagger Authorize에 Bearer 토큰으로 입력합니다.

```bash
curl -X POST http://localhost:8080/realms/sparta-logistics/protocol/openid-connect/token \
  -d "grant_type=password&client_id=<client_id>&username=<username>&password=<password>"
```
