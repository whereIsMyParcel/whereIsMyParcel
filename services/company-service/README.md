# Company-service

- 해당 서비스는 `Company`, `Product`, `Inventory`의 핵심 비즈니스 로직을 관장합니다.

## 아키텍처 설계

- 현재 단계에서는 시스템 복잡도를 낮추고 개발 생산성을 위해 하나의 어플리케이션 서버(모듈)내에 세 도메인을 배치하되, DB 스키마는 분리해놨습니다.
- JPA의 내부 객체 연관관계를 설정하였지만, 향후 완전한 MSA 분리시 서비스간 결합도를 낮추기 위해 서비스 레이어를 분리하여 작성했습니다.
- Microservice Architecture(MSA) 환경에서 타 서비스와의 정밀한 데이터 동기화를 위해 `Feign Client` 통신을 적극적으로 활용합니다.

## 주요 도메인 및 핵심 기능

### 1. Company

외부 유저 서비스와의 유기적인 데이터 동기화를 위해 Feign Client 통신을 활용하여 소속 및 권한을 제어합니다.

- 업체(Company) 등록/삭제: `MASTER`, `HUB_MANAGER` 권한 보유자만 가능합니다. 등록 시 사업자 번호를 기반으로 유저 서비스와 Feign 통신을 통해 마스터 멤버 ID를 동기화하며, 삭제
  시 관련 유저들의 소속 정보를 연쇄 해제(Null 처리)합니다.

- 업체 직원(CompanyMember) 관리: 업체 매니저는 소속 직원을 추가할 수 있습니다. 이때 등록할 유저가 이미 타 허브나 타 업체에 소속되어 있는지 Feign 통신 검증 프로세스를 거쳐 소속 중복을 원천
  차단합니다.

### 2. Product

하나의 상품이 가질 수 있는 수많은 옵션과 최종 구매 단위인 베리언트(Variant)를 객체지향적 구조로 관리합니다.

```
Product
   │
   ├─────────────────── (1:N) ───────────────────► ProductOption 
   │                                                     │
   │                                                   (1:N)
   │                                                     ▼
   │                                             ProductOptionValue 
   │                                                     │
   │                                                   (1:N)
   │                                                     ▼
   └─── (1:N) ───► ProductVariant  ◄── (N:1) ── ProductVariantOption 
                                                 
```

### [엔티티별 역할 및 핵심 설계 철학]

- **Product (상품)**

    - 상품 도메인의 최상위 Root Aggregate입니다. 상품명, 설명, 기본 가격을 가집니다.

- **ProductOption (옵션)**

    - 상품과 옵션값 사이를 중재하며, 옵션의 이름(예: `사이즈`, `색상`)을 가집니다.

    - **[설계 철학 - 상태 및 수정 기능 제거]**: 단순 화면 조회 시 껍데기 역할만 수행하므로 불필요한 상태(Status) 필드를 제거하고 수정이 불가능하도록 설계하여 데이터 정밀도와 조회 성능을
      향상시켰습니다.

- **ProductOptionValue (옵션 값)**

    - 실제 옵션의 알맹이 데이터(예: `블랙`, `260`)와 해당 옵션 선택 시 기본가에 추가되는 추가 금액(additionalPrice)을 관리합니다.

    - 판매 중지(INACTIVE) 및 재개(ACTIVE) 상태를 가집니다.

- **ProductVariantOption (조합 매핑 엔티티)**

    - ProductVariant와 ProductOptionValue 사이의 다리 역할을 하는 다대다(N:M) 해소용 중간 테이블입니다.

- **ProductVariant (옵션 조합/베리언트)**

    - 물류 및 재고 조회 시 호출되는 최종 판매 단위입니다. 상품명과 옵션값들이 조립된 최종 이름(예: `나이키 에어포스 (블랙 / 260)`)과 최종 가격(`기본가 + 추가 금액의 합`)을 가집니다.

### [핵심 기능: 재귀 기반의 자동 상품 등록 프로세스]

상품을 처음 등록할 때, 관리자가 일일이 수십 개의 옵션 조합을 손으로 입력하는 것은 비효율적이며 데이터 오류를 야기합니다. 이를 해결하기 위해 ProductService의 registerProduct 메서드는 재귀
알고리즘을 활용한 복합 옵션 빌더 구조로 구현되었습니다.

- **동작 메커니즘 (How it works)**

    1. 최상위 부모 생성: 입력받은 상품 기본 정보로 Product 엔티티를 영속화합니다.

    2. 옵션 트리 빌드: 요청으로 들어온 옵션 그룹(ex: 색상:[블랙, 화이트], 사이즈:[260, 270])을 순회하며 ProductOption과 ProductOptionValue를 먼저 데이터베이스에 안착시킵니다.

    3. 재귀적 조합 생성 (Combination):

        - 생성된 옵션 그룹 리스트의 depth를 타고 들어가며 수학적 카테시안 곱(Cartesian Product) 연산을 재귀 함수로 돌립니다.

        - [블랙, 화이트] 와 [260, 270] 이 들어온다면 재귀 스택을 통해 아래와 같이 총 4개의 조합을 유연하게 도출합니다.
    
            - 블랙 -> 260 픽 (조합 1 완료)
    
            - 블랙 -> 270 픽 (조합 2 완료)
    
            - 화이트 -> 260 픽 (조합 3 완료)
    
            - 화이트 -> 270 픽 (조합 4 완료)


    4. 베리언트 자동 생성 및 돈 계산:

        - 추출된 조합을 바탕으로 ProductVariant를 자동으로 생성합니다.
    
        - 이때, 상품 본체의 기본 가격과 재귀를 타고 내려오면서 합산된 옵션값들의 additionalPrice를 자동으로 합산하여 최종 베리언트 판매 가격(variantPrice)을 스스로 계산해 냅니다.
    
        - 이름 또한 상품명 + (옵션값 / 옵션값) 형태로 자동 빌드되어 ProductVariantOption(행정병) 매핑 테이블과 함께 안전하게 저장됩니다.

### 재귀 방식을 선택한 이유

만약 옵션이 2개(색상, 사이즈)로 고정되어 있다면 단순한 2중 for문으로 해결할 수 있습니다. 하지만 어떤 상품은 옵션이 없을 수도 있고, 어떤 상품은 옵션이 4개(색상, 사이즈, 기장, 로고유무)가 될 수도
있습니다.

재귀 알고리즘을 채택함으로써 옵션의 개수가 몇 개가 들어오든 코드 한 줄 수정 없이 동적으로 모든 N차원 옵션 조합을 완벽하게 계산하고 베리언트를 뽑아낼 수 있는 확장성을 확보했습니다.

### 3. Inventory

- 비관적 락(Pessimistic Lock) 기반 동시성 제어: 주문 인입 시 발생할 수 있는 동시성 이슈 및 데이터 적합성을 보장하기 위해 엔티티 레벨에 비관적 락(PESSIMISTIC_WRITE)을
  적용했습니다.
- 데드락(Deadlock) 방어 전략: 다중 상품 묶음 주문 시 자원 선점 순서가 일치하지 않아 발생하는 순환 대기(Circular Wait) 문제를 해결하기 위해, 로직 최상단에서 요청 아이템을 skuCode
  순으로 강제 정렬(Lock Ordering)하도록 구현했습니다. 추가로 재시도 매커니즘 적용했습니다.

## 보안 및 권한 (Security & Authorization)

- 인증 방식: API Gateway를 거쳐 인입되는 Header의 X-User-Role 및 X-User-Id를 기반으로 권한을 검증합니다.

- 권한 정책:

    - `MASTER` / `HUB_MANAGER`: 업체 등록/삭제, 소속 직원 삭제 권한 보유

    - `COMPANY_MANAGER`: 본인 소속 업체의 직원 등록 및 상품/재고 관리 권한 보유

## API 엔트포인트 명세

**Swagger UI** (`http://localhost:8083/swagger-ui/index.html`)

- Company

| Method   | URL                                               | Description           | Required Role / Header                      |
|:---------|:--------------------------------------------------|:----------------------|:--------------------------------------------|
| `POST`   | `/api/v1/companies`                               | 신규 업체 등록              | `MASTER`, `HUB_MANAGER`                     |
| `GET`    | `/api/v1/companies/{companyId}`                   | 특정 업체 단건 조회           | `ALL`                                       |
| `GET`    | `/api/v1/companies`                               | 전체 업체 목록 조회 (Paging)  | `ALL`                                       |
| `PATCH`  | `/api/v1/companies/{companyId}`                   | 업체 정보 수정              | `COMPANY_MANAGER`                           |
| `DELETE` | `/api/v1/companies/{companyId}`                   | 특정 업체 삭제              | `MASTER`, `HUB_MANAGER` / Header: X-User-Id |
| `POST`   | `/api/v1/companies/{companyId}/member`            | 업체 소속 멤버(직원) 등록       | `COMPANY_MANAGER`                           |
| `GET`    | `/api/v1/companies/{companyId}/member/{memberId}` | 업체 멤버 단건 조회           | `ALL`                                       |
| `GET`    | `/api/v1/companies/{companyId}/member`            | 업체별 멤버 목록 조회 (Paging) | `ALL`                                       |
| `DELETE` | `/api/v1/companies/{companyId}/member`            | 업체 멤버 삭제(해제)          | `COMPANY_MANAGER` / Header: X-User-Id       |

- Product

| Method   | URL                                                                | Description                 | Required Role / Header                 |
|:---------|:-------------------------------------------------------------------|:----------------------------|:---------------------------------------|
| `POST`   | `/api/v1/products`                                                 | 재귀 기반 상품 및 베리언트 자동 등록       | `COMPANY_MANAGER`                      |                                         
| `GET`    | `/api/v1/products/{productId}`                                     | 상품 상세 정보 조회                 | `ALL`                                  |                                   
| `GET`    | `/api/v1/products`                                                 | 상품 목록 페이징 조회                | `ALL`                                  |                                                 
| `GET`    | `/api/v1/products/{productId}/variants`                            | 상품의 최종 옵션 조합(Variant) 목록 조회 | `ALL`                                  |                          
| `PATCH`  | `/api/v1/products/{productId}`                                     | 상품 기본 정보 수정                 | `COMPANY_MANAGER`                      |                      
| `PATCH`  | `/api/v1/products/{productId}/optionValues/{optionValueId}`        | 상품 특정 옵션값 내용 수정             | `COMPANY_MANAGER`                      |              
| `PATCH`  | `/api/v1/products/{productId}/status`                              | 상품 전체 판매 상태 변경              | `COMPANY_MANAGER`                      |                                     
| `PATCH`  | `/api/v1/products/{productId}/optionValues/{optionValueId}/status` | 특정 옵션값 활성화/비활성화 상태 변경       | `COMPANY_MANAGER`                      |
| `DELETE` | `/api/v1/products/{productId}`                                     | 상품 논리 삭제                    | `COMPANY_MANAGER`  / Header: X-User-Id |                                
| `DELETE` | `/api/v1/products/{productId}/optionValues/{optionValueId}`        | 특정 상품 옵션 삭제                 | `COMPANY_MANAGER`  / Header: X-User-Id |

- Inventory

| Method | URL                                       | Description       | Required Role / Header |
|:-------|:------------------------------------------|:------------------|:-----------------------|
| `POST` | `/api/v1/inventory`                       | 상품 베리언트별 초기 재고 등록 | `COMPANY_MANAGER`      |
| `GET`  | `/api/v1/inventory?productVariantId={id}` | 특정 상품 베리언트의 재고 확인 | `ALL`                  |

## 기술 스택 (Tech Stack)

- Framework: Spring Boot 3.x
- Database: PostgreSQL (격리된 company_db, product_db, inventory_db 스키마 사용)
- ORM: Spring Data JPA (Hibernate 6.x)
- Network Communication: Spring Cloud OpenFeign (유저 서비스 연동)
- Service Discovery: Spring Cloud Netflix Eureka Client
- API Docs: Springdoc OpenAPI (Swagger UI)