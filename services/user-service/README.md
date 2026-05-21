# User Service

`user-service`는 회원 가입·승인·거절 및 Keycloak 계정 라이프사이클을 관리하는 서비스입니다.

전체 멀티모듈 프로젝트 구조는 [Project Structure](../../docs/project-structure.md)를 참고합니다.

## 구현 현황

| 기능 | 상태 |
| --- | --- |
| 회원가입 `POST /api/v1/auth/signup` | ✅ 완료 |
| 회원 승인 `PATCH /api/v1/users/{userId}/approve` | ✅ 완료 |
| 회원 거절 `PATCH /api/v1/users/{userId}/reject` | ✅ 완료 |
| 회원 조회 `GET /api/v1/users/{userId}` | 구현 예정 |
| 회원 목록 `GET /api/v1/users` | 구현 예정 |
| 회원 삭제 `DELETE /api/v1/users/{userId}` | 구현 예정 |

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
| `slackId` | `String` | ✅ | Slack 사용자 ID |
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

**Path Variable**

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

**Path Variable**

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

## 에러 코드

| 코드 | HTTP | 설명 |
| --- | --- | --- |
| `USER-001` | `404` | 사용자를 찾을 수 없습니다 |
| `USER-002` | `409` | 이미 존재하는 사용자입니다 (username·email·사업자등록번호 중복) |
| `USER-003` | `403` | 승인되지 않은 사용자입니다 |
| `USER-004` | `400` | 승인할 수 없는 상태입니다 (PENDING이 아닌 경우) |
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

---

## 모듈 구조

```
services/user-service/src/main/java/com/sparta/whereismyparcel/user/
├── application/
│   └── service/
│       └── UserService.java
├── domain/
│   ├── entity/
│   │   ├── User.java
│   │   ├── UserRole.java
│   │   └── UserStatus.java
│   ├── exception/
│   │   ├── UserErrorCode.java
│   │   ├── UserNotFoundException.java
│   │   ├── UserAlreadyExistsException.java
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
    │   └── UserController.java
    └── dto/
        ├── request/
        │   └── SignupRequest.java
        └── response/
            ├── SignupResponse.java
            └── ApproveResponse.java
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
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6'
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

**3. Swagger UI**

```
http://localhost:8081/swagger-ui/index.html
```
