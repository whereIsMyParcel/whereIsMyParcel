package com.sparta.whereismyparcel.shipment.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_shipment_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShipmentHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false)
    private UUID originHubId;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private UUID destinationHubId;

    private UUID hubDeliveryManagerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", nullable = false, length = 30)
    private ShipmentStatus status;

    private int estimatedDuration;

    private int actualDistance;

    private int actualDuration;

    @Column(length = 255)
    private String description;

    @Builder(access = AccessLevel.PRIVATE)
    private ShipmentHistory(
            Shipment shipment,
            UUID originHubId,
            int sequence,
            UUID destinationHubId,
            UUID hubDeliveryManagerId,
            ShipmentStatus status,
            int estimatedDuration,
            int actualDistance,
            int actualDuration,
            String description
    ) {
        this.shipment = shipment;
        this.originHubId = originHubId;
        this.sequence = sequence;
        this.destinationHubId = destinationHubId;
        this.hubDeliveryManagerId = hubDeliveryManagerId;
        this.status = status;
        this.estimatedDuration = estimatedDuration;
        this.actualDistance = actualDistance;
        this.actualDuration = actualDuration;
        this.description = description;
    }

    public static ShipmentHistory create(
            Shipment shipment,
            UUID originHubId,
            int sequence,
            UUID destinationHubId,
            UUID hubDeliveryManagerId,
            ShipmentStatus status,
            int estimatedDuration,
            int actualDistance,
            int actualDuration,
            String description
    ) {
        return ShipmentHistory.builder()
                .shipment(shipment)
                .originHubId(originHubId)
                .sequence(sequence)
                .destinationHubId(destinationHubId)
                .hubDeliveryManagerId(hubDeliveryManagerId)
                .status(status)
                .estimatedDuration(estimatedDuration)
                .actualDistance(actualDistance)
                .actualDuration(actualDuration)
                .description(description)
                .build();
    }
}
