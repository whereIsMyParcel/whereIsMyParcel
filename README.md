# 📦 WhereIsMyParcel
> MSA 기반 B2B 물류 허브 및 배송 경로 최적화 관리 시스템

![Java](https://img.shields.io/badge/java-17-%23ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/spring_boot-3.5.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/spring_cloud-2025.0.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)

<br/>

## 📑 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [시스템 아키텍처](#2-시스템-아키텍처)
3. [팀원 역할분담](#3-팀원-역할분담)
4. [기술 스택](#4-기술-스택)
5. [ERD (엔티티 관계도)](#5-erd-엔티티-관계도)
6. [실행 방법 (Quick Start)](#6-실행-방법-quick-start)
7. [API 문서](#7-api-문서)

---

## 📌 1. 프로젝트 개요
**WhereIsMyParcel**은 전국 17개 물류 허브와 각 생산/수령 업체를 연동하여 상품의 생산부터 배송까지의 전 과정을 효율적으로 관리하는 **B2B 물류 시스템**입니다. 
단일 서버의 한계를 극복하기 위해 **MSA(Microservice Architecture)** 로 구축되었으며, 각 도메인별 서비스가 독립적으로 배포 및 확장 가능하도록 설계되었습니다.
최단 경로 알고리즘, AI 기반 배송 기한 예측, 경로 최적화 및 상태 머신(State Machine)을 활용한 안전한 주문 관리를 통해 자동화되고 신뢰성 높은 물류 프로세스를 제공합니다.

---

## 🏗 2. 시스템 아키텍처

### 2.1 인프라 아키텍처 설계도
<img width="1728" height="1080" alt="인프라" src="https://github.com/user-attachments/assets/f2e381c6-dad4-4a2c-9c21-5e270fffd1ff" />


### 2.2 마이크로서비스(MSA) 구성
- **Eureka Server**: 서비스 디스커버리 및 레지스트리 관리
- **Config Server**: 중앙 집중식 통합 설정 파일 관리
- **API Gateway**: 진입점 라우팅, JWT 통합 인증 및 Role 기반 권한 필터링
- **User Service**: 사용자(일반, 허브 관리자, 배송 기사 등) CRUD 및 가입 승인
- **Hub Service**: 17개 물류 허브 관리, 허브 간 이동 경로/소요시간 관리, 최단 경로 알고리즘
- **Company Service**: 생산/수령 업체 관리, 상품 매핑 및 기본 재고 로직 처리
- **Order Service**: 주문 생성/취소 상태 머신 관리, 유효성 체크(Feign), 주문 이력 관리
- **Shipment Service**: 배송 상태 기록, 배송 상세 경로 로깅, 배송 담당자 자동 배정
- **AI & Slack Service**: Gemini 연동(발송 시한 계산), Naver Map API 연동(경로 최적화), 슬랙 스케줄링 알림

---

## 🧑‍💻 3. 팀원 역할분담

| 이름 | 담당 도메인 | 주요 역할 (Roles & Responsibilities) |
|:---:|:---:|---|
| **재중** | `Gateway & Auth` | - 유레카/게이트웨이 설정 및 라우팅<br>- JWT 통합 인증 및 권한(Role) 필터링<br>- 사용자(User) CRUD 및 가입 승인 로직 |
| **재훈** | `Hub & Route` | - 17개 물류 허브 관리 (CRUD)<br>- 허브 간 이동 경로/소요 시간 데이터 관리<br>- 최단 경로 탐색 알고리즘 구현 |
| **승민** | `Company & Product` | - 생산/수령 업체 관리 (Hub와 연동)<br>- 상품 정보 및 업체별 상품 매핑<br>- 기본 재고(Inventory) 관리 로직 |
| **재범** | `Order` | - 주문 생성/취소 및 상태(상태 머신) 관리<br>- 주문 시 상품/재고 유효성 체크 (Feign)<br>- 주문 이력 검색 및 페이징 처리 |
| **슬기** | `Delivery & Member` | - 배송 상태 및 상세 경로 기록(Log) 관리<br>- 배송 담당자(허브/업체) 관리<br>- 순번 기반 담당자 자동 배정 알고리즘 |
| **지민** | `AI & Slack` | - Gemini API 연동 (최종 발송 시한 계산)<br>- 네이버 Map API 연동 (경로 최적화)<br>- 슬랙 알림 발송 및 스케줄링 |

---

## 💻 4. 기술 스택

### Backend & Architecture
- **Language**: Java 17
- **Framework**: Spring Boot 3.5.x
- **Cloud/MSA**: Spring Cloud 2025.0.x (Eureka, Gateway, Config, OpenFeign)

### Database & Caching
- **RDBMS**: PostgreSQL
- **NoSQL/Cache**: Redis (세션 및 빠른 재고 캐싱 등)

### Monitoring & Logging
- **Metrics**: Prometheus, Micrometer
- **Tracing**: Zipkin (Brave)
- **Logging**: Loki, Sentry (에러 트래킹)

### External API & AI
- **AI/ML**: Google Gemini API
- **Map/Route**: Naver Map API
- **Notification**: Slack Webhook API

### Build & Deploy
- **Build**: Gradle (Multi-module Architecture)
- **Container**: Docker, Docker Compose

---

## 📊 5. ERD (엔티티 관계도)
<img width="2135" height="1383" alt="최종 erd" src="https://github.com/user-attachments/assets/fea8ac69-cc83-4dde-b392-222dbcb0f1d8" />

---

## 🚀 6. 실행 방법 (Quick Start)

### 사전 요구사항 (Prerequisites)
- `Java 17` 이상
- `Docker` 및 `Docker Compose`
- 로컬 환경 내 `.env` 파일 구성 (API Key 등)

### 로컬 실행 순서
1. **저장소 클론 및 환경변수 설정**
   ```bash
   git clone https://github.com/your-username/whereIsMyParcel.git
   cd whereIsMyParcel
   cp .env.example .env  # 본인의 환경에 맞게 변수 수정
   ```

2. **인프라(DB, Redis, 모니터링 등) 컨테이너 구동**
   ```bash
   docker-compose up -d
   ```

3. **프로젝트 빌드 (멀티 모듈 전체)**
   ```bash
   ./gradlew clean build -x test
   ```

4. **MSA 서비스 순차적 실행** (반드시 Eureka, Config가 먼저 기동되어야 합니다.)
   - `services:eureka-server`
   - `services:config-server`
   - `services:api-gateway`
   - 이후 도메인 서비스들 실행 (`user-service`, `order-service` 등)

---

## 📚 7. API 문서
시스템이 구동된 후, API Gateway를 거쳐 각 마이크로서비스의 API 명세를 확인할 수 있습니다.
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`


