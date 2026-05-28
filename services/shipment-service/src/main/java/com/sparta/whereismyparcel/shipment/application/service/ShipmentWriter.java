package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.shipment.domain.entity.Shipment;
import com.sparta.whereismyparcel.shipment.domain.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentWriter {

    private final ShipmentRepository shipmentRepository;

    @Transactional
    public List<Shipment> save(List<Shipment> shipments) {
        return shipmentRepository.saveAll(shipments);
    }
}
