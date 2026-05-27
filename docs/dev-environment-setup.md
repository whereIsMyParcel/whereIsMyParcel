# 개발 환경 실행 가이드

## 사전 준비

- Docker Desktop 설치 및 실행

---

## 1. 환경변수 설정

```bash
cp .env.example .env
```

기본값 그대로 사용하면 됩니다. `.env`는 Git에 커밋하지 않습니다.

---

## 2. 실행

모든 docker compose 명령은 **`infra/` 디렉토리에서** 실행합니다.

```bash
cd infra
```

**인프라만 (개발 중 기본 사용)**

```bash
docker compose up -d
```

postgres, keycloak, redis, zipkin, eureka-server, config-server가 올라옵니다.

**전체 서비스 포함**

```bash
docker compose --profile services up -d --build
```

---

## 3. 완전 초기화 (볼륨 포함 삭제)

이전에 한 번이라도 실행한 적 있는 환경에서 처음부터 다시 시작하려면 반드시 `-v` 옵션으로 볼륨까지 삭제해야 합니다.

```bash
# 볼륨까지 삭제 (keycloak DB, 스키마 포함 완전 초기화)
docker compose down -v

# 그 다음 다시 올리기
docker compose up -d
```

> `-v` 없이 `down`만 하면 postgres 볼륨이 남아 `init.sh`가 실행되지 않아 keycloak DB와 스키마가 생성되지 않습니다.

---

## 4. 상태 확인

```bash
# 컨테이너 상태
docker compose ps

# 스키마 생성 확인
docker exec -it postgres psql -U sparta -d sparta_logistics -c "\dn"

# Keycloak realm 확인
curl http://localhost:8080/realms/sparta-logistics

# Eureka 등록 서비스 확인
curl -H "Accept: application/json" http://localhost:8761/eureka/apps
```

> Keycloak은 최초 기동 시 `realm-export.json`을 자동 import합니다. 기동까지 **1분 정도** 소요됩니다.

---

## 5. 토큰 발급 테스트 (Postman)

```
POST http://localhost:8080/realms/sparta-logistics/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password
client_id=local-dev-client
username=master01
password=Master1234!
```

응답의 `access_token`을 API 요청 헤더에 사용합니다.

```
Authorization: Bearer {access_token}
```

**테스트 계정**

| username | password | role |
|---|---|---|
| `master01` | `Master1234!` | MASTER |
| `testuser01` | `Test1234!` | COMPANY_MANAGER |

---

## 6. Swagger UI

| 주소 | 설명 |
|---|---|
| `http://localhost:8000/swagger-ui/index.html` | API Gateway — 전 서비스 선택 가능 |
| `http://localhost:8081/swagger-ui/index.html` | user-service 직접 |
| `http://localhost:8082/swagger-ui/index.html` | hub-service 직접 |
| `http://localhost:8083/swagger-ui/index.html` | company-service 직접 |
| `http://localhost:8084/swagger-ui/index.html` | order-service 직접 |
| `http://localhost:8085/swagger-ui/index.html` | shipment-service 직접 |
| `http://localhost:8086/swagger-ui/index.html` | ai-slack-service 직접 |

---

## 7. 자주 쓰는 명령어

```bash
# 로그 확인
docker compose logs -f keycloak

# 특정 서비스만 재빌드
docker compose --profile services up -d --build user-service

# 종료 (볼륨 유지)
docker compose down

# 볼륨까지 삭제 (완전 초기화)
docker compose down -v
```

---

## Windows 사용자 주의사항

`.gitattributes`에 `*.sh text eol=lf` 설정이 되어 있어 `init.sh`는 자동으로 LF로 유지됩니다.
처음 클론 후 아래 명령을 한 번 실행하면 적용됩니다.

```bash
git rm --cached -r .
git reset --hard
```

---

## 참고 문서

| 문서 | 내용 |
|---|---|
| `docs/architecture-guide.md` | 전체 아키텍처 및 트래픽 흐름 |
| `docs/conventions.md` | 코드 컨벤션 |
