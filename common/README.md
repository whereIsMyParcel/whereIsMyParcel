# Common Module

`common`은 MSA 서비스들이 함께 사용하는 전역 공통 라이브러리입니다.

초기 단계에서는 구조가 과하게 복잡해지는 것을 피하기 위해 `common-core`, `common-web`, `common-jpa`로 나누지 않고 `common` 단일 모듈로 관리합니다. 공통 코드가 커지거나 Gateway, JPA, Security 의존성 분리가 실제로 필요해지면 그때 다시 모듈을 나누는 방향으로 확장합니다.

전체 멀티모듈 프로젝트 구조는 [Project Structure](../docs/project-structure.md)를 참고합니다.

## 모듈 구조

```text
common/
├── build.gradle
└── src/
    └── main/
        ├── java/
        │   └── com/sparta/whereismyparcel/common/
        │       ├── config/
        │       ├── entity/
        │       ├── exception/
        │       ├── response/
        │       ├── security/
        │       └── util/
        └── resources/
            └── META-INF/spring/
```

## 패키지 역할

| 패키지 | 역할 | 주요 클래스 |
| --- | --- | --- |
| `common.response` | API 응답 포맷 | `ApiResponse` |
| `common.exception` | 공통 예외 계약과 전역 예외 처리 | `ErrorCode`, `CommonErrorCode`, `BusinessException`, `GlobalExceptionHandler` |
| `common.entity` | JPA 엔티티 공통 필드 | `BaseEntity` |
| `common.security` | 게이트웨이 헤더 기반 인증 필터 | `GatewayHeaderAuthFilter` |
| `common.util` | 공통 유틸리티 | `PageableUtils` |
| `common.config` | 공통 Auto Configuration | `CommonWebAutoConfiguration`, `CommonJpaAutoConfiguration`, `CommonSecurityAutoConfiguration`, `CommonSwaggerAutoConfiguration` |

## Gradle 사용 예시

공통 기능이 필요한 서비스 모듈에서 아래처럼 의존성을 추가합니다.

```groovy
dependencies {
    implementation project(':common')
}
```

현재는 단일 `common` 모듈이기 때문에 Web, JPA, Security 관련 공통 코드가 같은 모듈 안에 있습니다. 초기 개발 속도와 팀 이해도를 우선한 선택이며, 추후 다음 상황이 생기면 모듈 분리를 다시 검토합니다.

| 분리 검토 상황 | 이유 |
| --- | --- |
| Gateway에서 Servlet/JPA 의존성이 부담되는 경우 | Spring Cloud Gateway는 WebFlux 기반 |
| 공통 코드 양이 많아지는 경우 | 책임별 모듈 경계가 필요 |
| Security 공통 설정이 확정되는 경우 | 인증/인가 의존성은 영향 범위가 큼 |

---

## 공통 응답

성공과 실패 모두 같은 최상위 구조를 사용합니다.

```json
{
  "success": true,
  "status": 201,
  "errorCode": null,
  "message": "Created",
  "data": {
    "orderId": "8d5f7a0d-2f2d-4f9d-b53b-6f8cb4d4a111"
  }
}
```

```json
{
  "success": false,
  "status": 403,
  "errorCode": "ORDER-001",
  "message": "해당 주문에 접근할 권한이 없습니다.",
  "data": null
}
```

Validation 실패처럼 상세 정보가 필요한 경우에는 `data`에 필드 오류 목록을 담습니다.

```json
{
  "success": false,
  "status": 400,
  "errorCode": "COMMON-001",
  "message": "잘못된 입력값입니다.",
  "data": [
    {
      "field": "email",
      "value": "wrong-email",
      "reason": "이메일 형식이 올바르지 않습니다."
    }
  ]
}
```

---

## 에러 코드 작성 기준

공통 에러 코드는 `CommonErrorCode`에서 관리하고, 서비스 전용 에러 코드는 각 서비스 모듈에서 `ErrorCode`를 구현하는 enum으로 관리합니다.

```java
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

---

## 전역 예외 처리

`GlobalExceptionHandler`는 다음 예외를 공통 응답으로 변환합니다.

| 예외 | 응답 |
| --- | --- |
| `BusinessException` | 예외가 가진 `ErrorCode` 기준 응답 |
| `MethodArgumentNotValidException` | 요청 body validation 실패 |
| `ConstraintViolationException` | 요청 parameter/path validation 실패 |
| `HttpMessageNotReadableException` | JSON 파싱 실패 등 잘못된 요청 body |
| `HttpRequestMethodNotSupportedException` | 지원하지 않는 HTTP method |
| `Exception` | 알 수 없는 서버 오류 |

공통 예외 핸들러는 Auto Configuration으로 등록됩니다. 서비스가 `common`을 의존하고 Servlet MVC 환경이면 `GlobalExceptionHandler`가 자동 등록됩니다.

비활성화가 필요하면 서비스 `application.yaml`에서 설정합니다.

```yaml
whereismyparcel:
  common:
    web:
      exception:
        enabled: false
```

---

## BaseEntity

`BaseEntity`는 JPA 엔티티에서 공통으로 사용하는 Audit 필드를 제공합니다.

```java
public class User extends BaseEntity {
    // service-specific fields
}
```

| 필드 | 설명 |
| --- | --- |
| `createdAt` | 엔티티 생성 시간 (자동) |
| `updatedAt` | 엔티티 마지막 수정 시간 (자동) |
| `createdBy` | 생성자 username (`X-Username` 헤더 값, 요청 없으면 `SYSTEM`) |
| `updatedBy` | 수정자 username (`X-Username` 헤더 값, 요청 없으면 `SYSTEM`) |
| `deletedAt` | 논리 삭제 시간 |
| `deletedBy` | 논리 삭제자 username |

`CommonJpaAutoConfiguration`을 통해 JPA Auditing 설정이 함께 제공됩니다. `AuditorAware`가 요청의 `X-Username` 헤더를 읽어 `createdBy`·`updatedBy`를 자동으로 채웁니다.

---

## 보안 — GatewayHeaderAuthFilter

API Gateway가 JWT를 검증한 뒤 내부 서비스로 아래 헤더를 주입합니다.

| 헤더 | 타입 | 설명 |
| --- | --- | --- |
| `X-User-Id` | `String` (UUID) | 인증된 사용자 UUID |
| `X-User-Role` | `String` | 사용자 권한 (`MASTER`, `HUB_MANAGER` 등) |
| `X-User-Status` | `String` | 승인 상태 (`APPROVED` 등) |
| `X-Username` | `String` | 로그인 ID |

`GatewayHeaderAuthFilter`는 이 헤더를 읽어 Spring Security `SecurityContext`에 세팅합니다. 각 서비스는 JWT를 직접 파싱하지 않고 헤더를 신뢰하며, `@PreAuthorize`로 선언적으로 권한을 검증합니다.

```java
@PreAuthorize("hasRole('MASTER')")
@PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
@PreAuthorize("hasRole('MASTER') or authentication.name == #userId.toString()")
```

---

## CommonSecurityAutoConfiguration

Servlet MVC 서비스에 공통 Spring Security 설정을 자동 등록합니다.

**기본 허용 경로**: `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`

**동작 방식**: `@ConditionalOnMissingBean(SecurityFilterChain.class)`

- 서비스에 `SecurityFilterChain` 빈이 없으면 → common 빈 자동 등록
- 서비스에 `SecurityFilterChain` 빈이 있으면 → common 빈 무시

추가 공개 경로가 필요 없는 서비스(hub, order, shipment 등)는 `SecurityConfig`를 별도로 작성하지 않아도 됩니다.

```
hub-service  → SecurityConfig 없음 → common 빈 자동 등록
user-service → SecurityConfig 있음 → common 빈 무시, 서비스 빈 사용
```

추가 경로가 필요한 서비스는 직접 빈을 정의합니다.

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new GatewayHeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

---

## CommonSwaggerAutoConfiguration

Servlet MVC 서비스의 Swagger UI에 게이트웨이 헤더 인증 스키마를 자동 등록합니다.

**동작 방식**: `@ConditionalOnMissingBean(OpenAPI.class)`

springdoc 의존성만 있으면 Swagger UI에 **Authorize 버튼**이 자동 생성됩니다.

```
X-User-Id:     {사용자 UUID}
X-User-Role:   MASTER
X-User-Status: APPROVED
X-Username:    user01
```

서비스가 직접 `OpenAPI` 빈을 정의하면 common 빈은 무시됩니다.

> 게이트웨이의 `SwaggerConfig`는 `/v3/api-docs/{service}` 라우팅 전용이며 이 AutoConfiguration과 별개입니다.

---

## PageableUtils

목록 조회 API에서 페이지 크기와 정렬 필드를 강제하는 유틸리티입니다.

**페이지 크기**: 10·30·50만 허용, 이외 값은 10으로 고정

```java
private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt");
private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

@GetMapping
public ResponseEntity<?> getList(Pageable pageable) {
    Pageable normalized = PageableUtils.normalize(pageable, ALLOWED_SORT_FIELDS, DEFAULT_SORT);
    return ResponseEntity.ok(service.getList(normalized));
}
```

각 서비스 컨트롤러는 허용할 정렬 필드와 기본 정렬만 선언하고, 페이지 크기 정책은 `PageableUtils`가 일괄 적용합니다.

---

## CommonFeignAutoConfiguration

OpenFeign이 classpath에 있는 Servlet MVC 서비스에 공통 Feign 설정을 자동 등록합니다.

### FeignHeaderPropagationInterceptor

모든 Feign 요청에 게이트웨이 헤더를 자동으로 전파합니다. 서비스에서 `@RequestHeader`로 헤더를 직접 선언할 필요가 없습니다.

| 전파 헤더 |
|---|
| `X-User-Id` |
| `X-Username` |
| `X-User-Role` |
| `X-User-Status` |

```java
// ✅ 헤더 선언 불필요 — 자동 전파
@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/internal/v1/users/{userId}")
    ApiResponse<InternalUserResponse> getUser(@PathVariable UUID userId);
}
```

### CommonFeignErrorDecoder

Feign 오류 응답 body를 `ApiResponse`로 파싱해 `RemoteServiceException`으로 변환합니다.
`RemoteServiceException`은 `BusinessException`을 상속하므로 `GlobalExceptionHandler`가 자동으로 처리합니다.

| 상황 | 에러 코드 |
|---|---|
| 응답 body 파싱 성공 | 원격 서비스의 에러 코드를 그대로 전달 |
| 응답 body 없음 | `COMMON-998` |
| 응답 body 파싱 실패 | `COMMON-999` |

---

## 공통 코드 추가 기준

`common`에는 모든 서비스가 사용해도 도메인 결합이 생기지 않는 코드만 추가합니다.

| 추가해도 좋은 예시 | 추가하지 않는 것이 좋은 예시 |
| --- | --- |
| 공통 응답 포맷 | 특정 서비스 전용 DTO |
| 공통 예외 기반 클래스 | 특정 서비스 전용 ErrorCode |
| 공통 validation 예외 처리 | Gateway 전용 필터 |
| JPA Auditing BaseEntity | 서비스 간 Feign Client DTO |
| 게이트웨이 헤더 인증 필터 | 특정 서비스 도메인 enum |
| 페이지네이션 정책 유틸리티 | |
