# 🧠 Dijkstra 최단 경로 알고리즘 구현 (Step 8)

## 1. 구현 내용
- **ShortestPathService**: 다익스트라(Dijkstra) 알고리즘을 사용하여 두 허브 간의 최단 경로를 계산하는 핵심 로직을 구현했습니다.
- **다익스트라 알고리즘**:
  - `PriorityQueue`를 활용하여 효율적인 최단 거리 노드 탐색을 수행합니다.
  - 거리(km)를 가중치로 사용하여 최적의 경로를 결정합니다.
  - 소요 시간(분)을 함께 합산하여 배송 타임라인 정보를 제공합니다.
- **Redis 캐싱 (Jitter 적용)**:
  - 계산된 경로 결과는 `path:{origin}:{dest}` 키로 Redis에 저장됩니다.
  - TTL 6시간에 0~10분의 무작위 **Jitter**를 추가하여 Cache Stampede(동시 캐시 만료로 인한 DB 부하)를 방지했습니다.

## 2. 왜 이 방식을 선택했는가 (설계 의도)
- **왜 Dijkstra인가**: 허브 간 네트워크는 가중치(거리/시간)가 있는 유향 그래프(Directed Graph)이므로, BFS/DFS보다 최적의 경로를 보장하는 다익스트라가 가장 적합합니다.
- **메모리 내 계산**: 현재 허브 개수가 17개로 적으므로, 매번 전체 경로 데이터를 가져와 메모리 내에서 그래프를 구성하여 계산하는 방식이 간단하면서도 충분히 빠릅니다.
- **경로 복원**: 단순 총합뿐만 아니라, 어느 허브를 순서대로 거쳐야 하는지 `List<RouteSegmentResponse>` 형태로 복원하여 배송 서비스(Shipment)에서 바로 활용할 수 있도록 했습니다.

## 3. 캐시 전략
- **Key**: `path:{originHubId}:{destinationHubId}`
- **TTL**: 6h + random(0~600s)
- **Eviction**: `HubRouteService`에서 경로 데이터 수정 시 해당 허브와 관련된 모든 `path:*` 캐시가 삭제됩니다.

## 4. 주의사항 및 한계
- 허브 수가 기하급수적으로 늘어날 경우, 전체 데이터를 조회하는 방식 대신 공간 인덱스나 인접 리스트의 캐싱 고도화가 필요할 수 있습니다.

## 5. 팀원 공유 사항
- `ShortestPathResponse`의 `routes` 리스트는 배송 순서(`sequence`)대로 정렬되어 있습니다.
- 만약 경로가 존재하지 않을 경우 `NoPathBetweenHubsException` (HUB-003)이 발생합니다.
