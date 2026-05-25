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
        
        // 경로가 생성되면 기존에 계산된 모든 최단 경로 캐시를 무효화해야 함
        evictPathCache(originHub.getHubId(), destinationHub.getHubId());
        
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
        
        // 경로 정보가 수정되면 관련 최단 경로 캐시 무효화
        evictPathCache(route.getOriginHub().getHubId(), route.getDestinationHub().getHubId());
        
        return HubRouteResponse.from(route);
    }

    @Transactional
    @CacheEvict(cacheNames = "hubRoute", key = "#hubRouteId")
    public void deleteHubRoute(UUID hubRouteId, String userId) {
        HubRoute route = hubRouteRepository.findById(hubRouteId)
                .orElseThrow(HubRouteNotFoundException::new);
        route.softDelete(userId);
        
        // 경로 삭제 시 관련 최단 경로 캐시 무효화
        evictPathCache(route.getOriginHub().getHubId(), route.getDestinationHub().getHubId());
    }

    private void evictPathCache(UUID originId, UUID destId) {
        Set<String> keys1 = redisTemplate.keys("path:*" + originId + "*");
        Set<String> keys2 = redisTemplate.keys("path:*" + destId + "*");
        if (keys1 != null) redisTemplate.delete(keys1);
        if (keys2 != null) redisTemplate.delete(keys2);
    }
}
