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
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HubRouteCommandService {

    private final HubRouteRepository hubRouteRepository;
    private final HubRepository hubRepository;
    private final RedisTemplate<String, Object> redisTemplate;

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

    @CachePut(cacheNames = "hubRoute", key = "#hubRouteId")
    public HubRouteResponse updateHubRoute(UUID hubRouteId, UpdateHubRouteRequest request) {
        HubRoute route = hubRouteRepository.findById(hubRouteId)
                .orElseThrow(HubRouteNotFoundException::new);
        route.update(request.distance(), request.duration());
        
        evictAllPathCache();
        
        return HubRouteResponse.from(route);
    }

    @CacheEvict(cacheNames = "hubRoute", key = "#hubRouteId")
    public void deleteHubRoute(UUID hubRouteId, String userId) {
        HubRoute route = hubRouteRepository.findById(hubRouteId)
                .orElseThrow(HubRouteNotFoundException::new);
        route.softDelete(userId);
        
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
