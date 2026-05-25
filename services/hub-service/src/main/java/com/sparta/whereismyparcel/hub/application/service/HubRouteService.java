package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.entity.HubRoute;
import com.sparta.whereismyparcel.hub.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.hub.domain.exception.HubRouteNotFoundException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRepository;
import com.sparta.whereismyparcel.hub.domain.repository.HubRouteRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.request.CreateHubRouteRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.request.UpdateHubRouteRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubRouteResponse;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteService {

    private final HubRouteRepository hubRouteRepository;
    private final HubRepository hubRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public HubRouteResponse createHubRoute(CreateHubRouteRequest request) {
        Hub originHub = hubRepository.findById(request.originHubId())
                .orElseThrow(HubNotFoundException::new);
        Hub destinationHub = hubRepository.findById(request.destinationHubId())
                .orElseThrow(HubNotFoundException::new);

        HubRoute route = HubRoute.create(originHub, destinationHub, request.distance(), request.duration());
        
        // [최적화] 경로 변경 시 모든 최단 경로 캐시 초기화 (SMALL_V=17이므로 전체 삭제가 안전)
        evictAllPathCache();
        
        return HubRouteResponse.from(hubRouteRepository.save(route));
    }

    @Cacheable(cacheNames = "hubRoute", key = "#hubRouteId")
    public HubRouteResponse getHubRoute(UUID hubRouteId) {
        HubRoute route = hubRouteRepository.findById(hubRouteId)
                .orElseThrow(HubRouteNotFoundException::new);
        return HubRouteResponse.from(route);
    }

    public Page<HubRouteResponse> getHubRoutes(Pageable pageable) {
        return hubRouteRepository.findAll(pageable)
                .map(HubRouteResponse::from);
    }

    @Transactional
    @CachePut(cacheNames = "hubRoute", key = "#hubRouteId")
    public HubRouteResponse updateHubRoute(UUID hubRouteId, UpdateHubRouteRequest request) {
        HubRoute route = hubRouteRepository.findById(hubRouteId)
                .orElseThrow(HubRouteNotFoundException::new);
        route.update(request.distance(), request.duration());
        
        evictAllPathCache();
        
        return HubRouteResponse.from(route);
    }

    @Transactional
    @CacheEvict(cacheNames = "hubRoute", key = "#hubRouteId")
    public void deleteHubRoute(UUID hubRouteId, String userId) {
        HubRoute route = hubRouteRepository.findById(hubRouteId)
                .orElseThrow(HubRouteNotFoundException::new);
        route.softDelete(userId);
        
        evictAllPathCache();
    }

    private void evictAllPathCache() {
        // SCAN 대신, 허브 수가 적고 정합성이 중요하므로 prefix 기반 전체 삭제를 권장함 (또는 버전 관리)
        // 여기서는 keys() 대신 connection 수준의 작업을 고려하거나, 
        // Small dataset 이므로 명확하게 전체 삭제를 수행함.
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
