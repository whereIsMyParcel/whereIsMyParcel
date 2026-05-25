# 🏢 Hub Service (물류 허브 및 경로 관리 서비스)

`Hub Service`는 마이크로서비스 아키텍처(MSA) 기반의 물류 시스템에서 **중앙 물류 허브(Hub)**와 **허브 간의 이동 경로(Hub Route)**를 관리하는 핵심 도메인 서비스입니다.

## 📌 주요 기능 (Features)

### 1. 허브(Hub) 관리
물류의 거점이 되는 '허브' 정보를 관리합니다.
* **허브 생성/수정**: 허브의 이름, 주소, 위도(Latitude), 경도(Longitude) 정보를 등록합니다.
* **허브 조회**: 전체 허브 목록(페이징) 및 특정 허브의 상세 정보를 조회합니다.
* **허브 삭제 (Soft Delete)**: 실수로 인한 데이터 유실을 방지하기 위해 DB에서 완전히 지우지 않고 `deleted_at` 시간을 기록하여 숨김 처리합니다.

### 2. 허브 간 경로(Hub Route) 관리
허브와 허브를 잇는 이동 경로와 소요 시간, 거리 정보를 관리합니다.
* **경로 생성/수정**: 출발지 허브와 목적지 허브를 연결하고, 두 허브 사이의 **거리(Distance)**와 **소요 시간(Duration)**을 등록합니다.
* **경로 조회**: 전체 경로 목록 및 특정 경로 정보를 조회합니다.
* **캐시 최적화(Redis)**: 허브 간 최단 경로 탐색 등 조회 성능을 극대화하기 위해 Redis 캐시를 사용합니다. 경로가 추가/수정/삭제될 때마다 자동으로 관련 캐시(`evictAllPathCache`)를 초기화하여 데이터 정합성을 유지합니다.

---

## 🔒 보안 및 권한 (Security & Authorization)

이 서비스는 안전한 데이터 관리를 위해 철저한 권한 제어(Role-Based Access Control)를 시행하고 있습니다.

* **읽기(조회) 권한**: 누구나 (또는 내부망 API Gateway를 통해) 조회 가능합니다. (`GET` 요청)
* **쓰기(생성/수정/삭제) 권한**: 
  * API 요청 시 Header에 반드시 `X-User-Role` 값을 전달해야 합니다.
  * 허용된 권한: **`MASTER`** (최고 관리자) 또는 **`HUB_MANAGER`** (허브 담당자)
  * 권한이 없거나 일치하지 않으면 `403 Forbidden` 에러를 반환합니다.
* **삭제(Audit) 기록**: 
  * 데이터 삭제(`DELETE`) 시, 누가 삭제했는지 추적하기 위해 Header에 `X-User-Id`를 필수로 요구합니다.

---

## 📡 API 엔드포인트 명세

모든 API는 `/api/v1`을 Base URL로 사용합니다. 상세한 스펙은 서버 실행 후 **Swagger UI** (`http://localhost:8082/swagger-ui/index.html`)에서 확인 및 테스트 가능합니다.

### 🏢 Hub API (`/api/v1/hubs`)
| Method | URL | Description | Required Role |
|---|---|---|---|
| `POST` | `/api/v1/hubs` | 신규 허브 등록 | `MASTER`, `HUB_MANAGER` |
| `GET` | `/api/v1/hubs` | 전체 허브 목록 조회 (페이징 지원) | 없음 (Public) |
| `GET` | `/api/v1/hubs/{hubId}` | 특정 허브 단건 조회 | 없음 (Public) |
| `PATCH`| `/api/v1/hubs/{hubId}` | 기존 허브 정보 수정 | `MASTER`, `HUB_MANAGER` |
| `DELETE`| `/api/v1/hubs/{hubId}` | 특정 허브 삭제 (Soft Delete) | `MASTER`, `HUB_MANAGER` |

### 🛣️ Hub Route API (`/api/v1/hub-routes`)
| Method | URL | Description | Required Role |
|---|---|---|---|
| `POST` | `/api/v1/hub-routes` | 신규 허브 간 경로 등록 | `MASTER`, `HUB_MANAGER` |
| `GET` | `/api/v1/hub-routes` | 전체 허브 경로 목록 조회 (페이징 지원) | 없음 (Public) |
| `GET` | `/api/v1/hub-routes/{routeId}`| 특정 허브 경로 단건 조회 | 없음 (Public) |
| `PATCH`| `/api/v1/hub-routes/{routeId}`| 기존 허브 경로 정보(거리, 시간) 수정| `MASTER`, `HUB_MANAGER` |
| `DELETE`| `/api/v1/hub-routes/{routeId}`| 특정 허브 경로 삭제 (Soft Delete) | `MASTER`, `HUB_MANAGER` |

---

## 🛠 기술 스택 (Tech Stack)
* **Framework**: Spring Boot 3.x
* **Database**: PostgreSQL (`hub_db` 스키마 사용)
* **ORM**: Spring Data JPA (Hibernate)
* **Cache**: Redis (경로 조회 최적화)
* **Service Discovery**: Spring Cloud Netflix Eureka Client
* **API Docs**: Springdoc OpenAPI (Swagger UI)
