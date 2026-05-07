# 전체 개발 환경 실행 가이드

본 문서는 *Where Is My Parcel* 프로젝트의 전체 개발 환경을 로컬에서 실행하기 위한 가이드입니다.

각 마이크로서비스는 멀티모듈 레포지토리의 `services` 하위 모듈로 관리하며, 전체 개발 환경은 `infra/docker-compose.yml`로 실행합니다.

---

## 목차

1. 서비스 구성
2. Dockerfile 구성
3. 환경변수 설정
4. 전체 개발 환경 실행 방법
5. 통신 상태 확인
6. 주의사항

---

## 1. 서비스 구성

| 서비스 | 포트 | 설명 |
|---|---:|---|
| `eureka-server` | 8761 | 서비스 디스커버리. 다른 서비스들이 등록되는 레지스트리 |
| `config-server` | 8888 | 중앙 설정 서버. Eureka에 등록 후 각 서비스에 설정 제공 |
| `api-gateway` | 8000 | API 라우팅 게이트웨이 |
| `user-service` | 8081 | 사용자 관리 서비스 |

실행 순서는 `eureka-server` -> `config-server` -> `api-gateway`, `user-service`입니다.

`infra/docker-compose.yml`에서는 `depends_on`과 healthcheck를 사용해 위 순서로 실행되도록 구성합니다.

---

## 2. Dockerfile 구성

각 실행 서비스 디렉토리에는 `Dockerfile`이 포함되어 있습니다.

```text
whereIsMyParcel/
├── infra/
│   └── docker-compose.yml
└── services/
    ├── api-gateway/
    │   └── Dockerfile
    ├── config-server/
    │   └── Dockerfile
    ├── eureka-server/
    │   └── Dockerfile
    └── user-service/
        └── Dockerfile
```

Docker build context는 프로젝트 루트이며, `infra/docker-compose.yml`에서 서비스별 Dockerfile 경로를 지정합니다.

```yaml
build:
  context: ..
  dockerfile: services/user-service/Dockerfile
```

각 Dockerfile은 멀티모듈 구조에 맞춰 루트의 Gradle 파일, `common` 모듈, `services` 모듈을 복사한 뒤 해당 서비스의 `bootJar`를 빌드합니다.

```dockerfile
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common ./common
COPY services ./services

RUN chmod +x gradlew
RUN ./gradlew :services:user-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/services/user-service/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

`config-server`는 JGit 사용으로 인해 런타임 이미지에 아래 환경변수가 추가되어 있습니다.

```dockerfile
ENV XDG_CONFIG_HOME=/tmp
```

---

## 3. 환경변수 설정

Docker Compose 실행 시 아래 환경변수를 사용할 수 있습니다.

| 변수명 | 설명 | 기본값 |
|---|---|---|
| `EUREKA_URL` | Eureka Server 주소 | `http://eureka-server:8761/eureka/` |
| `SPRING_PROFILES_ACTIVE` | 활성화할 Spring profile | `dev` |

현재 `infra/docker-compose.yml`은 Docker 내부 네트워크 기준으로 `EUREKA_URL` 기본값을 설정합니다.

```yaml
environment:
  EUREKA_URL: ${EUREKA_URL:-http://eureka-server:8761/eureka/}
  SPRING_PROFILES_ACTIVE: dev
```

로컬에서 별도 환경 값을 주입해야 하는 경우 프로젝트 루트에 `.env`를 생성해 관리할 수 있습니다.

> 민감정보(DB 패스워드, JWT Secret 등)는 Git에 커밋하지 않습니다.

---

## 4. 전체 개발 환경 실행 방법

프로젝트 루트에서 아래 명령어를 실행합니다.

```bash
docker compose -f infra/docker-compose.yml up -d --build
```

컨테이너 상태를 확인합니다.

```bash
docker compose -f infra/docker-compose.yml ps
```

전체 로그를 확인합니다.

```bash
docker compose -f infra/docker-compose.yml logs -f
```

특정 서비스 로그만 확인합니다.

```bash
docker compose -f infra/docker-compose.yml logs -f user-service
```

특정 서비스만 재빌드하려면 아래 명령어를 실행합니다.

```bash
docker compose -f infra/docker-compose.yml up -d --build user-service
```

전체 개발 환경을 종료합니다.

```bash
docker compose -f infra/docker-compose.yml down
```

---

## 5. 통신 상태 확인

Windows PowerShell에서는 실제 curl 실행 파일을 명확히 호출하기 위해 `curl.exe` 사용을 권장합니다.

Eureka에 등록된 전체 서비스 목록을 확인합니다.

```powershell
curl.exe -H "Accept: application/json" http://localhost:8761/eureka/apps
```

특정 서비스가 Eureka에 등록됐는지 확인합니다.

```powershell
curl.exe -H "Accept: application/json" http://localhost:8761/eureka/apps/CONFIG-SERVER
curl.exe -H "Accept: application/json" http://localhost:8761/eureka/apps/API-GATEWAY
curl.exe -H "Accept: application/json" http://localhost:8761/eureka/apps/USER-SERVICE
```

Config Server가 각 서비스 설정을 내려주는지 확인합니다.

```powershell
curl.exe http://localhost:8888/user-service/dev
curl.exe http://localhost:8888/api-gateway/dev
```

Git Bash, WSL, macOS, Linux에서는 `curl.exe` 대신 `curl`을 사용하면 됩니다.

```bash
curl -H "Accept: application/json" http://localhost:8761/eureka/apps
curl http://localhost:8888/user-service/dev
curl http://localhost:8888/api-gateway/dev
```

---

## 6. 주의사항

`common` 모듈은 독립 실행 애플리케이션이 아니므로 Docker Compose 서비스 대상에 포함하지 않습니다.

Docker Compose는 `where-is-my-parcel-network` 브리지 네트워크를 생성하고, 각 서비스는 컨테이너 이름으로 통신합니다.

컨테이너 내부에서는 Eureka 주소로 `localhost`가 아니라 `http://eureka-server:8761/eureka/`를 사용해야 합니다.
