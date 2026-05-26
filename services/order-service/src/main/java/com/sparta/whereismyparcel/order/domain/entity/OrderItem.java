package com.sparta.whereismyparcel.order.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "order_item_id", nullable = false, updatable = false)
    private UUID orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(name = "sku_code", nullable = false, length = 100)
    private String skuCode;

    @Column(name = "product_name_snapshot", nullable = false, length = 150)
    private String productNameSnapshot;

    @Column(name = "product_option_snapshot", length = 150)
    private String productOptionSnapshot;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Builder(access = AccessLevel.PRIVATE)
    private OrderItem(
            UUID productVariantId,
            String skuCode,
            String productNameSnapshot,
            String productOptionSnapshot,
            Long unitPrice,
            Integer quantity
    ) {
        this.productVariantId = productVariantId;
        this.skuCode = skuCode;
        this.productNameSnapshot = productNameSnapshot;
        this.productOptionSnapshot = productOptionSnapshot;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public static OrderItem create(
            UUID productVariantId,
            String skuCode,
            String productNameSnapshot,
            String productOptionSnapshot,
            Long unitPrice,
            Integer quantity
    ) {
        return OrderItem.builder()
                .productVariantId(productVariantId)
                .skuCode(skuCode)
                .productNameSnapshot(productNameSnapshot)
                .productOptionSnapshot(productOptionSnapshot)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .build();
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public Long calculateTotalPrice() {
        return unitPrice * quantity;
    }
}
