package com.sparta.whereismyparcel.shipment.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentAlreadyStartedException;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentCannotBeDeliveredException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_shipments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
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
            ShipmentStatus status,
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
                .shipmentStatus(status)
                .deliveryAddress(deliveryAddress)
                .recipientName(recipientName)
                .recipientSlackId(recipientSlackId)
                .build();
    }

    public void cancel() {
        if (!canCancel()) {
            throw new ShipmentAlreadyStartedException();
        }

        this.shipmentStatus = ShipmentStatus.CANCELLED;
    }

    public boolean canCancel() {
        return this.shipmentStatus.canCancel();
    }

    public boolean isAssignedDeliveryManager(UUID managerId) {

        boolean companyMatch = companyDeliveryManagerId.equals(managerId);

        boolean hubMatch = histories.stream()
                .anyMatch(h -> managerId.equals(h.getHubDeliveryManagerId()));

        return companyMatch || hubMatch;
    }

    public void delivered() {
        if (this.shipmentStatus != ShipmentStatus.COMPANY_MOVING) {
            throw new ShipmentCannotBeDeliveredException();
        }
        this.shipmentStatus = ShipmentStatus.DELIVERED;
    }

    public boolean isDelivered() {
        return this.shipmentStatus == ShipmentStatus.DELIVERED;
    }
}
