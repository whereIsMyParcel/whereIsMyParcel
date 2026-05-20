# Step 2: Hub 엔티티 및 Repository 구현 가이드

## 1. 구현 내용
- `Hub` 엔티티 클래스 생성
  - UUID PK (`hub_id`) 및 `BaseEntity` 상속을 통한 Audit/Soft Delete 적용
  - 테이블명 `p_hubs`, 스키마 `hub_db` 지정
  - Dijkstra 알고리즘을 위한 위경도(`latitude`, `longitude`) 필드 추가
- `HubRepository` 인터페이스 생성
  - Soft Delete를 고려한 `findByHubIdAndDeletedAtIsNull` 등 조회 메서드 정의

## 2. 설계 의도 및 패턴 선택 이유
- **UUID PK**: 분산 환경(MSA)에서 ID 생성의 충돌을 방지하고 보안성을 높이기 위해 채택했습니다.
- **Soft Delete**: 하네스 규칙에 따라 데이터를 물리적으로 삭제하지 않고 `deleted_at`을 통해 관리함으로써 데이터 보존 및 감사 추적성을 확보했습니다.
- **위경도 필드**: 17개 허브 간의 경로 계산(Dijkstra) 시 좌표 기반의 거리 또는 가중치를 산정하기 위해 필수적으로 포함했습니다.

## 3. 핵심 코드 설명
- `Hub.java`: `@Table(schema = "hub_db")`를 통해 멀티 스키마 환경에서 정확한 테이블 위치를 지정했습니다.
- `HubRepository.java`: JPA 기본 `findById` 대신 `AndDeletedAtIsNull` 조건이 붙은 쿼리 메서드를 사용하여 삭제된 허브가 비즈니스 로직에 포함되지 않도록 원천 차단했습니다.


