# 프로젝트 마스터 가이드: whereIsMyParcel (DelivHub)

## 1. 프로젝트 기본 정보
- **서비스 명칭**: DelivHub
- **팀 명칭**: 보람사조 (3팀)
- **아키텍처**: MSA (Microservices Architecture)
- **관리 방식**: Gradle 기반 멀티 모듈 프로젝트

## 2. 핵심 기술 스택
- **언어**: Java 17 (LTS)
- **프레임워크**: Spring Boot 3.5.14 / Spring Cloud 2023.0.x
- **빌드 도구**: Gradle (Groovy)
- **기본 패키지**: com.sparta.delivhub

## 3. 멀티 모듈 구성
- `eureka-server`: 서비스 레지스트리 (포트: 8761)
- `config-server`: 중앙 설정 관리 (포트: 8888)
- `api-gateway`: 시스템 진입점, 라우팅 및 보안
- `user-service`: 사용자 관리 및 인증/인가 로직
- `common-module`: 공통 라이브러리 (응답 규격, 엔티티, 예외 처리)
- `infra`: Docker Compose 및 전체 시스템 통합 환경

## 4. 개발 규정 및 컨벤션
- **언어 설정**: AI는 모든 답변, 설명, 주석, 문서를 반드시 **한국어**로만 작성해야 합니다. 영어 사용은 절대 금지되며, 전문 용어의 경우 한글로 표기하거나 괄호 안에 병기합니다.
- **보안 검수**: Git에 코드를 올리기 전, 다음 항목을 반드시 자동 검수합니다.
    - API 키 (예: Gemini API Key, AWS Key)
    - 데이터베이스 비밀번호 및 접속 정보
    - `.env`, `application.yaml` 내의 평문 민감 정보
    - 위 항목이 발견될 경우 작업을 중단하고 사용자에게 알립니다.
- **엔티티**: 식별자는 UUID를 사용하며, 테이블 명칭에는 `p_` 접두사를 붙입니다.
- **논리적 삭제 (Soft Delete)**: 모든 엔티티는 `deleted_at`, `deleted_by` 필드를 포함해야 합니다.
- **감사 로그 (Audit)**: 모든 테이블에 `created_at`, `created_by`, `updated_at`, `updated_by` 필드를 포함합니다.
- **계층 구조**: Controller -> Service -> Repository -> Domain 순의 계층형 아키텍처를 준수합니다.

## 6. 개발 프로세스 및 PR 규칙
- **기능 단위 PR**: 하나의 기능 구현이 완료되면 반드시 PR(Pull Request)을 생성하여 코드 리뷰를 요청합니다.
- **작업 전환 시 필수 PR**: 새로운 기능이나 작업을 시작하기 전, 이전 작업에 대한 PR을 반드시 먼저 올리고 시작하는 것을 원칙으로 합니다.
- **PR 템플릿 준수**: 모든 PR은 정해진 템플릿에 맞춰 작업 내용, 관련 이슈, 체크리스트를 상세히 작성합니다.

## 5. 보안 전략
- JWT 기반 인증 방식을 사용합니다.
- 설정 서버(Config Server)는 HTTP Basic Auth 및 환경 변수를 통해 민감 정보를 보호합니다.
- 공개 저장소 규정을 준수하며, 비밀번호나 API 키 등은 절대 커밋하지 않습니다.
