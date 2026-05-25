package com.sparta.whereismyparcel.hub.domain.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HubRouteTest {

    private Hub originHub;
    private Hub destinationHub;

    @BeforeEach
    void setUp() {
        originHub = Hub.create("서울 센터", "서울", 37.0, 127.0);
        destinationHub = Hub.create("대전 센터", "대전", 36.0, 127.0);
    }

    @Test
    @DisplayName("정상적인 요청으로 허브 경로를 생성할 수 있다.")
    void createHubRoute_Success() {
        // when
        HubRoute route = HubRoute.create(originHub, destinationHub, 150.5, 120);

        // then
        assertThat(route.getOriginHub()).isEqualTo(originHub);
        assertThat(route.getDestinationHub()).isEqualTo(destinationHub);
        assertThat(route.getDistance()).isEqualTo(150.5);
        assertThat(route.getDuration()).isEqualTo(120);
    }

    @Test
    @DisplayName("출발지와 목적지가 같은 경로 생성 시 예외가 발생한다.")
    void createHubRoute_SameHub_Exception() {
        // when & then
        assertThatThrownBy(() -> HubRoute.create(originHub, originHub, 10.0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같을 수 없습니다");
    }

    @Test
    @DisplayName("거리나 시간이 0 이하인 경로 생성 시 예외가 발생한다.")
    void createHubRoute_InvalidMetrics_Exception() {
        // when & then
        assertThatThrownBy(() -> HubRoute.create(originHub, destinationHub, 0.0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HubRoute.create(originHub, destinationHub, 10.0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("경로 정보를 수정할 수 있다.")
    void updateHubRoute_Success() {
        // given
        HubRoute route = HubRoute.create(originHub, destinationHub, 100.0, 60);

        // when
        route.update(120.0, 80);

        // then
        assertThat(route.getDistance()).isEqualTo(120.0);
        assertThat(route.getDuration()).isEqualTo(80);
    }
}
