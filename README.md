# Config Server 사용 가이드

본 애플리케이션은 *Where Is My Parcel* 프로젝트의 Config Server 애플리케이션입니다.

`config-hub` Git repository의 설정 파일을 읽어 각 마이크로서비스에 설정을 제공합니다.

---

## 0. 간편 가이드

각 마이크로서비스 담당자는 아래 사항을 체크해주세요.

- `spring-cloud-starter-config` 의존성 추가
- `spring-boot-starter-actuator` 의존성 추가
- 서비스의 `application.yml`에 Config Server 접속 설정 추가
- `spring.application.name`을 config-hub의 파일명과 일치시킴
- 로컬 실행 시 필요한 환경변수 설정
- dev 환경에서 `/actuator/refresh` 필요 시 호출

---

## 1. 역할

Config Server는 각 Config Client가 요청한 `{application}`, `{profile}`, `{label}` 값에 맞는 설정을 `config-hub`에서 조회하여 제공합니다.

요청 형식:

```text
GET /{application}/{profile}/{label}
```

---

## 2. Config Client 설정 방법

각 마이크로서비스가 Config Server에서 설정을 받아오려면 다음 의존성을 추가해야 합니다.

```gradle
implementation 'org.springframework.cloud:spring-cloud-starter-config'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

각 서비스의 `application.yml`에는 Config Server 접속을 위한 최소 설정만 작성합니다.

```yaml
spring:
  application:
    name: 서비스-이름

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  config:
    import: "optional:configserver:"

  cloud:
    config:
      discovery:
        enabled: true
        service-id: config-server

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
```

`spring.application.name`은 config-hub의 파일명과 일치해야 합니다.

```text
spring.application.name=user-service
→ config-hub/user-service.yml
→ config-hub/user-service-dev.yml
```

---

## 3. 환경변수 설정

Config Client는 실행 환경에 따라 아래 환경변수를 주입받습니다.

| 변수명 | 설명 | 로컬 기본값 예시 |
|---|---|-----------|
| `SPRING_PROFILES_ACTIVE` | 활성화할 Spring profile | `dev`     |
| `CONFIG_REPO_LABEL` | config-hub Git label | `main`    |
| `EUREKA_URL` | Eureka Server 주소 | `http://localhost:8761/eureka/` |

---

## 4. 설정 조회 테스트

설정 파일 확인 명령어를 통해, Config Server가 정상 동작하는지 직접 확인할 수 있습니다:

```bash
curl http://localhost:8888/user-service/dev/main
```

YAML 형식으로 확인:

```bash
curl http://localhost:8888/user-service/dev/main.yml
```

Health check:

```bash
curl http://localhost:8888/actuator/health
```

---

## 5. Actuator refresh 사용

dev profile에서는 Config Client의 `/actuator/refresh` 으로 수동 refresh할 수 있습니다.

```bash
curl -X POST http://localhost:8081/actuator/refresh
```

Config Server가 아니라 각 Config Client에 호출하는 endpoint입니다.

```text
user-service    → /actuator/refresh
api-gateway     → /actuator/refresh
eureka-server   → /actuator/refresh
```

운영 환경에서는 refresh endpoint를 외부에 공개하지 않거나 인증/내부망 접근 제한 적용할 예정입니다.

---

## 6. Docker 실행 시 주의사항

Config Server Docker image에는 다음 환경변수를 설정해야 합니다.

```dockerfile
ENV XDG_CONFIG_HOME=/tmp
```

Git backend 사용 시 JGit이 컨테이너 내부에서 config 파일을 쓸 수 있는 경로를 보장해주어야 하기 때문입니다.

로컬 Docker 실행 예시:

```bash
docker run --rm --name config-server \
  -p 8888:8888 \
  -e XDG_CONFIG_HOME=/tmp \
  config-server:local
```

Windows 명령 프롬프트 기준 실행 예시:

```cmd
docker run --rm --name config-server -p 8888:8888 -e XDG_CONFIG_HOME=/tmp config-server:local
```
