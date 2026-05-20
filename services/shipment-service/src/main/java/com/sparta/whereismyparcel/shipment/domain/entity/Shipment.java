package com.sparta.whereismyparcel.shipment.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_shipments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    private UUID originHubId;

    private UUID currentHubId;

    private UUID destinationHubId;

    private UUID companyDeliveryManagerId;

    @Column(nullable = false, unique = true, length = 50)
    private String shipmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", nullable = false, length = 30)
    private ShipmentStatus shipmentStatus;

    @Column(nullable = false, length = 255)
    private String deliveryAddress;

    @Column(nullable = false, length = 100)
    private String recipientName;

    @Column(nullable = false, length = 100)
    private String recipientSlackId;

    private LocalDateTime estimatedDeliveryAt;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentHistory> histories = new ArrayList<>();
}
