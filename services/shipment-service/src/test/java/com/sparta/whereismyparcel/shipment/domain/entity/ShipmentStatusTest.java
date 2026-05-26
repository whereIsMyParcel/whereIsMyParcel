package com.sparta.whereismyparcel.shipment.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ShipmentStatusTest {

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = "HUB_WAITING")
    @DisplayName("HUB_WAITING 상태는 배송 취소 가능하다")
    void canCancelByHubWaiting(ShipmentStatus status) {
        boolean result = status.canCancel();

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "HUB_WAITING")
    @DisplayName("HUB_WAITING 이외 상태는 배송 취소 불가능하다")
    void canCancelByNonHubWaiting(ShipmentStatus status) {
        boolean result = status.canCancel();

        assertThat(result).isFalse();
    }
}
