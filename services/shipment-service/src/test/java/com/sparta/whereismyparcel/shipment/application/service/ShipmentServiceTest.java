package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.shipment.domain.entity.Shipment;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentAlreadyStartedException;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentUpdateDeniedException;
import com.sparta.whereismyparcel.shipment.domain.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class ShipmentServiceTest {

    private ShipmentRepository shipmentRepository;
    private ShipmentService shipmentService;

    private UUID orderId;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        shipmentService = new ShipmentService(shipmentRepository);

        orderId = UUID.randomUUID();
        managerId = UUID.randomUUID();
    }

    @DisplayName("담당 배송 관리자는 주문에 속한 배송을 취소할 수 있다")
    @Test
    void cancel() {
        // given
        Shipment shipment1 = cancelableShipment(true);
        Shipment shipment2 = cancelableShipment(true);

        mockShipments(shipment1, shipment2);

        // when
        shipmentService.cancel(managerId.toString(), orderId);

        // then
        verify(shipment1).cancel();
        verify(shipment2).cancel();
    }

    @DisplayName("담당 배송 관리자가 아니면 배송 취소 시 예외가 발생한다")
    @Test
    void cancel_fail_permission() {
        // given
        Shipment shipment = mock(Shipment.class);

        given(shipment.isAssignedDeliveryManager(managerId))
                .willReturn(false);

        mockShipments(shipment);

        // when & then
        assertThatThrownBy(() ->
                shipmentService.cancel(managerId.toString(), orderId)
        ).isInstanceOf(ShipmentUpdateDeniedException.class);

        verify(shipment, never()).cancel();
    }

    @DisplayName("주문에 속한 배송 중 하나라도 이미 출발했다면 취소 시 예외가 발생한다")
    @Test
    void cancel_fail_already_started() {
        // given
        Shipment shipment1 = cancelableShipment(true);
        Shipment shipment2 = cancelableShipment(false);

        mockShipments(shipment1, shipment2);

        // when & then
        assertThatThrownBy(() ->
                shipmentService.cancel(managerId.toString(), orderId)
        ).isInstanceOf(ShipmentAlreadyStartedException.class);

        verify(shipment1, never()).cancel();
        verify(shipment2, never()).cancel();
    }

    private Shipment cancelableShipment(boolean canCancel) {
        Shipment shipment = mock(Shipment.class);

        given(shipment.isAssignedDeliveryManager(managerId)).willReturn(true);

        given(shipment.canCancel()).willReturn(canCancel);

        return shipment;
    }

    private void mockShipments(Shipment... shipments) {
        given(shipmentRepository.findAllByOrderId(orderId))
                .willReturn(List.of(shipments));
    }
}
