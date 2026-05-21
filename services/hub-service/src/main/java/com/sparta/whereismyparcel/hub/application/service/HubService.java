package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.request.CreateHubRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.request.UpdateHubRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubService {

    private final HubRepository hubRepository;

    @Transactional
    public HubResponse createHub(CreateHubRequest request) {
        Hub hub = Hub.create(request);
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
    public HubResponse updateHub(UUID hubId, UpdateHubRequest request) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(HubNotFoundException::new);
        hub.update(request);
        return HubResponse.from(hub);
    }

    @Transactional
    @CacheEvict(cacheNames = "hub", key = "#hubId")
    public void deleteHub(UUID hubId, String userId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(HubNotFoundException::new);
        hub.softDelete(userId);
    }
}
