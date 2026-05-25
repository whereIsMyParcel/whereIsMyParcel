# 프로젝트 구조

---

## 디렉토리 구조

```
whereIsMyParcel/
├── build.gradle              # 전체 모듈 공통 Gradle 설정
├── settings.gradle           # 모듈 등록
├── common/                   # 공통 라이브러리
├── services/
│   ├── eureka-server/
│   ├── config-server/
│   ├── api-gateway/
│   ├── user-service/
│   ├── hub-service/
│   ├── company-service/
│   ├── order-service/
│   ├── shipment-service/
│   └── ai-slack-service/
├── infra/
│   ├── docker-compose.yml
│   ├── keycloak/             # realm-export.json
│   └── postgres/             # init.sh (스키마 초기화)
└── docs/
```

> `infra/redis/`는 존재하지 않습니다. Redis는 비밀번호 설정만 docker-compose 커맨드로 처리합니다.
> `config-hub/`는 **별도 Git 저장소**로 관리합니다. 이 레포에 포함하지 않습니다.

---

## 서비스별 패키지

```
com.sparta.whereismyparcel.{서비스명}
```

| 모듈 | 패키지 |
|------|--------|
| common | `com.sparta.whereismyparcel.common` |
| api-gateway | `com.sparta.whereismyparcel.gateway` |
| user-service | `com.sparta.whereismyparcel.user` |
| hub-service | `com.sparta.whereismyparcel.hub` |
| company-service | `com.sparta.whereismyparcel.company` |
| order-service | `com.sparta.whereismyparcel.order` |
| shipment-service | `com.sparta.whereismyparcel.shipment` |
| ai-slack-service | `com.sparta.whereismyparcel.aislack` |

---

## 새 서비스 모듈 추가 시

1. `services/{service-name}/` 디렉토리 생성
2. `settings.gradle`에 `include 'services:{service-name}'` 추가
3. `build.gradle`, `Dockerfile`, `Application.java`, `application.yaml` 작성
4. config-hub 저장소에 `{service-name}.yml` 추가
5. `docker-compose.yml`에 서비스 블록 추가

---

## Gradle 구조

루트 `build.gradle`이 Java 버전, Spring Boot/Cloud 버전, 공통 의존성을 관리합니다. 각 서비스는 플러그인 버전 없이 `id 'org.springframework.boot'`만 선언하면 루트 설정을 상속합니다.

```groovy
// 각 서비스 build.gradle 최소 구조
plugins {
    id 'org.springframework.boot'
}

dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    ...
}
```

---

## config-hub 저장소

Config Server가 읽는 중앙 설정 저장소입니다. 백엔드 레포와 별도 Git 저장소로 관리합니다.

```
whereIsMyParcel-config-hub/
├── application.yml       # 모든 서비스 공통 (Eureka, Zipkin, Actuator)
├── api-gateway.yml
├── user-service.yml
├── hub-service.yml
├── company-service.yml
├── order-service.yml
├── shipment-service.yml
└── ai-slack-service.yml
```

서비스는 `spring.application.name` 기준으로 파일을 받습니다.

---

## common 모듈

여러 서비스가 공통으로 사용하는 라이브러리입니다.

```
common/src/main/java/com/sparta/whereismyparcel/common/
├── config/       # CommonJpaAutoConfiguration, CommonSecurityAutoConfiguration, CommonSwaggerAutoConfiguration, CommonFeignAutoConfiguration
├── entity/       # BaseEntity
├── exception/    # BusinessException, ErrorCode, RemoteServiceException
├── feign/        # FeignHeaderPropagationInterceptor, CommonFeignErrorDecoder
├── response/     # ApiResponse
├── security/     # GatewayHeaderAuthFilter
└── util/         # PageableUtils
```

- `CommonSecurityAutoConfiguration`: `@EnableWebSecurity`·`@EnableMethodSecurity` 및 기본 `SecurityFilterChain` 자동 등록. 서비스가 자체 `SecurityFilterChain`을 정의하면 기본 FilterChain은 등록되지 않음 (`@ConditionalOnMissingBean`)
- `CommonSwaggerAutoConfiguration`: Swagger X-User-* 헤더 인증 스킴 자동 등록
- `CommonFeignAutoConfiguration`: Feign classpath에 있는 서비스에 헤더 전파(`FeignHeaderPropagationInterceptor`)와 에러 디코더(`CommonFeignErrorDecoder`) 자동 등록
- `PageableUtils.normalize()`: 목록 API 공통 페이지 크기(10·30·50) 및 정렬 필드 검증 유틸

서비스 간 도메인 엔티티(User, Order 등)는 common에 두지 않습니다. 각 서비스 내부에 위치합니다.
