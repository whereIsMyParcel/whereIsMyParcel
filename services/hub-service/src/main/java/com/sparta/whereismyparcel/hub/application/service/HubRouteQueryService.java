package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.entity.HubRoute;
import com.sparta.whereismyparcel.hub.domain.exception.HubRouteNotFoundException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRouteRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubRouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteQueryService {

    private final HubRouteRepository hubRouteRepository;

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
}
