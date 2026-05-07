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
        │       └── response/
        └── resources/
            └── META-INF/spring/
```

## 패키지 역할

| 패키지 | 역할 | 주요 클래스 |
| --- | --- | --- |
| `common.response` | API 응답 포맷 | `ApiResponse` |
| `common.exception` | 공통 예외 계약과 전역 예외 처리 | `ErrorCode`, `CommonErrorCode`, `BusinessException`, `GlobalExceptionHandler` |
| `common.entity` | JPA 엔티티 공통 필드 | `BaseEntity` |
| `common.config` | 공통 Auto Configuration | `CommonWebAutoConfiguration`, `CommonJpaAutoConfiguration` |

## Gradle 사용 예시

공통 기능이 필요한 서비스 모듈에서 아래처럼 의존성을 추가합니다.

```groovy
dependencies {
    implementation project(':common')
}
```

현재는 단일 `common` 모듈이기 때문에 Web, JPA 관련 공통 코드가 같은 모듈 안에 있습니다. 초기 개발 속도와 팀 이해도를 우선한 선택이며, 추후 다음 상황이 생기면 `common-core`, `common-web`, `common-jpa`, `common-security` 분리를 다시 검토합니다.

| 분리 검토 상황 | 이유 |
| --- | --- |
| Gateway에서 Servlet/JPA 의존성이 부담되는 경우 | Spring Cloud Gateway는 보통 WebFlux 기반 |
| 공통 코드 양이 많아지는 경우 | 책임별 모듈 경계가 필요 |
| Security 공통 설정이 확정되는 경우 | 인증/인가 의존성은 영향 범위가 큼 |

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

## 에러 코드 작성 기준

공통 에러 코드는 `CommonErrorCode`에서 관리하고, 서비스 전용 에러 코드는 각 서비스 모듈에서 `ErrorCode`를 구현하는 enum으로 관리합니다.

```java
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

현재는 단일 `common` 모듈로 운영하므로 `ErrorCode`의 `status`는 `HttpStatus`를 사용합니다. HTTP 상태를 enum에서 바로 읽을 수 있어 팀원들이 에러 코드를 더 직관적으로 작성할 수 있습니다.

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

공통 예외 핸들러는 Spring Boot Auto Configuration으로 등록됩니다. 서비스가 `common`을 의존하고 Servlet MVC 환경이면 `GlobalExceptionHandler`가 자동 등록됩니다.

필요하면 서비스별 설정으로 비활성화할 수 있습니다.

```yaml
whereismyparcel:
  common:
    web:
      exception:
        enabled: false
```

## BaseEntity

`BaseEntity`는 JPA 엔티티에서 공통으로 사용하는 생성일시와 수정일시를 제공합니다.

```java
public class User extends BaseEntity {
    // service-specific fields
}
```

| 필드 | 설명 |
| --- | --- |
| `createdAt` | 엔티티 생성 시간 |
| `updatedAt` | 엔티티 마지막 수정 시간 |

`CommonJpaAutoConfiguration`을 통해 JPA Auditing 설정도 함께 제공합니다.

## 공통 코드 추가 기준

`common`에는 모든 서비스가 사용해도 도메인 결합이 생기지 않는 코드만 추가합니다.

| 추가해도 좋은 예시 | 추가하지 않는 것이 좋은 예시 |
| --- | --- |
| 공통 응답 포맷 | 특정 서비스 전용 DTO |
| 공통 예외 기반 클래스 | 특정 서비스 전용 ErrorCode |
| 공통 validation 예외 처리 | Gateway 전용 필터 |
| JPA Auditing BaseEntity | 서비스 간 Feign Client DTO |
