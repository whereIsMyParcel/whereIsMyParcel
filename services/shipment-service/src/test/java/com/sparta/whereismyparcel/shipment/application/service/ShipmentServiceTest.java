package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.shipment.domain.entity.Shipment;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentAlreadyStartedException;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentNotFoundException;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentUpdateDeniedException;
import com.sparta.whereismyparcel.shipment.domain.repository.ShipmentRepository;
import com.sparta.whereismyparcel.shipment.infrastructure.client.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class ShipmentServiceTest {

    private ShipmentRepository shipmentRepository;
    private DeliveryManagerService deliveryManagerService;
    private ShipmentService shipmentService;
    private OrderClient orderClient;
    private CompanyClient companyClient;
    private HubClient hubClient;

    private UUID orderId;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        deliveryManagerService = mock(DeliveryManagerService.class);
        orderClient = mock(OrderClient.class);
        companyClient = mock(CompanyClient.class);
        hubClient = mock(HubClient.class);

        shipmentService = new ShipmentService(
                shipmentRepository,
                deliveryManagerService,
                orderClient,
                companyClient,
                hubClient
        );

        orderId = UUID.randomUUID();
        managerId = UUID.randomUUID();
    }

    @DisplayName("배송 취소")
    @Nested
    class Cancel {

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
    }

    @DisplayName("배송 완료")
    @Nested
    class Delivered {

        @DisplayName("담당 배송 관리자는 배송 완료 처리할 수 있다")
        @Test
        void delivered() {
            // given
            UUID shipmentId = UUID.randomUUID();

            Shipment shipment1 = mock(Shipment.class);
            Shipment shipment2 = mock(Shipment.class);

            given(shipmentRepository.findById(shipmentId))
                    .willReturn(java.util.Optional.of(shipment1));

            given(shipment1.isAssignedDeliveryManager(managerId))
                    .willReturn(true);

            given(shipment1.getOrderId())
                    .willReturn(orderId);

            given(shipmentRepository.findAllByOrderId(orderId))
                    .willReturn(List.of(shipment1, shipment2));

            given(shipment1.isDelivered())
                    .willReturn(true);

            given(shipment2.isDelivered())
                    .willReturn(true);

            // when
            shipmentService.delivered(managerId.toString(), shipmentId);

            // then
            verify(shipment1).delivered();

            verify(orderClient)
                    .complete(orderId);
        }

        @DisplayName("담당 배송 관리자가 아니면 배송 완료 시 예외가 발생한다")
        @Test
        void delivered_fail_permission() {
            // given
            UUID shipmentId = UUID.randomUUID();

            Shipment shipment = mock(Shipment.class);

            given(shipmentRepository.findById(shipmentId))
                    .willReturn(java.util.Optional.of(shipment));

            given(shipment.isAssignedDeliveryManager(managerId))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() ->
                    shipmentService.delivered(managerId.toString(), shipmentId)
            ).isInstanceOf(ShipmentUpdateDeniedException.class);

            verify(shipment, never()).delivered();

            verify(orderClient, never())
                    .complete(any(UUID.class));
        }

        @DisplayName("주문에 속한 배송 중 완료되지 않은 배송이 존재하면 주문 완료 요청하지 않는다")
        @Test
        void delivered_not_all_delivered() {
            // given
            UUID shipmentId = UUID.randomUUID();

            Shipment shipment1 = mock(Shipment.class);
            Shipment shipment2 = mock(Shipment.class);

            given(shipmentRepository.findById(shipmentId))
                    .willReturn(java.util.Optional.of(shipment1));

            given(shipment1.isAssignedDeliveryManager(managerId))
                    .willReturn(true);

            given(shipment1.getOrderId())
                    .willReturn(orderId);

            given(shipmentRepository.findAllByOrderId(orderId))
                    .willReturn(List.of(shipment1, shipment2));

            given(shipment1.isDelivered())
                    .willReturn(true);

            given(shipment2.isDelivered())
                    .willReturn(false);

            // when
            shipmentService.delivered(managerId.toString(), shipmentId);

            // then
            verify(shipment1).delivered();

            verify(orderClient, never())
                    .complete(any(UUID.class));
        }
    }

    @Test
    @DisplayName("주문에 속한 배송이 없으면 예외가 발생한다")
    void getShipmentByOrderId_notFound() {
        // given
        when(shipmentRepository.findAllByOrderId(orderId))
                .thenReturn(emptyList());

        // when & then
        assertThrows(
                ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentByOrderId(orderId)
        );

        verify(shipmentRepository).findAllByOrderId(orderId);
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
