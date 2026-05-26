# 📡 Internal API 구현 및 타 서비스 연동 (Step 9)

## 1. 구현 내용
- **HubInternalController**: 서비스 간 내부 통신을 위한 전용 엔드포인트를 구축했습니다.
- **주요 엔드포인트**:
  - `GET /internal/v1/hubs/{hubId}/validate`: 특정 허브의 존재 여부를 `Boolean`으로 반환합니다.
  - `GET /internal/v1/hub-routes/shortest-path`: 다익스트라 알고리즘 기반의 최단 경로 정보를 반환합니다.

## 2. 왜 이 방식을 선택했는가 (설계 의도)
- **보안 및 인증 우회**: `/internal/v1/**` 프리픽스를 사용하여 게이트웨이 수준에서 JWT 인증을 건너뛰고 서비스 간 다이렉트 통신(OpenFeign 등)이 가능하도록 설계했습니다.
- **예외 캡슐화**: `validate` API의 경우, 허브가 없을 때 에러를 터뜨리는 대신 `false`를 반환하도록 처리하여 호출하는 측(Company Service)의 로직을 단순화했습니다.

## 3. API 스펙
### GET /internal/v1/hubs/{hubId}/validate
- 목적: 허브 존재 확인
- 응답: `{ "data": true/false }`
### GET /internal/v1/hub-routes/shortest-path?originHubId={id}&destinationHubId={id}
- 목적: 배송을 위한 최적 경로 데이터 확보
- 응답: `ShortestPathResponse` (경유지 리스트 및 총 메트릭 포함)

## 4. 주의사항 및 한계
- 내부 API이므로 절대 외부에 노출되어서는 안 됩니다. (Gateway 설정 확인 필수)


