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
@Table(name = "p_product_option_values", schema = "product_db")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOptionValue extends BaseEntity {

    // 소대장
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_option_value_id", nullable = false,  updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption options;

    @Column(name = "option_value", nullable = false)
    private String value;

    @Column(name = "option_additional_prive", nullable = false)
    private Integer additionalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_value_status", nullable = false)
    private ProductStatus status;

    @OneToMany(mappedBy = "optionValues", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariantOption> variantOptions = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    public ProductOptionValue(ProductOption options, String value, Integer additionalPrice) {
        this.options = options;
        this.value = value;
        this.additionalPrice = additionalPrice;
        this.status = ProductStatus.ACTIVE;
    }

    public static ProductOptionValue addOptionValue(ProductOption options, String value, Integer additionalPrice) {
        ProductOptionValue productOptionValue = ProductOptionValue.builder()
                .options(options)
                .value(value)
                .additionalPrice(additionalPrice)
                .build();

        options.addOptionValues(productOptionValue);
        return productOptionValue;
    }

    public void addVariantOption(ProductVariantOption variantOption) {
        this.variantOptions.add(variantOption);
    }

    public void updateValueDetails(String newValue, Integer newAdditionalPrice) {
        this.value = newValue;
        this.additionalPrice = newAdditionalPrice;
    }

    public void stopSelling() {
        this.status =  ProductStatus.INACTIVE;
    }

    public void resumeSelling() {
        this.status =  ProductStatus.ACTIVE;
    }

    public void delete(String userId) {
        super.softDelete(userId);
        this.getVariantOptions().forEach(variantOption -> variantOption.delete(userId));
        this.status = ProductStatus.DELETED;
    }
}