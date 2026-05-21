4# ⚡ Redis 캐싱 전략 및 설정 (Step 5)

## 1. 구현 내용
- **CacheConfig**: Spring Cache 추상화와 `RedisCacheManager`를 설정했습니다.
- **RedisTemplate**: 복잡한 캐시 패턴 삭제 및 동적 TTL(Jitter) 적용을 위해 `RedisTemplate<String, Object>`를 빈으로 등록했습니다.
- **TTL 설정**:
  - `hub`: 허브 단건 조회용 (TTL 1시간)
  - `path`: 다익스트라 경로 탐색 결과용 (TTL 6시간)

## 2. 왜 이 방식을 선택했는가 (설계 의도)
- **Cache Stampede 방지**: 기본 TTL 외에 비즈니스 로직(Dijkstra)에서 `RedisTemplate`을 사용할 때 0~10분의 Jitter를 추가하여 대규모 캐시 동시 만료 현상을 방지하도록 설계했습니다.
- **직렬화 최적화**: `GenericJackson2JsonRedisSerializer`를 사용하여 객체를 JSON 형태로 저장, Redis CLI에서도 데이터를 쉽게 확인할 수 있도록 했습니다.
- **Null 캐싱 방지**: 존재하지 않는 데이터에 대한 반복적인 DB 접근을 막기 위해 `disableCachingNullValues()` 설정을 고려했으나, 상황에 따라 `Cacheable` 어노테이션에서 세부 조정할 예정입니다.

## 3. 캐시 키 규칙
- 허브 단건: `hub::{hubId}`
- 경로 결과: `path::{originHubId}:{destinationHubId}`

## 4. 주의사항 및 한계
- Redis 서버가 다운될 경우 애플리케이션에 영향이 없도록 예외 처리가 필요할 수 있습니다. (현재는 기본 설정 유지)

## 5. 팀원 공유 사항
- 캐시 삭제가 필요한 작업(허브 정보 수정/삭제) 발생 시 반드시 `@CacheEvict` 또는 `redisTemplate`을 사용하여 캐시 무결성을 유지해야 합니다.
