package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubResponse;
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
public class HubQueryService {

    private final HubRepository hubRepository;

    @Cacheable(cacheNames = "hub", key = "#hubId")
    public HubResponse getHub(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(HubNotFoundException::new);
        return HubResponse.from(hub);
    }

    public boolean existsHub(UUID hubId) {
        return hubRepository.existsById(hubId);
    }

    public Page<HubResponse> getHubs(Pageable pageable) {
        return hubRepository.findAll(pageable)
                .map(HubResponse::from);
    }
}
