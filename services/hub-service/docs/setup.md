# 🛠️ Hub Service 환경 설정 (Step 1)

## 1. 구현 내용
- **의존성 최적화**: 담당 범위(Hub & Route) 외의 기능을 위한 Redisson(분산 락) 의존성을 제거하고, MSA 환경에 필요한 핵심 의존성(Config, Eureka, OpenFeign) 위주로 재구성했습니다.
- **Gradle 설정**: 루트 프로젝트의 설정을 상속받으며, `common` 모듈과 JPA, Redis, Validation 관련 스타터를 포함합니다.

## 2. 왜 이 방식을 선택했는가 (설계 의도)
- **도메인 경계 준수**: 배송담당자 배정(Saga/Lock) , 불필요한 인프라 복잡성을 줄이기 위해 Redisson을 제거했습니다.
- **중앙 설정 관리**: `spring-cloud-starter-config`를 통해 환경별 설정을 `config-hub` 저장소에서 관리할 수 있도록 구성했습니다.

## 3. API 스펙
- 해당 단계는 인프라 설정 단계로, 별도의 노출 API가 없습니다.

## 4. 주의사항 및 한계
- 로컬 실행 시 `config-server`와 `eureka-server`가 먼저 구동되어 있어야 정상적인 설정 로딩이 가능합니다.
- `hub_db` 스키마가 PostgreSQL에 미리 생성되어 있어야 합니다.

