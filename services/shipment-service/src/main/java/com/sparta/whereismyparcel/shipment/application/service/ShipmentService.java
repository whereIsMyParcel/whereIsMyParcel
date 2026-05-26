package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.shipment.domain.entity.Shipment;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentAlreadyStartedException;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentUpdateDeniedException;
import com.sparta.whereismyparcel.shipment.domain.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Transactional(readOnly = true)
@Service
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional
    public void cancel(String userId, UUID orderId) {
        UUID managerId = UUID.fromString(userId);
        //1. 주문에 속한 모든 배송들 조회
        List<Shipment> shipments = shipmentRepository.findAllByOrderId(orderId);

        //2. 권한 체크
        validateUpdatePermission(shipments, managerId);

        //3. 주문에 속한 모든 배송들 출발 전인지 확인
        boolean allCancelable = shipments.stream()
                .allMatch(Shipment::canCancel);

        if (!allCancelable) {
            throw new ShipmentAlreadyStartedException();
        }
        //4. 취소 처리
        shipments.forEach(Shipment::cancel);
    }

    private void validateUpdatePermission(List<Shipment> shipments, UUID managerId) {
        boolean hasPermission = shipments.stream()
                .allMatch(shipment -> shipment.isAssignedDeliveryManager(managerId));

        if (!hasPermission) {
            throw new ShipmentUpdateDeniedException();
        }
    }
}
