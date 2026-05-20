# 개발 컨벤션

이 문서는 팀원 전원이 동일한 기준으로 코드를 작성하기 위한 컨벤션입니다.
**코드 리뷰 기준이 되므로 모든 팀원이 숙지 후 개발을 시작해야 합니다.**

---

## 목차

1. [Git 브랜치 전략](#1-git-브랜치-전략)
2. [커밋 메시지](#2-커밋-메시지)
3. [PR 규칙](#3-pr-규칙)
4. [패키지 구조](#4-패키지-구조)
5. [네이밍 컨벤션](#5-네이밍-컨벤션)
6. [API 응답 형식](#6-api-응답-형식)
7. [에러 코드 관리](#7-에러-코드-관리)
8. [예외 처리](#8-예외-처리)
9. [Gateway 헤더 스펙](#9-gateway-헤더-스펙)
10. [DB 컨벤션](#10-db-컨벤션)
11. [서비스 구현 규칙](#11-서비스-구현-규칙)
12. [서비스 간 통신](#12-서비스-간-통신)

---

## 1. Git 브랜치 전략

```
main          ← 배포 브랜치. 직접 push 금지
develop       ← 통합 브랜치. feature 브랜치를 여기로 merge
feature/{...} ← 기능 개발 브랜치
hotfix/{...}  ← 긴급 수정 브랜치 (main에서 분기)
```

### 브랜치 네이밍

```
feature/user-signup
feature/hub-crud
feature/gateway-jwt-filter
hotfix/user-auth-null-pointer
```

### 규칙

- `main`에 직접 push 금지. PR만 허용
- `develop` → `main` merge 
- 기능 단위로 브랜치를 나눕니다. 하나의 브랜치에 여러 기능을 섞지 않습니다
- 브랜치 이름은 kebab-case, 소문자

---

## 2. 커밋 메시지

### 형식

```
<type>: <subject>

[optional body]
```

### type 목록

| type | 설명 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없이 코드 구조 개선 |
| `chore` | 빌드 설정, 의존성, 설정 파일 변경 |
| `docs` | 문서 작성/수정 |
| `test` | 테스트 코드 추가/수정 |
| `style` | 코드 포맷, 세미콜론 등 로직 변화 없는 스타일 변경 |

### 예시

```
feat: 회원가입 API 구현

feat: 가입 승인/거절 API 구현
fix: 승인 시 Keycloak 호출 NPE 수정
chore: user-service build.gradle 의존성 추가
docs: user-service API 명세 업데이트
refactor: UserService 트랜잭션 범위 분리
```

### 규칙

- 제목은 50자 이내, 마침표 없음
- 현재형으로 작성 (`구현`, `수정`, `추가`)
- 본문은 필요할 때만. `왜` 변경했는지를 씁니다

---

## 3. PR 규칙

### PR 제목

커밋 메시지와 동일한 형식을 사용합니다.

```
feat: 회원가입 API 구현
feat: hub-service CRUD 전체 구현
fix: 배송 상태 업데이트 동시성 오류 수정
```

### PR 작성 기준

- PR 하나는 기능 하나. 리뷰어가 30분 내에 리뷰할 수 있는 크기를 유지합니다
- `develop` 브랜치로 PR을 올립니다
- 셀프 머지 금지. 최소 2명의 Approve 후 머지합니다
- 리뷰 요청 후 24시간 내 리뷰를 목표로 합니다

---

## 4. 패키지 구조

모든 서비스는 아래 구조를 따릅니다. 패키지 루트는 `com.sparta.whereismyparcel.{service-name}` 입니다.

```
com.sparta.whereismyparcel.{service-name}
├── application/
│   └── service/          ← 비즈니스 로직
├── domain/
│   ├── entity/           ← JPA 엔티티
│   └── repository/       ← JPA Repository 인터페이스
├── infrastructure/
│   ├── client/           ← FeignClient 인터페이스
│   └── config/           ← 설정 클래스 (@Configuration)
└── presentation/
    ├── controller/       ← REST Controller
    └── dto/
        ├── request/      ← 요청 DTO
        └── response/     ← 응답 DTO
```

### 서비스별 패키지 루트

| 서비스 | 패키지 루트 |
| --- | --- |
| `user-service` | `com.sparta.whereismyparcel.user` |
| `hub-service` | `com.sparta.whereismyparcel.hub` |
| `company-service` | `com.sparta.whereismyparcel.company` |
| `order-service` | `com.sparta.whereismyparcel.order` |
| `shipment-service` | `com.sparta.whereismyparcel.shipment` |
| `ai-slack-service` | `com.sparta.whereismyparcel.aislack` |

---

## 5. 네이밍 컨벤션

### 기본 규칙

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 클래스 | PascalCase | `UserService`, `SignupRequest` |
| 메서드/변수 | camelCase | `getUserById`, `userId` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 패키지 | 소문자, 단수 | `controller`, `entity` |
| DB 테이블 | `p_` + snake_case | `p_users`, `p_hub_routes` |
| DB 컬럼 | snake_case | `created_at`, `hub_id` |

### 클래스 네이밍

```
User                 ← 엔티티 (도메인 이름 그대로)
UserRepository       ← 레포지토리 (Repository 접미사)
UserService          ← 서비스 (Service 접미사)
UserController       ← 컨트롤러 (Controller 접미사)
SignupRequest        ← 요청 DTO (Request 접미사)
SignupResponse       ← 응답 DTO (Response 접미사)
UserErrorCode        ← 서비스별 ErrorCode (ErrorCode 접미사)
UserFeignClient      ← Feign 클라이언트 (FeignClient 접미사)
```

## 6. API 응답 형식

모든 응답은 `common` 모듈의 `ApiResponse<T>`를 사용합니다. **직접 ResponseEntity를 반환하거나 Map을 반환하지 않습니다.**

### 성공 응답

```java
// 200 OK (데이터 있음)
return ResponseEntity.ok(ApiResponse.success(data));

// 200 OK (데이터 없음)
return ResponseEntity.ok(ApiResponse.ok());

// 201 Created
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
```

### 실패 응답

실패는 구체적인 예외 클래스를 throw합니다. `GlobalExceptionHandler`가 `BusinessException`을 잡아서 자동으로 `ApiResponse`로 변환합니다.

```java
// ✅ 구체적인 예외 클래스 사용
throw new UserNotFoundException();
throw new UserAlreadyExistsException();
```

### 응답 JSON 구조

**성공**
```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": { ... }
}
```

**실패**
```json
{
  "success": false,
  "status": 404,
  "errorCode": "USER-001",
  "message": "사용자를 찾을 수 없습니다.",
  "data": null
}
```

**Validation 실패**
```json
{
  "success": false,
  "status": 400,
  "errorCode": "COMMON-001",
  "message": "잘못된 입력값입니다.",
  "data": [
    { "field": "username", "value": "a", "reason": "4자 이상 입력해야 합니다." }
  ]
}
```

---

## 7. 에러 코드 관리

### 코드 형식

```
{서비스-약어}-{3자리 숫자}
```

| 서비스 | 접두어 | 예시 |
| --- | --- | --- |
| common | `COMMON` | `COMMON-001` |
| user-service | `USER` | `USER-001` |
| hub-service | `HUB` | `HUB-001` |
| company-service | `COMPANY` | `COMPANY-001` |
| order-service | `ORDER` | `ORDER-001` |
| shipment-service | `SHIPMENT` | `SHIPMENT-001` |
| ai-slack-service | `AI` | `AI-001` |

### 에러 코드 파일 위치

각 서비스의 `domain/exception/` 패키지에 ErrorCode와 구체적인 예외 클래스를 함께 둡니다.

```java
// user-service 예시
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER-002", "이미 존재하는 사용자입니다."),
    USER_NOT_APPROVED(HttpStatus.FORBIDDEN, "USER-003", "승인되지 않은 사용자입니다."),
    INVALID_APPROVAL_STATUS(HttpStatus.BAD_REQUEST, "USER-004", "승인할 수 없는 상태입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

### 번호 할당 규칙

- `001~099`: 조회 관련 (Not Found, 권한 없음)
- `100~199`: 생성/수정 관련 (중복, 유효하지 않은 상태)
- `900~999`: 외부 연동 오류 (Keycloak, Feign 호출 실패)

---

## 8. 예외 처리

### 기본 구조

`common` 모듈의 `BusinessException`을 상속해서 **에러 코드마다 구체적인 예외 클래스**를 만듭니다.
`GlobalExceptionHandler`가 `BusinessException`을 잡아서 `ApiResponse`로 변환하므로, 서비스 코드에서는 throw만 합니다.

### 예외 클래스 작성 방법

```java
// 각 서비스의 exception/ 패키지에 작성
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}

public class UserAlreadyExistsException extends BusinessException {
    public UserAlreadyExistsException() {
        super(UserErrorCode.USER_ALREADY_EXISTS);
    }
}

public class InvalidApprovalStatusException extends BusinessException {
    public InvalidApprovalStatusException() {
        super(UserErrorCode.INVALID_APPROVAL_STATUS);
    }
}
```

### throw 방법

```java
// ❌ 잘못된 예 — RuntimeException 직접 사용
throw new RuntimeException("사용자를 찾을 수 없습니다.");

// ❌ 잘못된 예 — BusinessException에 ErrorCode 직접 넘기기
throw new BusinessException(UserErrorCode.USER_NOT_FOUND);

// ✅ 올바른 예 — 구체적인 예외 클래스 사용
throw new UserNotFoundException();
throw new InvalidApprovalStatusException();
```

### 예외 클래스 파일 위치

ErrorCode와 예외 클래스는 `domain/exception/` 한 곳에 모읍니다.

```
services/user-service/src/main/java/com/sparta/whereismyparcel/user/
└── domain/
    └── exception/
        ├── UserErrorCode.java
        ├── UserNotFoundException.java
        ├── UserAlreadyExistsException.java
        └── InvalidApprovalStatusException.java
```

### 규칙 요약

- `BusinessException` 직접 throw 금지. 반드시 구체적인 서브클래스를 만들어서 사용
- `try-catch`로 예외를 잡아서 응답을 직접 만들지 않습니다 (`GlobalExceptionHandler`가 처리)
- `RuntimeException` 직접 throw 금지
- Feign 호출 실패는 `FeignErrorDecoder`에서 구체적인 예외 클래스로 변환합니다

### 권한 체크

각 서비스는 `X-User-Role` 헤더를 읽어서 도메인 권한을 직접 검증합니다. JWT를 파싱하지 않습니다.

```java
@PatchMapping("/{userId}/approve")
public ResponseEntity<ApiResponse<ApproveResponse>> approve(
    @RequestHeader("X-User-Role") String role,
    @RequestHeader("X-User-Id") String requesterId,
    @PathVariable UUID userId,
    @RequestBody @Valid ApproveRequest request
) {
    if (!role.equals("MASTER") && !role.equals("HUB_MANAGER")) {
        throw new ForbiddenException();  // CommonErrorCode.FORBIDDEN을 감싼 예외 클래스
    }
    ...
}
```

---

## 9. Gateway 헤더 스펙

Gateway가 JWT 검증 후 각 서비스로 주입하는 헤더입니다.
**각 서비스는 이 헤더를 신뢰하고, JWT를 별도로 파싱하지 않습니다.**

| 헤더 | 타입 | 설명 | 예시 |
| --- | --- | --- | --- |
| `X-User-Id` | `String` (UUID) | 인증된 사용자 UUID (Keycloak sub) | `6f7d9c5e-7fa3-4c87-bfb7-f3fa20d72e7d` |
| `X-Username` | `String` | 로그인 ID | `user01` |
| `X-User-Role` | `String` | 현재 사용자 권한 | `MASTER`, `HUB_MANAGER`, `DELIVERY_MANAGER`, `COMPANY_MANAGER` |
| `X-User-Status` | `String` | 현재 사용자 승인 상태 | `APPROVED` |

### 인증 불필요 엔드포인트 (Gateway 통과)

```
POST /api/v1/auth/signup
POST /actuator/**
```

### 서비스 간 Feign 호출 시

내부 서비스 호출 시에도 동일한 헤더를 전달합니다.

```java
@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/internal/v1/users/{userId}/auth-context")
    ApiResponse<AuthContextResponse> getAuthContext(
        @RequestHeader("X-User-Id") String userId,
        @PathVariable("userId") UUID targetUserId
    );
}
```

---

## 10. DB 컨벤션

### 테이블 네이밍

- 모든 테이블은 `p_` 접두사 + snake_case 복수형
- `p_users`, `p_hubs`, `p_hub_routes`, `p_shipments`

### PK

- 모든 PK는 `UUID`, 컬럼명은 `{테이블단수명}_id`
- PostgreSQL `gen_random_uuid()` 기본값 사용
- `user_id`, `hub_id`, `shipment_id`

### Audit 필드 (6개 필수)

모든 테이블에 아래 6개 필드를 포함합니다. `BaseEntity`를 상속하면 자동으로 채워집니다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `created_at` | `TIMESTAMP NOT NULL` | 생성 일시 (자동) |
| `created_by` | `VARCHAR(100)` | 생성자 ID |
| `updated_at` | `TIMESTAMP` | 수정 일시 (자동) |
| `updated_by` | `VARCHAR(100)` | 수정자 ID |
| `deleted_at` | `TIMESTAMP` | 논리 삭제 일시 |
| `deleted_by` | `VARCHAR(100)` | 논리 삭제자 ID |

### Soft Delete

- 물리 삭제(`DELETE` SQL) 금지
- 삭제 시 `deleted_at = now()`, `deleted_by = {요청자 ID}` 세팅
- 모든 엔티티에 `@SQLRestriction("deleted_at IS NULL")` 선언 필수 (Hibernate 6.x)
- JpaRepository의 기본 메서드(`findById`, `findAll` 등) 사용 시 자동으로 삭제되지 않은 데이터만 조회됩니다.
- **주의**: Native Query 사용 시에는 직접 `deleted_at IS NULL` 조건을 추가해야 합니다.

```java
// ✅ 올바른 예 (전역 필터 적용으로 접미사 불필요)
userRepository.findById(userId)
    .orElseThrow(UserNotFoundException::new);
```

### 스키마 분리

각 서비스는 자신의 스키마만 접근합니다. 다른 서비스의 테이블에 직접 쿼리하지 않습니다.

```yaml
# application.yaml 예시 (user-service)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sparta_logistics?currentSchema=user_db
```

---

## 11. 서비스 구현 규칙

### 레이어 책임

| 레이어 | 책임 | 금지 사항 |
| --- | --- | --- |
| `Controller` | HTTP 요청/응답, 헤더 추출, 권한 체크 | 비즈니스 로직, DTO 변환 |
| `Service` | 비즈니스 로직, DTO 변환, 트랜잭션 관리 | HTTP 관련 코드 (`HttpServletRequest` 등) |
| `Repository` | DB 접근 | 비즈니스 로직 |
| `Entity` | 도메인 상태, 상태 변경 메서드 | 외부 의존성 |

### DTO 변환 위치

DTO 변환은 **Service에서** 합니다. Controller는 요청을 받아 Service를 호출하고 응답을 반환하는 역할만 합니다.

```java
// Service — DTO 변환 후 반환
public SignupResponse signup(SignupRequest request) {
    User user = userRepository.save(...);
    return SignupResponse.from(user);
}

// Controller — 얇게 유지
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(userService.signup(request)));
```

### 트랜잭션

- `@Transactional`은 Service 메서드에 선언
- 조회 전용 메서드는 `@Transactional(readOnly = true)`
- Keycloak Admin API 호출처럼 외부 I/O가 포함된 경우, 트랜잭션 범위와 외부 호출 순서를 명확히 정의

```java
// ✅ 올바른 예
@Transactional
public ApproveResponse approve(UUID userId, UUID approverId) {
    User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
        .orElseThrow(UserNotFoundException::new);
    keycloakService.enableUser(userId);  // 외부 호출
    user.approve(approverId);            // 도메인 상태 변경 (엔티티 메서드)
    return ApproveResponse.from(user);
}
```

### 엔티티 상태 변경

상태 변경 로직은 Service가 아닌 **엔티티 메서드**로 캡슐화합니다.

```java
// ❌ Service에서 직접 세팅
user.setStatus(UserStatus.APPROVED);
user.setUpdatedBy(approverId.toString());

// ✅ 엔티티 메서드 위임
user.approve(approverId);  // 엔티티 내부에서 상태/감사 필드 처리
```

## 12. 엔티티 생성 규칙

- **외부**: 정적 팩토리 메서드 `create()` 사용 필수
- **내부**: `@Builder`를 `private` 생성자에 선언해서 `create()` 안에서만 사용 (클래스 레벨 선언 금지)
- **JPA**: `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 필수

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hub extends BaseEntity {
    // ... 필드 정의

    @Builder(access = AccessLevel.PRIVATE) // 내부 빌더
    private Hub(String name, ...) {
        this.name = name;
        // ...
    }

    public static Hub create(String name, ...) {
        return Hub.builder()
                .name(name)
                .build();
    }
}
```

## 13. 서비스 간 통신

### 규칙

- 서비스 간 통신은 `OpenFeign`만 사용합니다
- 다른 서비스의 모듈을 `implementation project(':...')` 로 의존하지 않습니다
- `common` 모듈만 각 서비스가 `implementation`으로 의존할 수 있습니다
- Feign Client는 `infrastructure/client/` 패키지에 위치합니다

### Feign 호출 실패 처리

Feign 호출 실패는 `FeignErrorDecoder`에서 구체적인 예외 클래스로 변환하여 서비스 로직에서 별도 처리가 필요 없게 합니다.

### 서비스 간 데이터 공유

다른 서비스의 데이터가 필요한 경우 UUID 식별자를 받아서 Feign으로 조회합니다. DB 직접 조회 금지.

```java
// ✅ order-service에서 company-service 재고 차감 예시
@FeignClient(name = "company-service")
public interface CompanyFeignClient {
    @PostMapping("/internal/v1/inventories/{productId}/deduct")
    ApiResponse<Void> deductStock(
        @RequestHeader("X-User-Id") String userId,
        @PathVariable UUID productId,
        @RequestBody DeductStockRequest request
    );
}
```

### 유레카 서비스명 (lb:// 라우팅 기준)

| 서비스 | `spring.application.name` |
| --- | --- |
| `user-service` | `user-service` |
| `hub-service` | `hub-service` |
| `company-service` | `company-service` |
| `order-service` | `order-service` |
| `shipment-service` | `shipment-service` |
| `ai-slack-service` | `ai-slack-service` |
