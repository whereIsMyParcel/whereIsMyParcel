package com.sparta.whereismyparcel.shipment.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_delivery_managers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryManager extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID hubId;

    @Column(nullable = false)
    private UUID slackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryType type;

    @Column(nullable = false)
    private int deliveryOrder;
}
