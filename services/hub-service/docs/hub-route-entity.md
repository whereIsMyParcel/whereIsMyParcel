# 🚚 HubRoute 엔티티 및 Repository 구현 (Step 3)

## 1. 구현 내용
- **HubRoute 엔티티**: 허브 간 이동 경로 및 소요 시간 데이터를 관리하는 엔티티를 구현했습니다. 
  - `originHub`, `destinationHub`와 다대일(ManyToOne) 지연 로딩 관계 설정.
  - UUID PK, 거리(km), 소요 시간(분) 정보를 포함하며 JPA Audit 및 Soft Delete가 적용되었습니다.
- **HubRouteRepository**: 출발/목적지 기반 조회 및 Dijkstra 알고리즘을 위한 인접 경로 탐색 메서드(`findAllByOriginHub`)를 포함합니다.

## 2. 왜 이 방식을 선택했는가 (설계 의도)
- **무결성 검증**: 동일한 출발지와 목적지를 설정할 수 없도록 도메인 내부에서 검증 로직(`validateHubs`)을 추가했습니다.
- **Dijkstra 탐색 최적화**: 경로 탐색 시 특정 허브에서 연결된 모든 경로를 효율적으로 가져오기 위해 Repository 수준에서 최적화된 쿼리 메서드를 정의했습니다.
- **지연 로딩 (Lazy Loading)**: 불필요한 Hub 정보의 즉시 조회를 방지하여 성능 최적화를 도모했습니다.

## 3. API 스펙 (Request/Response DTO)
### CreateHubRouteRequest (record)
- `originHubId`: 출발 허브 UUID
- `destinationHubId`: 목적 허브 UUID
- `distance`: 이동 거리 (km)
- `duration`: 소요 시간 (분)

## 4. 주의사항 및 한계
- 경로 데이터는 허브 간 일대일 대응이 아닐 수 있으나(왕복 소요 시간이 다를 수 있음), 현재는 단방향으로 각기 저장하는 구조입니다.

## 5. 팀원 공유 사항
- 특정 허브 삭제 시 해당 허브를 포함하는 모든 경로를 연쇄적으로 비활성화하는 로직은 서비스 레이어에서 구현될 예정입니다.
