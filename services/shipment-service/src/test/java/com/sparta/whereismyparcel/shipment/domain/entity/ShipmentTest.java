package com.sparta.whereismyparcel.shipment.domain.entity;

import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentAlreadyStartedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class ShipmentTest {

    @DisplayName("배송 취소")
    @Nested
    class cancel {

        @DisplayName("배송이 시작되기 전 상태에서만 배송 취소가 가능하다")
        @ParameterizedTest
        @EnumSource(value = ShipmentStatus.class, names = "HUB_WAITING")
        void canCancel(ShipmentStatus status) {
            Shipment shipment = Shipment.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "배송번호 1",
                    status,
                    "배송지 주소",
                    "수령인명",
                    "slack id"
            );

            shipment.cancel();

            assertThat(shipment.getShipmentStatus())
                    .isEqualTo(ShipmentStatus.CANCELLED);
        }

        @DisplayName("배송이 시작된 상태에서는 배송 취소 시 예외가 발생한다")
        @ParameterizedTest
        @EnumSource(value = ShipmentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "HUB_WAITING")
        void canNotCancel(ShipmentStatus status) {
            Shipment shipment = Shipment.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "배송번호 1",
                    status,
                    "배송지 주소",
                    "수령인명",
                    "slack id"
            );

            assertThatThrownBy(shipment::cancel)
                    .isInstanceOf(ShipmentAlreadyStartedException.class);
        }

        @DisplayName("해당 유저가 담당 배송 담당자인지 여부를 확인한다")
        @ParameterizedTest
        @MethodSource("managerProvider")
        void isAssignedDeliveryManager(boolean expected, UUID assignedManagerId, UUID requestManagerId) {
            // given
            Shipment shipment = Shipment.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "배송번호 1",
                    ShipmentStatus.HUB_WAITING,
                    "배송지 주소",
                    "수령인명",
                    "slack id"
            );

            ShipmentHistory history = ShipmentHistory.create(
                    shipment,
                    UUID.randomUUID(),
                    1,
                    UUID.randomUUID(),
                    assignedManagerId,
                    ShipmentStatus.HUB_WAITING,
                    0,
                    0,
                    0,
                    "test"
            );

            shipment.getHistories().add(history);

            // when
            boolean result = shipment.isAssignedDeliveryManager(requestManagerId);

            // then
            assertThat(result).isEqualTo(expected);
        }

        static Stream<Arguments> managerProvider() {
            UUID assignedManagerId = UUID.randomUUID();

            return Stream.of(
                    Arguments.of(true, assignedManagerId, assignedManagerId),
                    Arguments.of(false, assignedManagerId, UUID.randomUUID())
            );
        }
    }
}
