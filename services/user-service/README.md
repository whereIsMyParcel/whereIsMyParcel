# User Service

`user-service`는 DelivHub의 회원 관리 및 인증/인가를 담당하는 서비스입니다.

전체 멀티모듈 프로젝트 구조는 [Project Structure](../../docs/project-structure.md)를 참고합니다.

## 담당

| 항목 | 내용 |
| --- | --- |
| 담당자 | 재훈 |
| 포트 | `8081` |
| 서비스명 | `user-service` (Eureka 등록명) |

## 현재 상태

뼈대 구성 단계입니다. 현재는 애플리케이션 기동과 헬스 체크만 가능하며, 비즈니스 로직은 순차적으로 구현 예정입니다.

| 기능 | 상태 |
| --- | --- |
| 애플리케이션 기동 | ✅ 완료 |
| 헬스 체크 (`/actuator/health`) | ✅ 완료 |
| Eureka 등록 | ✅ 설정 완료 (Eureka 서버 기동 시 자동 등록) |
| 회원가입 / 로그인 | 구현 예정 |
| JWT 인증/인가 | 구현 예정 |
| 회원 정보 CRUD | 구현 예정 |

## 모듈 구조

```text
services/user-service/
├── build.gradle
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/sparta/whereismyparcel/user/
    │   │       └── UserServiceApplication.java
    │   └── resources/
    │       └── application.yaml
    └── test/
        └── java/
            └── com/sparta/whereismyparcel/user/
                └── UserServiceApplicationTests.java
```

## 의존성

```groovy
dependencies {
    implementation project(':common')          // 공통 응답, 예외, BaseEntity

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
}
```

`common` 모듈을 통해 `ApiResponse`, `GlobalExceptionHandler`, `BaseEntity` 등이 자동으로 적용됩니다.

## 헬스 체크

서버 기동 후 아래 엔드포인트로 상태를 확인합니다.

```
GET http://localhost:8081/actuator/health
```

```json
{
  "status": "UP"
}
```

## 로컬 실행

```bash
# 프로젝트 루트에서 실행
./gradlew :services:user-service:bootRun
```

> Eureka 서버(`localhost:8761`)가 없어도 기동은 됩니다. 단, Eureka 연결 실패 경고 로그가 출력되며 서비스 디스커버리는 동작하지 않습니다.

## 구현 예정 기능

비즈니스 로직 구현 시 아래 패키지 구조를 따릅니다.

| 패키지 | 역할 |
| --- | --- |
| `domain/user/controller` | 회원 관련 API 엔드포인트 |
| `domain/user/service` | 회원 비즈니스 로직 |
| `domain/user/repository` | DB 접근 |
| `domain/user/entity` | 회원 엔티티 (`BaseEntity` 상속) |
| `domain/auth/controller` | 로그인, 토큰 재발급 API |
| `domain/auth/service` | JWT 생성/검증 로직 |
| `common` | 서비스 전용 에러 코드 (`UserErrorCode`), 공통 유틸 |
| `config` | Security, JPA 등 설정 클래스 |

### 에러 코드 작성 예시

`common` 모듈의 `ErrorCode`를 구현하여 서비스 전용 에러 코드를 관리합니다.

```java
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT,  "USER-002", "이미 사용 중인 이메일입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```
