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

**인프라만 (개발 중 기본 사용)**

```bash
docker compose -f infra/docker-compose.yml --env-file .env up -d
```

postgres, keycloak, redis, zipkin, eureka-server, config-server가 올라옵니다.

**전체 서비스 포함**

```bash
docker compose -f infra/docker-compose.yml --env-file .env --profile services up -d --build
```

> `--env-file .env` 옵션은 반드시 포함해야 합니다. 없으면 환경변수 치환이 실패합니다.

---

## 3. 상태 확인

```bash
# 컨테이너 상태
docker compose -f infra/docker-compose.yml --env-file .env ps

# 스키마 생성 확인
docker exec -it postgres psql -U sparta -d sparta_logistics -c "\dn"

# Keycloak realm 확인
curl http://localhost:8080/realms/sparta-logistics

# Eureka 등록 서비스 확인
curl -H "Accept: application/json" http://localhost:8761/eureka/apps
```

> Keycloak은 최초 기동 시 `realm-export.json`을 자동 import합니다. 기동까지 **1분 정도** 소요됩니다.

---

## 4. 토큰 발급 테스트 (Postman)

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

## 5. 자주 쓰는 명령어

```bash
# 로그 확인
docker compose -f infra/docker-compose.yml --env-file .env logs -f keycloak

# 특정 서비스만 재빌드
docker compose -f infra/docker-compose.yml --env-file .env up -d --build user-service

# 종료
docker compose -f infra/docker-compose.yml --env-file .env down

# 볼륨까지 삭제 (완전 초기화)
docker compose -f infra/docker-compose.yml --env-file .env down -v
```

---

## 참고 문서

| 문서 | 내용 |
|---|---|
| `docs/architecture-guide.md` | 전체 아키텍처 및 트래픽 흐름 |
| `docs/conventions.md` | 코드 컨벤션 |
