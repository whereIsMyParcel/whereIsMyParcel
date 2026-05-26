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
@Table(name = "p_product_variants", schema = "product_db")
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

    @Column(name = "variant_price", nullable = false)
    private Integer variantPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_status")
    private ProductStatus status;

    @OneToMany(mappedBy = "variants", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariantOption> variantOptions = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    public ProductVariant(Product product, String skuCode, String variantName, Integer variantPrice) {
        this.product = product;
        this.skuCode = skuCode;
        this.variantName = variantName;
        this.variantPrice = variantPrice;
        this.status = ProductStatus.ACTIVE;
    }

    public static ProductVariant addVariant(Product product, String skuCode, String variantName, Integer variantPrice) {
        ProductVariant variants = ProductVariant.builder()
                .product(product)
                .skuCode(skuCode)
                .variantName(variantName)
                .variantPrice(variantPrice)
                .build();

        product.addVariant(variants);
        return variants;
    }

    public void syncVariants() {
        int totalAdditionalPrice = this.variantOptions.stream()
                .map(ProductVariantOption::getOptionValues)
                .mapToInt(ProductOptionValue::getAdditionalPrice)
                .sum();
        this.variantPrice = this.product.getPrice() + totalAdditionalPrice;

        if (this.variantOptions.isEmpty()) {
            this.variantName = this.product.getName();
            return;
        }

        StringBuilder sb = new StringBuilder(this.product.getName()).append(" (");

        for (int i = 0; i < this.variantOptions.size(); i++) {
            String valueText = this.variantOptions.get(i).getOptionValues().getValue();
            sb.append(valueText);

            if (i < this.variantOptions.size() - 1) {
                sb.append(" / ");
            }
        }
        sb.append(")");

        this.variantName = sb.toString();
    }

    public boolean containsOptionValue(ProductOptionValue targetValue) {
        return this.variantOptions.stream()
                .anyMatch(vo -> vo.getOptionValues().getId().equals(targetValue.getId()));
    }

    public void addVariantOption(ProductVariantOption variantOption) {
        this.variantOptions.add(variantOption);
    }

    public void stopSelling() {
        this.status = ProductStatus.INACTIVE;
    }

    public void resumeSelling() {
        this.status = ProductStatus.ACTIVE;
    }

    public void delete(String userId) {
        super.softDelete(userId);
        this.status = ProductStatus.DELETED;
    }
}
