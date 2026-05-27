package com.sparta.whereismyparcel.shipment.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_shipment_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class ShipmentItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(nullable = false)
    private int quantity;

    @Builder(access = AccessLevel.PRIVATE)
    private ShipmentItem(
            Shipment shipment,
            UUID orderItemId,
            int quantity
    ) {
        this.shipment = shipment;
        this.orderItemId = orderItemId;
        this.quantity = quantity;
    }

    public static ShipmentItem create(
            Shipment shipment,
            UUID orderItemId,
            int quantity
    ) {
        return new ShipmentItem(
                shipment,
                orderItemId,
                quantity
        );
    }
}
