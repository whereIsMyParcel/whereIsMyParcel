package com.sparta.whereismyparcel.product.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_product_variant_options", schema = "product_db")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariantOption extends BaseEntity {

    // 행정병
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_variant_option_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant variants;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_value_id", nullable = false)
    private ProductOptionValue optionValues;

    @Builder(access = AccessLevel.PRIVATE)
    public ProductVariantOption(ProductVariant variants, ProductOptionValue optionValues) {
        this.variants = variants;
        this.optionValues = optionValues;
    }

    public static ProductVariantOption addVariantOption(ProductVariant variants, ProductOptionValue optionValues) {
        ProductVariantOption variantOption = ProductVariantOption.builder()
                .variants(variants)
                .optionValues(optionValues)
                .build();

        variants.addVariantOption(variantOption);
        optionValues.addVariantOption(variantOption);
        return variantOption;
    }

    public void delete(String userId) {
        super.softDelete(userId);
    }

}
