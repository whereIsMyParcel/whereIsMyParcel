## Company-service

해당 서비스는 업체를 등록하고 업체 직원을 관리하는 서비스를 담당합니다

### company Entity
업체는 특정 허브에 소속되며
해당 허브의 매니저와 마스터만이 업체를 등록할 수 있습니다
업체 등록시 업체 사업자 이름을 통해 초기 매니저인 업체 매니저가 자동 추가 됩니다

업체 타입은 생산, 공급, 통합으로 나눠지며 초기 생성시 NONE타입으로 지정되며
필수적으로 업체 매니저가 수정을 진행해야 합니다

```java
// company entity
public boolean setupRequired() {
    return companyType == CompanyType.NONE;
}
```
재고를 추가하거나 배송을 받을 업체를 지정 시 필수적으로 해당 업체가 setup 설정을 완료했는지 확인하는 로직
false면 허브에 재고를 보관하거나 상품을 받지도 공급하지도 못합니다