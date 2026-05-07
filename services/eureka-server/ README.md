# Eureka Server 사용 가이드
본 애플리케이션은 Where Is My Parcel 프로젝트의 Service Discovery(Eureka) 서버입니다. 

모든 마이크로서비스는 이 서버에 자신의 위치를 등록하고, 서로의 위치를 찾아 통신합니다.

---

## 0. 간편 체크리스트 (클라이언트 담당자용)

새로운 마이크로서비스를 만들 때 아래 사항을 반드시 적용해주세요.

- `spring-cloud-starter-netflix-eureka-client` 의존성 추가

- application.yml에 유레카 서버 주소 설정

- Instance ID 설정 (중복 등록 및 식별 방지)

---

## 1. 역할

모든 마이크로서비스(Client)의 IP와 포트 번호를 동적으로 관리합니다.

Service Registration: 서비스가 시작될 때 유레카 서버에 자신의 정보를 등록합니다.

Service Discovery: 다른 서비스의 위치(IP)가 필요할 때 유레카 서버에 물어봅니다.

## 2. Eureka Client 설정 방법
   
### 의존성 추가
```gradle
dependencies {
implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'

dependencies {
implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
}
```

### application.yml 설정
```yaml
spring:
  application:
    name: 서비스-이름 # 예: user-service (반드시 소문자 권장)

eureka:
  instance:
    # 1. 호스트 이름 대신 IP 주소로 등록 (안정성)
    prefer-ip-address: true
    # 2. 대시보드에서 식별하기 위한 고유 ID (이름 + 랜덤값)
    instance-id: ${spring.application.name}:${spring.application.instance_id:${random.value}}
    
  client:
    # 유레카 서버 접속 주소
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
    # 서버로부터 다른 서비스 목록을 30초마다 갱신해서 가져옴
    fetch-registry: true
    # 나 자신을 서버에 등록함
    register-with-eureka: true
```

---

## 3. 대시보드 확인 (Monitoring)
서버를 실행한 후 브라우저에서 아래 주소에 접속하면 현재 등록된 모든 서비스 현황을 한눈에 볼 수 있습니다.

URL: http://localhost:8761

확인 사항:

- Instances currently registered: 여기에 본인이 만든 서비스의 이름(UP 상태)이 떠야 정상입니다.

- Status: 설정한 instance-id가 제대로 표시되는지 확인하세요.

## 4. 로컬 테스트 및 주의사항
### 실행 순서
- 가장 안정적인 실행 순서는 다음과 같습니다.

1. Eureka Server (8761)

2. Config Server (8888)

3. API Gateway & Microservices

### 헬스 체크
유레카 서버 자체가 정상인지 확인하려면 아래 엔드포인트를 호출하세요.

```Bash
curl http://localhost:8761/actuator/health
```

### 서버 설정 특징 (Server-Side)
유레카 서버 본체(eureka-server)는 서버 역할만 수행하므로 자기 자신을 등록하지 않도록 설정되어 있습니다.

- register-with-eureka: false

- fetch-registry: false

### 자기보호모드 안내
- 현재 로컬 개발 편의를 위해 Self-Preservation 모드는 꺼져 있습니다.

- 서비스를 종료하면 유레카 대시보드에서 즉시 제거되므로, 테스트 시 참고하세요.

- 운영 환경 배포 시에는 네트워크 순변동에 대비해 이 기능을 다시 활성화할 예정입니다.

>> **로컬에서 꺼놓는 이유**
> 
> 로컬에서 개발시 서비스를 껐다 켰다 하거나 강제종료하는 경우가 생김
> - 모드 켜있을 시 : 서비스를 종료했는데도 유레카 대시보드에는 한참 동안 UP으로 남아있습니다. 다른 서비스들이 죽은 줄 모르고 호출했다가 에러가 발생할 수 있습니다.
> - 모드 꺼져있을 시 : 신호가 안 오면 즉시 목록에서 지워버립니다. 

**eureka server**
```yaml
eureka:
  server:
    # 자기 보호 모드 비활성화 (기본값은 true)
    enable-self-preservation: false
    # 서비스가 신호를 안 보낼 때 목록에서 지우는 주기 (로컬용으로 짧게 설정 가능)
    eviction-interval-timer-in-ms: 3000
```

**eureka client (service)**
```yaml
eureka:
  instance:
    # 유레카 서버에게 5초마다 신호를 보냄 (기본 30초)
    lease-renewal-interval-in-seconds: 30
    # 10초 동안 신호가 없으면 나를 죽은 것으로 간주하라고 요청 (기본 90초)
    lease-expiration-duration-in-seconds: 90
```