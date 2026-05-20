# Step 3: HubRoute 엔티티 및 Repository 구현 가이드

## 1. 구현 내용
- `HubRoute` 엔티티 구현
  - UUID PK (`hub_route_id`) 적용 및 `p_hub_routes` 테이블 매핑
  - `originHub`, `destinationHub`와 다대일(ManyToOne) 연관관계 설정 (Lazy Loading)
  - 거리(`distance`) 및 소요 시간(`duration`) 필드 추가
  - @SQLRestriction을 활용한 전역 Soft Delete 적용
  - 내부 빌더 + 정적 팩토리 메서드(`create`) 패턴 적용
- `HubRouteRepository` 인터페이스 구현
  - 출발/목적지 기반 조회 및 인접 리스트 탐색용 메서드 정의

## 2. 설계 의도 및 패턴 선택 이유
- **전역 Soft Delete 필터링**: 엔티티 레벨에 `@SQLRestriction`을 적용하여, 삭제된 경로가 Dijkstra 경로 탐색 결과에 포함되어 배송 지연을 유발하는 상황을 원천 차단했습니다.
- **도메인 무결성 검증**: 동일한 출발지와 목적지를 설정하거나, 0 이하의 거리/시간 값을 입력하는 것을 도메인 내부에서 방지하도록 설계했습니다.
- **Dijkstra 최적화**: Repository에 `findAllByOriginHub`를 정의하여, 경로 탐색 시 특정 노드의 인접 노드들을 효율적으로 조회할 수 있도록 준비했습니다.

## 3. 핵심 코드 설명
- `HubRoute.java`: `originHub`와 `destinationHub`가 같은지 체크하는 `validateHubs` 로직 포함.
- `HubRouteRepository.java`: JPA 전역 필터링 덕분에 별도의 `deleted_at` 조건을 수동으로 관리할 필요가 없습니다.


