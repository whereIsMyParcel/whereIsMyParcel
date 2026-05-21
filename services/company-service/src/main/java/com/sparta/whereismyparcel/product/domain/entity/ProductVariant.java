package com.sparta.whereismyparcel.product.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_product_variants")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant extends BaseEntity {

    // 2중대장
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_variant_id", nullable = false,  updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(name = "sku_code", nullable = false, updatable = false, unique = true)
    private String skuCode;

    @Column(name = "variant_name", nullable = false, updatable = false)
    private String variantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_status")
    private ProductStatus status;

    @OneToMany(mappedBy = "product_variant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
    private List<ProductVariantOption> variantOptions = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    public ProductVariant(Product product, String skuCode, String variantName) {
        this.product = product;
        this.skuCode = skuCode;
        this.variantName = variantName;
        this.status = ProductStatus.ACTIVE;
    }

    public static ProductVariant addVariant(Product product, String skuCode, String variantName) {
        ProductVariant variants = ProductVariant.builder()
                .product(product)
                .skuCode(skuCode)
                .variantName(variantName)
                .build();

        product.addVariant(variants);
        return variants;
    }

    public void updateProductNameInVariant(String oldProductName, String newProductName) {
        if (this.variantName != null && this.variantName.contains(oldProductName)) {
            this.variantName = this.variantName.replace(oldProductName, newProductName);
        }
    }

    public void addVariantOption(ProductVariantOption option) {
        this.variantOptions.add(option);
    }

    public void stopSelling() {
        this.status = ProductStatus.INACTIVE;
    }

    public void delete(String userId) {
        super.softDelete(userId);
        this.status = ProductStatus.DELETED;
    }
}
