# Step 1: Hub Service 모듈 설정 가이드

## 1. 구현 내용
- `hub-service` 모듈의 `build.gradle` 의존성 추가
  - OpenFeign, Redis, Redisson (분산 락용) 의존성 추가
- `application.yaml` 환경 설정
  - PostgreSQL 스키마를 `hub_schema`로 지정
  - Redis 연결 정보 및 로깅 레벨 설정
  - Eureka 및 Config Server 클라이언트 활성화

## 2. 설계 의도 및 패턴 선택 이유
- **Redisson**: MSA 환경에서 여러 인스턴스가 동시에 배송 담당자를 배정할 때 발생할 수 있는 레이스 컨디션을 방지하기 위해 분산 락(Distributed Lock)이 필요하며, Spring Boot와 연동이 쉬운 Redisson을 선택했습니다.
- **OpenFeign**: 서비스 간 통신 시(Shipment 등) 하드코딩된 URL이 아닌 Eureka 서비스명을 참조하여 선언적으로 호출하기 위해 사용합니다.
- **Schema 분리**: `currentSchema=hub_schema` 설정을 통해 단일 DB 내에서도 서비스별 논리적 격리를 유지합니다.

## 3. 핵심 코드 설명
- `build.gradle`: `implementation 'org.redisson:redisson-spring-boot-starter:3.43.0'` 추가
- `application.yaml`: `spring.datasource.url` 및 `spring.jpa.properties.hibernate.default_schema` 업데이트

## 4. 관련 팀원 공유 사항
- **공통**: DB 연결 시 `hub_schema` 권한이 필요합니다. 로컬 테스트 시 해당 스키마가 생성되어 있는지 확인해 주세요.
