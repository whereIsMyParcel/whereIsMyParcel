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
@Table(name = "p_product_options")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption extends BaseEntity {

    // 1중대장
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_option_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(name = "option_name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_status", nullable = false)
    private ProductStatus status;

    @OneToMany(mappedBy = "product_option", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
    private List<ProductOptionValue> optionValues = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    public ProductOption(Product product, String name) {
        this.product = product;
        this.name = name;
        this.status = ProductStatus.ACTIVE;
    }

    public static ProductOption addOption(Product product, String name) {
        ProductOption productOption = ProductOption.builder()
                .product(product)
                .name(name)
                .build();

        product.addOption(productOption);
        return productOption;
    }

    public void addOptionValues(ProductOptionValue optionValues) {
        this.optionValues.add(optionValues);
    }

    public void updateOption(ProductOption productOption) {

    }

    public void stopSelling() {
        this.status = ProductStatus.INACTIVE;

        this.optionValues.forEach(optionValues->{optionValues.stopSelling();});
    }

    public void resumeSelling() {
        this.status = ProductStatus.ACTIVE;

        this.optionValues.forEach(optionValues->{optionValues.resumeSelling();});
    }

    public void delete(String userId) {
        super.softDelete(userId);
        this.status = ProductStatus.DELETED;

        this.optionValues.forEach(optionValues->{optionValues.delete(userId);});
    }
}
