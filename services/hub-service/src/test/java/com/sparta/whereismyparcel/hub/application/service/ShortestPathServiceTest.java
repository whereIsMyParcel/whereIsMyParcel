package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.entity.HubRoute;
import com.sparta.whereismyparcel.hub.domain.exception.NoPathBetweenHubsException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRepository;
import com.sparta.whereismyparcel.hub.domain.repository.HubRouteRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.response.ShortestPathResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ShortestPathServiceTest {

    @Mock
    private HubRepository hubRepository;
    @Mock
    private HubRouteRepository hubRouteRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ShortestPathService shortestPathService;

    private Hub hubA, hubB, hubC, hubD;
    private List<Hub> allHubs;
    private List<HubRoute> allRoutes;

    @BeforeEach
    void setUp() {
        hubA = Hub.create("Hub A", "addr", 37.0, 127.0);
        hubB = Hub.create("Hub B", "addr", 36.0, 127.0);
        hubC = Hub.create("Hub C", "addr", 35.0, 127.0);
        hubD = Hub.create("Hub D", "addr", 34.0, 127.0);

        allHubs = List.of(hubA, hubB, hubC, hubD);

        HubRoute routeAB = HubRoute.create(hubA, hubB, 10.0, 10);
        HubRoute routeBC = HubRoute.create(hubB, hubC, 20.0, 20);
        HubRoute routeAC = HubRoute.create(hubA, hubC, 50.0, 50);
        HubRoute routeCD = HubRoute.create(hubC, hubD, 30.0, 30);

        allRoutes = List.of(routeAB, routeBC, routeAC, routeCD);
    }

    @Test
    @DisplayName("다익스트라 알고리즘으로 최단 경로를 정확히 계산한다.")
    void getShortestPath_Success() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        
        given(hubRepository.existsById(hubA.getHubId())).willReturn(true);
        given(hubRepository.existsById(hubD.getHubId())).willReturn(true);
        given(hubRouteRepository.findAll()).willReturn(allRoutes);

        // when
        ShortestPathResponse response = shortestPathService.getShortestPath(hubA.getHubId(), hubD.getHubId());

        // then
        assertThat(response.totalDistance()).isEqualTo(60.0);
        assertThat(response.totalDuration()).isEqualTo(60);
        assertThat(response.routes()).hasSize(3);
    }

    @Test
    @DisplayName("경로가 존재하지 않는 경우 예외를 발생시킨다.")
    void getShortestPath_NoPath_Exception() {
        // given
        Hub hubIsolated = Hub.create("Isolated", "addr", 30.0, 120.0);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        
        given(hubRepository.existsById(hubA.getHubId())).willReturn(true);
        given(hubRepository.existsById(hubIsolated.getHubId())).willReturn(true);
        given(hubRouteRepository.findAll()).willReturn(allRoutes);

        // when & then
        assertThatThrownBy(() -> shortestPathService.getShortestPath(hubA.getHubId(), hubIsolated.getHubId()))
                .isInstanceOf(NoPathBetweenHubsException.class);
    }

    @Test
    @DisplayName("출발지와 목적지가 같으면 즉시 빈 경로를 반환한다.")
    void getShortestPath_SameHub() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(hubRepository.existsById(hubA.getHubId())).willReturn(true);

        // when
        ShortestPathResponse response = shortestPathService.getShortestPath(hubA.getHubId(), hubA.getHubId());

        // then
        assertThat(response.totalDistance()).isEqualTo(0.0);
        assertThat(response.routes()).isEmpty();
    }
}
