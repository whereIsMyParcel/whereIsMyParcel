package com.sparta.whereismyparcel.shipment.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
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

    @Column(nullable = false)
    private UUID originHubId;

    @Column(nullable = false)
    private UUID currentHubId;

    @Column(nullable = false)
    private UUID destinationHubId;

    @Column(nullable = false)
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


    @Builder(access = AccessLevel.PRIVATE)
    private Shipment(
            UUID orderId,
            UUID originHubId,
            UUID currentHubId,
            UUID destinationHubId,
            UUID companyDeliveryManagerId,
            String shipmentNumber,
            ShipmentStatus shipmentStatus,
            String deliveryAddress,
            String recipientName,
            String recipientSlackId,
            LocalDateTime estimatedDeliveryAt,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt
    ) {
        this.orderId = orderId;
        this.originHubId = originHubId;
        this.currentHubId = currentHubId;
        this.destinationHubId = destinationHubId;
        this.companyDeliveryManagerId = companyDeliveryManagerId;
        this.shipmentNumber = shipmentNumber;
        this.shipmentStatus = shipmentStatus;
        this.deliveryAddress = deliveryAddress;
        this.recipientName = recipientName;
        this.recipientSlackId = recipientSlackId;
        this.estimatedDeliveryAt = estimatedDeliveryAt;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
    }

    public static Shipment create(
            UUID orderId,
            UUID originHubId,
            UUID destinationHubId,
            UUID companyDeliveryManagerId,
            String shipmentNumber,
            String deliveryAddress,
            String recipientName,
            String recipientSlackId
    ) {
        return Shipment.builder()
                .orderId(orderId)
                .originHubId(originHubId)
                .currentHubId(originHubId)
                .destinationHubId(destinationHubId)
                .companyDeliveryManagerId(companyDeliveryManagerId)
                .shipmentNumber(shipmentNumber)
                .shipmentStatus(ShipmentStatus.HUB_WAITING)
                .deliveryAddress(deliveryAddress)
                .recipientName(recipientName)
                .recipientSlackId(recipientSlackId)
                .build();
    }
}
