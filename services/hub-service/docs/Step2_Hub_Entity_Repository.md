# Step 2: Hub 엔티티 및 Repository 구현 가이드

## 1. 구현 내용
- `Hub` 엔티티 클래스 생성
  - UUID PK (`hub_id`) 및 `BaseEntity` 상속을 통한 Audit/Soft Delete 적용
  - 테이블명 `p_hubs`, 스키마 `hub_db` 지정
  - Dijkstra 알고리즘을 위한 위경도(`latitude`, `longitude`) 필드 추가
- `HubRepository` 인터페이스 생성
  - Soft Delete를 고려한 `findByHubIdAndDeletedAtIsNull` 등 조회 메서드 정의

## 2. 설계 의도 및 패턴 선택 이유
- **전역 Soft Delete 필터링 (@SQLRestriction)**: 엔티티 레벨에 `@SQLRestriction("deleted_at IS NULL")`을 적용하여, `JpaRepository`의 기본 메서드(`findById`, `findAll`) 호출 시에도 자동으로 삭제되지 않은 데이터만 조회되도록 강제했습니다. 이로 인해 개발자의 휴먼 에러를 방지하고 Repository 인터페이스를 깔끔하게 유지할 수 있습니다.
- **위경도 유효성 검증**: 도메인 객체 생성 및 수정 시 위도(-90~90)와 경도(-180~180)의 범위를 검증(`validateCoordinates`)하여, 이후 Dijkstra 알고리즘 연산 시 논리적 오류를 유발할 수 있는 잘못된 좌표 데이터가 DB에 삽입되는 것을 원천 차단했습니다.
- **UUID PK**: 분산 환경(MSA)에서 ID 생성의 충돌을 방지하고 보안성을 높이기 위해 채택했습니다.

## 3. 핵심 코드 설명
- `Hub.java`: 
  - `@SQLRestriction("deleted_at IS NULL")` 어노테이션 적용
  - `validateCoordinates` 메서드를 통한 생성자 및 `update` 메서드 방어 로직 구현
- `HubRepository.java`: 전역 필터링 적용으로 인해 불필요해진 `AndDeletedAtIsNull` 중복 쿼리 메서드 제거


