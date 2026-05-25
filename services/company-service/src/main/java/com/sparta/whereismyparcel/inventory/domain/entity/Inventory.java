package com.sparta.whereismyparcel.inventory.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import com.sparta.whereismyparcel.product.domain.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_inventories", schema = "inventory_db")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inventory_id", nullable = false, updatable = false)
    private UUID inventoryId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "quantity",  nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity")
    private Integer reservedQuantity;

    @Column(name = "safety_stock_quantity", nullable = false)
    private Integer safetyStockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InventoryStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private Inventory(
            UUID hubId,
            ProductVariant productVariant,
            Integer quantity,
            Integer safetyStockQuantity
    ) {
        this.hubId = hubId;
        this.productVariant = productVariant;
        this.quantity = quantity;
        this.reservedQuantity = 0;
        this.safetyStockQuantity = safetyStockQuantity;
        this.status = InventoryStatus.ACTIVE;
    }

    public static Inventory addInventory(
            UUID hubId,
            ProductVariant productVariant,
            Integer quantity,
            Integer safetyStockQuantity
    ) {
        Inventory inventory = Inventory.builder()
                .hubId(hubId)
                .productVariant(productVariant)
                .quantity(quantity)
                .safetyStockQuantity(safetyStockQuantity)
                .build();
        return inventory;
    }

    public Integer getAvailableQuantity() {
        return this.quantity - this.reservedQuantity;
    }

    // 예약 재고 설정
    public void addReservedStock(Integer orderQuantity) {
        if (this.quantity - this.reservedQuantity < orderQuantity) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.reservedQuantity += orderQuantity;
    }

    public void confirmShipment(Integer orderQuantity) {
        if (this.reservedQuantity < orderQuantity) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (this.quantity < orderQuantity) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        this.quantity -= orderQuantity;
        this.reservedQuantity -= orderQuantity;
    }

    public void cancelReservation(Integer orderQuantity) {
        if (this.reservedQuantity < orderQuantity) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        this.reservedQuantity -= orderQuantity;
    }

    public void autoIncreaseQuantity() {
        if (this.safetyStockQuantity < 5) {
            this.quantity += 100;
        }
    }
}
