package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.entity.HubRoute;
import com.sparta.whereismyparcel.hub.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRepository;
import com.sparta.whereismyparcel.hub.domain.repository.HubRouteRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.request.CreateHubRouteRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.request.UpdateHubRouteRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubRouteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HubRouteCommandServiceTest {

    @InjectMocks
    private HubRouteCommandService hubRouteCommandService;

    @Mock
    private HubRouteRepository hubRouteRepository;

    @Mock
    private HubRepository hubRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("허브 경로 생성 성공")
    void createHubRoute_Success() {
        UUID originId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        Hub originHub = Hub.create("출발", "주소", 37.0, 127.0);
        Hub destHub = Hub.create("도착", "주소", 38.0, 128.0);
        CreateHubRouteRequest request = new CreateHubRouteRequest(originId, destId, 1500.0, 120);

        given(hubRepository.findById(originId)).willReturn(Optional.of(originHub));
        given(hubRepository.findById(destId)).willReturn(Optional.of(destHub));

        HubRoute savedRoute = HubRoute.create(originHub, destHub, 1500.0, 120);
        given(hubRouteRepository.save(any(HubRoute.class))).willReturn(savedRoute);

        HubRouteResponse response = hubRouteCommandService.createHubRoute(request);

        assertThat(response.distance()).isEqualTo(1500);
        assertThat(response.duration()).isEqualTo(120);
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("경로 생성 시 허브가 없으면 예외 발생")
    void createHubRoute_HubNotFound() {
        UUID originId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        CreateHubRouteRequest request = new CreateHubRouteRequest(originId, destId, 1500.0, 120);

        given(hubRepository.findById(originId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> hubRouteCommandService.createHubRoute(request))
                .isInstanceOf(HubNotFoundException.class);
    }

    @Test
    @DisplayName("허브 경로 수정 및 캐시 삭제 호출 성공")
    void updateHubRoute_Success() {
        UUID routeId = UUID.randomUUID();
        Hub originHub = Hub.create("출발", "주소", 37.0, 127.0);
        Hub destHub = Hub.create("도착", "주소", 38.0, 128.0);
        HubRoute route = HubRoute.create(originHub, destHub, 1000.0, 60);

        given(hubRouteRepository.findById(routeId)).willReturn(Optional.of(route));
        UpdateHubRouteRequest request = new UpdateHubRouteRequest(2000.0, 150);

        HubRouteResponse response = hubRouteCommandService.updateHubRoute(routeId, request);

        assertThat(response.distance()).isEqualTo(2000);
        assertThat(response.duration()).isEqualTo(150);
        assertThat(route.getDistance()).isEqualTo(2000);
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("허브 경로 삭제 시 소프트 딜리트 처리 및 캐시 삭제 호출 성공")
    void deleteHubRoute_Success() {
        UUID routeId = UUID.randomUUID();
        Hub originHub = Hub.create("출발", "주소", 37.0, 127.0);
        Hub destHub = Hub.create("도착", "주소", 38.0, 128.0);
        HubRoute route = HubRoute.create(originHub, destHub, 1000.0, 60);

        given(hubRouteRepository.findById(routeId)).willReturn(Optional.of(route));

        hubRouteCommandService.deleteHubRoute(routeId, "user-123");

        assertThat(route.isDeleted()).isTrue();
        assertThat(route.getDeletedBy()).isEqualTo("user-123");
        verify(redisTemplate).execute(any(RedisCallback.class));
    }
}
