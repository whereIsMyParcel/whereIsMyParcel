# Project Structure

이 문서는 MSA 기반 B2B 물류 관리 및 배송 시스템의 멀티모듈 프로젝트 구조를 설명합니다. 팀원이 새 모듈을 만들거나 공통 코드를 추가할 때, 어떤 위치에 무엇을 두어야 하는지 빠르게 판단하는 것을 목표로 합니다.

## 목표 구조

초기 프로젝트는 아래 구조를 기준으로 잡습니다.

```text
b2b-logistics-platform/
├── settings.gradle
├── build.gradle
├── gradle/
├── common/
│   ├── build.gradle
│   └── src/
├── services/
│   ├── eureka-server/
│   ├── config-server/
│   ├── api-gateway/
│   └── user-service/
├── infra/
│   ├── docker-compose.yml
│   ├── keycloak/
│   ├── postgres/
│   ├── redis/
│   └── kafka/
└── docs/
```

## 현재 구조

현재는 공통 모듈을 먼저 만든 상태입니다.

```text
whereIsMyParcel/
├── settings.gradle
├── build.gradle
├── gradle/
├── common/
│   ├── build.gradle
│   └── src/
├── docs/
└── src/
```

루트의 `src/`는 초기 단일 Spring Boot 프로젝트 생성 시 만들어진 흔적입니다. 멀티모듈 MSA 구조에서는 루트 프로젝트가 직접 실행되는 애플리케이션이 아니라 하위 모듈을 묶는 aggregator 역할을 합니다. 따라서 실제 서비스 모듈이 만들어지면 루트 `src/`는 삭제하거나 적절한 서비스 모듈로 옮기는 것을 권장합니다.

## 루트 프로젝트 역할

루트 프로젝트는 직접 실행되는 Spring Boot 애플리케이션이 아닙니다.

| 역할 | 설명 |
| --- | --- |
| 모듈 등록 | `settings.gradle`에서 하위 Gradle 모듈을 등록 |
| 공통 Gradle 설정 | Java 버전, repository, dependency management 등 공통 설정 관리 |
| 버전 관리 | Spring Boot, Spring Dependency Management 등 플러그인 버전 관리 |
| 빌드 진입점 | 전체 모듈 빌드 명령의 시작점 |

루트 `build.gradle`에서 `org.springframework.boot` 플러그인에 `apply false`를 사용하는 이유는 플러그인 버전은 루트에서 관리하되, 루트 프로젝트 자체에는 Spring Boot 애플리케이션 플러그인을 적용하지 않기 위해서입니다.

실행 가능한 Spring Boot 애플리케이션은 `services/user-service`, `services/api-gateway` 같은 하위 서비스 모듈에서 담당합니다.

## Gradle 모듈 등록 기준

Java로 컴파일하거나 다른 모듈에서 의존해야 하는 코드는 Gradle 모듈로 등록합니다.

초기 등록 예시는 다음과 같습니다.

```groovy
include 'common'

include 'services:eureka-server'
include 'services:config-server'
include 'services:api-gateway'
include 'services:user-service'
```

| 위치 | 등록 이유 |
| --- | --- |
| `common` | 여러 서비스가 의존하는 공통 Java 라이브러리 |
| `services/user-service` | 실행 가능한 Spring Boot 서비스 |
| `services/api-gateway` | 실행 가능한 Spring Cloud Gateway 서비스 |
| `services/eureka-server` | 실행 가능한 Service Discovery 서버 |
| `services/config-server` | 실행 가능한 Spring Cloud Config Server |

Java 컴파일 대상이 아닌 설정, 문서, 인프라 파일은 보통 Gradle 모듈로 등록하지 않습니다.

| 위치 | 등록하지 않는 이유 |
| --- | --- |
| `config-hub` | 별도 Git repository로 관리하는 설정 파일 저장소 |
| `infra` | Docker Compose, Keycloak, PostgreSQL, Redis, Kafka 설정 저장소 |
| `docs` | 프로젝트 문서 저장소 |

## common 디렉터리

`common`은 여러 서비스가 함께 사용하는 공통 라이브러리입니다. 초기에는 팀 이해도와 개발 속도를 위해 단일 모듈로 관리합니다.

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

| 패키지 | 역할 |
| --- | --- |
| `common.response` | 공통 API 응답 포맷 |
| `common.exception` | 공통 예외 계약과 전역 예외 처리 |
| `common.entity` | JPA 공통 엔티티 필드 |
| `common.config` | 공통 Auto Configuration |

서비스 모듈에서는 필요한 경우 아래처럼 의존합니다.

```groovy
dependencies {
    implementation project(':common')
}
```

나중에 공통 코드가 커지거나 의존성 충돌이 생기면 `common-core`, `common-web`, `common-jpa`, `common-security`로 재분리할 수 있습니다. 지금은 처음 MSA를 시작하는 단계이므로 단순한 구조를 우선합니다.

## services 디렉터리

`services`는 실제 실행되는 Spring Boot 애플리케이션을 모아두는 위치입니다.

```text
services/
├── eureka-server/
├── config-server/
├── api-gateway/
└── user-service/
```

각 서비스 모듈은 독립적인 `build.gradle`, `src/main/java`, `src/main/resources`를 가집니다.

```text
services/user-service/
├── build.gradle
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/sparta/whereismyparcel/user/
    │   │       └── UserServiceApplication.java
    │   └── resources/
    │       └── application.yml
    └── test/
```

## config-hub repository

`config-hub`는 Spring Cloud Config Server가 읽는 중앙 설정 저장소입니다. 백엔드 코드와 설정 파일의 변경 흐름을 분리하기 위해 별도 Git repository로 관리하는 것을 권장합니다.

로컬 개발 시에는 백엔드 repository와 같은 상위 디렉터리에 나란히 clone해서 사용합니다.

```text
projects/
├── whereIsMyParcel-backend/
└── whereIsMyParcel-config-hub/
```

예상 파일 구조는 다음과 같습니다.

```text
whereIsMyParcel-config-hub/
├── application.yml
├── application-dev.yml
├── application-prod.yml
├── eureka-server.yml
├── eureka-server-dev.yml
├── eureka-server-prod.yml
├── config-server.yml
├── config-server-dev.yml
├── config-server-prod.yml
├── api-gateway.yml
├── api-gateway-dev.yml
├── api-gateway-prod.yml
├── user-service.yml
├── user-service-dev.yml
└── user-service-prod.yml
```

## infra 디렉터리

`infra`는 로컬 개발 및 배포 인프라 설정을 관리합니다.

```text
infra/
├── docker-compose.yml
├── keycloak/
├── postgres/
├── redis/
└── kafka/
```

## 패키지 네이밍 기준

루트 패키지는 프로젝트 공통으로 맞춥니다.

```text
com.sparta.whereismyparcel
```

common 모듈은 다음 패키지를 사용합니다.

```text
com.sparta.whereismyparcel.common.response
com.sparta.whereismyparcel.common.exception
com.sparta.whereismyparcel.common.entity
com.sparta.whereismyparcel.common.config
```

서비스 모듈은 서비스명을 기준으로 패키지를 나눕니다.

```text
com.sparta.whereismyparcel.user
com.sparta.whereismyparcel.gateway
com.sparta.whereismyparcel.eureka
com.sparta.whereismyparcel.config
```

## 정리

| 기준 | 설명 |
| --- | --- |
| 루트 프로젝트 | 실행 앱이 아니라 멀티모듈 aggregator |
| `common` | 여러 서비스가 재사용하는 단일 공통 Java 라이브러리 |
| `services` | 실제 실행되는 Spring Boot 애플리케이션 |
| `config-hub` | Config Server가 읽는 별도 Git 설정 repository |
