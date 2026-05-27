package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HubCommandService {

    private final HubRepository hubRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public HubResponse createHub(String name, String address, Double latitude, Double longitude) {
        Hub hub = Hub.create(name, address, latitude, longitude);
        return HubResponse.from(hubRepository.save(hub));
    }

    @CachePut(cacheNames = "hub", key = "#hubId")
    public HubResponse updateHub(UUID hubId, String name, String address, Double latitude, Double longitude) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(HubNotFoundException::new);
        hub.update(name, address, latitude, longitude);
        
        // 허브 정보 변경 시 모든 경로 정보가 틀어질 수 있으므로 캐시 초기화
        evictAllPathCache();
        
        return HubResponse.from(hub);
    }

    @CacheEvict(cacheNames = "hub", key = "#hubId")
    public void deleteHub(UUID hubId, String userId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(HubNotFoundException::new);
        hub.softDelete(userId);
        
        evictAllPathCache();
    }

    private void evictAllPathCache() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(org.springframework.data.redis.core.ScanOptions.scanOptions().match("path:*").count(100).build())) {
                while (cursor.hasNext()) {
                    connection.del(cursor.next());
                }
            } catch (Exception e) {
                // ignore
            }
            return null;
        });
    }
}
