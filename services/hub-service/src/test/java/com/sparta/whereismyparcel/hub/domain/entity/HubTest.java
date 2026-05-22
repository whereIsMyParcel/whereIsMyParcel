package com.sparta.whereismyparcel.hub.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HubTest {

    @Test
    @DisplayName("정상적인 요청으로 허브를 생성할 수 있다.")
    void createHub_Success() {
        // when
        Hub hub = Hub.create("서울 센터", "서울특별시 중구", 37.5665, 126.9780);

        // then
        assertThat(hub.getName()).isEqualTo("서울 센터");
        assertThat(hub.getAddress()).isEqualTo("서울특별시 중구");
        assertThat(hub.getLatitude()).isEqualTo(37.5665);
        assertThat(hub.getLongitude()).isEqualTo(126.9780);
        assertThat(hub.getHubId()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
            "91.0, 126.9780",   // 위도 초과
            "-91.0, 126.9780",  // 위도 미달
            "37.5665, 181.0",   // 경도 초과
            "37.5665, -181.0"   // 경도 미달
    })
    @DisplayName("잘못된 위경도 좌표로 허브 생성 시 예외가 발생한다.")
    void createHub_InvalidCoordinates(Double lat, Double lon) {
        // when & then
        assertThatThrownBy(() -> Hub.create("오류 센터", "주소", lat, lon))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사이여야 합니다");
    }

    @Test
    @DisplayName("허브 정보를 수정할 수 있다.")
    void updateHub_Success() {
        // given
        Hub hub = Hub.create("구 이름", "구 주소", 37.0, 127.0);

        // when
        hub.update("신 이름", "신 주소", 38.0, 128.0);

        // then
        assertThat(hub.getName()).isEqualTo("신 이름");
        assertThat(hub.getAddress()).isEqualTo("신 주소");
        assertThat(hub.getLatitude()).isEqualTo(38.0);
        assertThat(hub.getLongitude()).isEqualTo(128.0);
    }

    @Test
    @DisplayName("Soft Delete 호출 시 삭제 상태가 된다.")
    void softDelete_Success() {
        // given
        Hub hub = Hub.create("서울 센터", "주소", 37.0, 127.0);

        // when
        hub.softDelete("user-1");

        // then
        assertThat(hub.isDeleted()).isTrue();
        assertThat(hub.getDeletedAt()).isNotNull();
        assertThat(hub.getDeletedBy()).isEqualTo("user-1");
    }
}
