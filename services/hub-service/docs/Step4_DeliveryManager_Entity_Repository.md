# Step 4: DeliveryManager 엔티티 및 Repository 구현 가이드

## 1. 구현 내용
- `DeliveryManager` 엔티티 구현
  - UUID PK (`delivery_manager_id`) 및 `p_delivery_managers` 테이블 매핑
  - `Hub`와의 다대일(ManyToOne) 지연 로딩 관계 설정
  - 배송원 타입(`HUB`, `COMPANY`) 및 상태(`AVAILABLE`, `ASSIGNED`) Enum 추가
  - 배정 순번(`deliveryOrder`) 및 슬랙 ID(`slackId`) 필드 추가
  - 내부 빌더 + 외부 정적 팩토리 메서드(`create`) 패턴 적용
- `DeliveryManagerRepository` 인터페이스 구현
  - 마지막 순번 조회(`findMaxDeliveryOrderByHub`) 및 배정 최우선 순위 조회 메서드 정의

## 2. 설계 의도 및 패턴 선택 이유
- **배정 순번 자동화**: 담당자 등록 시 해당 허브의 마지막 순번 + 1을 부여하기 위해 Repository 수준에서 `MAX` 쿼리를 지원하도록 설계했습니다.
- **분산 락 대비**: 향후 `DeliveryManagerStatus` 변경 시(배정/해제) Redisson 분산 락을 적용할 대상이므로, 상태 변경 메서드(`assign`, `unassign`)를 엔티티 내부에 캡슐화했습니다.
- **슬랙 연동**: 배송 상태 알림 발송을 위해 필수 값인 `slackId`를 고유 값(unique)으로 관리합니다.

## 3. 핵심 코드 설명
- `DeliveryManager.java`: `status` 초기값을 `AVAILABLE`로 고정하는 `create` 메서드 구현.
- `DeliveryManagerRepository.java`: `findFirstBy...OrderByDeliveryOrderAsc`를 통해 비즈니스 규칙(순번 오름차순 배정)을 쿼리 레벨에서 보장.

