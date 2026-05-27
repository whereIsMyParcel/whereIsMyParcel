package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.exception.HubRouteNotFoundException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRouteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HubRouteQueryServiceTest {

    @InjectMocks
    private HubRouteQueryService hubRouteQueryService;

    @Mock
    private HubRouteRepository hubRouteRepository;

    @Test
    @DisplayName("ID로 허브 경로 조회 실패 시 예외 발생")
    void getHubRoute_NotFound() {
        UUID routeId = UUID.randomUUID();
        given(hubRouteRepository.findById(routeId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> hubRouteQueryService.getHubRoute(routeId))
                .isInstanceOf(HubRouteNotFoundException.class);
    }
}
