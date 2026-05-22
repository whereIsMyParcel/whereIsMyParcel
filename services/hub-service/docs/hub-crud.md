# 🏢 Hub CRUD API 및 캐싱 구현 (Step 6)

## 1. 구현 내용
- **HubService**: 허브의 생성, 조회, 수정, 삭제(Soft Delete) 비즈니스 로직을 구현했습니다.
- **HubController**: 외부 요청을 처리하며, 권한 체크 및 페이지네이션 유효성 검증 로직을 포함합니다.
- **Redis 캐싱**: `@Cacheable`, `@CachePut`, `@CacheEvict`를 사용하여 허브 단건 조회 성능을 최적화하고 데이터 변경 시 캐시 정합성을 유지합니다.

## 2. 왜 이 방식을 선택했는가 (설계 의도)
- **RBAC (Role-Based Access Control)**: Gateway에서 주입된 `X-User-Role` 헤더를 검증하여 `MASTER` 또는 `HUB_MANAGER`만 쓰기 작업을 수행할 수 있도록 제한했습니다.
- **페이지네이션 제약**: 서버 부하 방지를 위해 페이지 크기를 10, 30, 50으로 강제했습니다.
- **Read-Through/Write-Through 캐싱**: 조회 시 캐시를 먼저 확인하고, 수정 시 캐시를 업데이트하며, 삭제 시 캐시를 제거하여 DB와 캐시 간의 정합성을 보장했습니다.

## 3. API 스펙
### GET /api/v1/hubs
- 전역 사용자 조회 가능
- Query Param: `page`, `size` (10, 30, 50 필수), `sort`
### POST /api/v1/hubs
- MASTER/HUB_MANAGER 전용
- Request Body: `CreateHubRequest`

## 4. 주의사항 및 한계
- 현재 권한 부족 시 `ForbiddenException`을 던지며, 이는 공통 `GlobalExceptionHandler`를 통해 처리됩니다.

## 5. 팀원 공유 사항
- 허브 조회 시 `hub::{hubId}` 키로 Redis에 캐싱되므로, 직접 DB를 수정할 경우 캐시를 수동으로 비워줘야 합니다.
