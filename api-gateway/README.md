# API Gateway Module

이 모듈은 Spring Cloud Gateway를 기반으로 구축된 API Gateway입니다. 마이크로서비스 아키텍처에서 모든 클라이언트 요청의 단일 진입점 역할을 하며, 다음과 같은 주요 기능을 수행합니다:

*   **라우팅 (Routing):** 들어오는 요청을 적절한 백엔드 서비스로 전달합니다.
*   **필터링 (Filtering):** 요청 전/후에 인증, 권한 부여, 로깅, 속도 제한 등과 같은 횡단 관심사(cross-cutting concerns)를 처리합니다.
*   **로드 밸런싱 (Load Balancing):** 여러 인스턴스로 구성된 백엔드 서비스에 요청을 분산합니다.(현재 서비스가 없어 로드밸런싱은 없습니다.)
*   **보안 (Security):** 중앙 집중식으로 보안 정책을 적용합니다.

## Getting Started

### Build and Run

(여기에 프로젝트의 빌드 및 실행 방법에 대한 구체적인 지침을 추가할 수 있습니다. 예: `mvn clean install`, `java -jar target/apiGateway-0.0.1-SNAPSHOT.jar` 등)

### Configuration

환경별 설정은 `src/main/resources/application-{profile}.yml` 파일을 참조하십시오.

---

## 추가 가이드

### 1. `application.yaml` 파일 관리 (주석 처리 부분 정리)

`application.yaml` 파일은 Spring Boot 애플리케이션의 핵심 설정 파일입니다. 효과적인 관리를 위해 다음과 같은 방법을 권장합니다:

*   **기본 `application.yaml`:** 모든 환경에 공통적으로 적용되는 설정을 포함합니다. 민감한 정보나 환경별로 달라지는 설정은 여기에 직접 넣지 않도록 합니다.
*   **프로파일별 파일 (`application-{profile}.yaml`):** 개발(dev), 테스트(test), 운영(prod) 등 특정 환경에만 적용되는 설정을 별도의 파일로 분리합니다. 예를 들어, `application-dev.yaml`, `application-prod.yaml` 등이 있습니다.
    *   **활성화:** `spring.profiles.active` 속성을 통해 활성화할 프로파일을 지정하거나, 애플리케이션 실행 시 `-Dspring.profiles.active=prod` 또는 `--spring.profiles.active=prod` 옵션을 사용합니다.
*   **주석 처리 최소화:** 사용하지 않는 설정이나 임시 설정은 주석 처리하기보다는, 필요 없으면 삭제하거나 별도의 문서에 기록하는 것이 좋습니다. 주석이 많아지면 파일의 가독성이 떨어지고 혼란을 야기할 수 있습니다.
*   **환경 변수 활용:** 데이터베이스 비밀번호, API 키 등 민감한 정보는 `application.yaml`에 직접 노출하기보다 환경 변수를 통해 주입받는 것이 보안상 안전합니다.

### 2. WebFlux vs Discovery: 게이트웨이의 구동 엔진과 위치 기반 지도

Spring Cloud Gateway는 두 가지 핵심 기술의 상호작용으로 강력한 기능을 제공합니다.

*   **WebFlux (게이트웨이의 구동 엔진):**
    *   Spring Cloud Gateway는 Spring WebFlux 위에 구축됩니다. WebFlux는 비동기(asynchronous) 및 논블로킹(non-blocking) 방식으로 동작하는 리액티브(reactive) 웹 프레임워크입니다.
    *   이는 게이트웨이가 적은 수의 스레드로도 높은 동시성 요청을 효율적으로 처리할 수 있게 하여, 뛰어난 성능과 확장성을 제공합니다.
    *   모든 요청 처리는 논블로킹 I/O 모델을 따르므로, 백엔드 서비스의 응답을 기다리는 동안 다른 요청을 처리할 수 있습니다.

*   **Discovery (위치 기반 지도):**
    *   Discovery Service (예: Eureka, Consul, Nacos)는 마이크로서비스 아키텍처에서 서비스 인스턴스들의 등록 및 검색을 담당합니다. 각 마이크로서비스는 시작 시 자신의 네트워크 위치(IP 주소, 포트)를 Discovery Service에 등록합니다.
    *   API Gateway는 이 Discovery Service를 "위치 기반 지도"처럼 활용하여, 들어오는 요청의 대상 서비스 이름을 실제 서비스 인스턴스의 네트워크 주소로 변환합니다.
    *   이를 통해 게이트웨이는 백엔드 서비스의 물리적 위치를 하드코딩할 필요 없이, 서비스 이름만으로 동적으로 라우팅할 수 있습니다. 서비스 인스턴스가 추가되거나 제거되어도 게이트웨이 설정 변경 없이 유연하게 대응 가능합니다.

*   **상호작용:**
    *   클라이언트 요청이 API Gateway에 도착하면, WebFlux 기반의 게이트웨이 라우팅 로직이 요청을 처리합니다.
    *   이때, 라우팅 규칙에 서비스 이름이 포함되어 있다면, 게이트웨이는 Discovery Service에 해당 서비스 이름으로 등록된 인스턴스들의 목록을 조회합니다.
    *   조회된 인스턴스 중 하나를 선택하여 (로드 밸런싱 정책에 따라) 요청을 포워딩합니다. 이 모든 과정은 WebFlux의 논블로킹 방식으로 효율적으로 이루어집니다.

### 3. Controller 설정: `/actuator/health`와 `management.endpoint.health.show-details: always`의 차이

현재 `apiGateway` 모듈의 `Controller.java`에 구현된 `/actuator/health` 엔드포인트와 Spring Boot Actuator가 제공하는 헬스 체크 기능은 목적과 제공하는 정보의 깊이에서 차이가 있습니다.

*   **현재 `Controller.java`의 `/actuator/health`:**
    *   **목적:** 단순히 API Gateway 애플리케이션이 "살아있다(UP)"는 것을 외부에 알리는 가장 기본적인 헬스 체크입니다.
    *   **구현 방식:** 개발자가 직접 `RestController`를 통해 `/actuator/health` 경로에 `GET` 매핑을 하고, 고정된 응답 (`{"status": "UP"}`)을 반환하도록 구현했습니다.
    *   **정보 수준:** 매우 제한적입니다. 애플리케이션이 실행 중이라는 것 외에는 어떤 내부 컴포넌트의 상태도 알 수 없습니다.

*   **Spring Boot Actuator의 `management.endpoint.health.show-details: always`:**
    *   **목적:** 애플리케이션의 전반적인 건강 상태를 상세하게 모니터링하고 진단하기 위함입니다.
    *   **구현 방식:** Spring Boot Actuator 의존성을 추가하고 `application.yaml`에 `management.endpoint.health.show-details: always`와 같은 설정을 추가하면, Spring Boot가 자동으로 `/actuator/health` 엔드포인트를 제공합니다.
    *   **정보 수준:** `always`로 설정하면, 데이터베이스 연결 상태, 디스크 공간, 메시지 큐 연결, Redis 연결 등 애플리케이션이 의존하는 다양한 컴포넌트들의 상세한 건강 상태를 JSON 형태로 제공합니다. 이는 운영 환경에서 애플리케이션의 문제를 진단하고 모니터링 시스템과 연동하는 데 필수적입니다.
    *   **예시 응답 (일부):**
        ```json
        {
            "status": "UP",
            "components": {
                "db": {
                    "status": "UP",
                    "details": {
                        "database": "H2",
                        "validationQuery": "isValid()"
                    }
                },
                "diskSpace": {
                    "status": "UP",
                    "details": {
                        "total": 250790436864,
                        "free": 150000000000,
                        "threshold": 10485760
                    }
                }
                // ... 다른 컴포넌트들
            }
        }
        ```

*   **차이점 요약:**
    *   **자동 vs 수동:** Actuator는 자동, 현재 Controller는 수동 구현.
    *   **정보의 깊이:** Actuator는 상세한 내부 컴포넌트 상태 제공, Controller는 단순 "UP" 상태만 제공.
    *   **활용성:** Actuator는 모니터링 시스템 연동 및 문제 진단에 적합, Controller는 최소한의 생존 확인용.

**결론:** 프로덕션 환경에서는 Spring Boot Actuator의 헬스 체크 기능을 사용하는 것이 강력히 권장됩니다. 현재 `Controller.java`의 구현은 Actuator가 활성화되지 않았거나, 매우 특수한 상황에서만 사용될 수 있습니다. Actuator를 사용한다면 현재 `Controller.java`의 `/actuator/health` 매핑은 제거하는 것이 좋습니다.

#### API Gateway Controller 역할?
게이트웨이 자체가 직접 응답을 줘야 할 때만 존재

* Health Check 커스텀: /actuator/health 외에 서버의 상태를 더 상세하게 커스텀해서 보여주고 싶을 때.

* 점검 페이지: 서비스 전체 점검 중일 때, 모든 요청을 막고 "현재 점검 중입니다"라는 메시지를 직접 응답할 때.

* API 문서 통합: Swagger 등을 사용하여 각 서비스의 API 명세서를 게이트웨이에서 하나로 모아서 보여주는 페이지를 만들 때.
