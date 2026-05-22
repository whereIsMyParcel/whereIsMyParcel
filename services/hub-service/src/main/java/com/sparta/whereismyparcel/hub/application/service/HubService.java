package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubService {

    private final HubRepository hubRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public HubResponse createHub(String name, String address, Double latitude, Double longitude) {
        Hub hub = Hub.create(name, address, latitude, longitude);
        return HubResponse.from(hubRepository.save(hub));
    }

    @Cacheable(cacheNames = "hub", key = "#hubId")
    public HubResponse getHub(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(HubNotFoundException::new);
        return HubResponse.from(hub);
    }

    public Page<HubResponse> getHubs(Pageable pageable) {
        return hubRepository.findAll(pageable)
                .map(HubResponse::from);
    }

    @Transactional
    @CachePut(cacheNames = "hub", key = "#hubId")
    public HubResponse updateHub(UUID hubId, String name, String address, Double latitude, Double longitude) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(HubNotFoundException::new);
        hub.update(name, address, latitude, longitude);
        
        // 허브 정보 변경 시 모든 경로 정보가 틀어질 수 있으므로 캐시 초기화
        evictAllPathCache();
        
        return HubResponse.from(hub);
    }

    @Transactional
    @CacheEvict(cacheNames = "hub", key = "#hubId")
    public void deleteHub(UUID hubId, String userId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(HubNotFoundException::new);
        hub.softDelete(userId);
        
        evictAllPathCache();
    }

    private void evictAllPathCache() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            Set<byte[]> keys = connection.keys("path:*".getBytes());
            if (keys != null && !keys.isEmpty()) {
                connection.del(keys.toArray(new byte[0][]));
            }
            return null;
        });
    }
}
