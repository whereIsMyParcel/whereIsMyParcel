package com.sparta.whereismyparcel.shipment.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
}
