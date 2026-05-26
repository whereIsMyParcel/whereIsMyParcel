package com.sparta.whereismyparcel.shipment.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(
        name = "p_delivery_managers",
        indexes = {
                @Index(
                        name = "idx_delivery_manager_hub_type_order",
                        columnList = "hub_id, type, delivery_order"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class DeliveryManager extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID hubId;

    @Column(nullable = false, length = 30)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryType type;

    @Column(nullable = false)
    private int deliveryOrder;

    @Builder(access = AccessLevel.PRIVATE)
    private DeliveryManager(UUID hubId, String slackId, DeliveryType type, int deliveryOrder) {
        this.hubId = hubId;
        this.slackId = slackId;
        this.type = type;
        this.deliveryOrder = deliveryOrder;
    }

    public static DeliveryManager create(UUID hubId, String slackId, DeliveryType type, int deliveryOrder) {
        return DeliveryManager.builder()
                .hubId(hubId)
                .slackId(slackId)
                .type(type)
                .deliveryOrder(deliveryOrder)
                .build();
    }
}
