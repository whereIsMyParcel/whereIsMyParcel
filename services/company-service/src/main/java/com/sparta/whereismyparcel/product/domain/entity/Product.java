package com.sparta.whereismyparcel.product.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_products", schema = "product_db")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    // 대대장
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_name", nullable = false, length = 100)
    private String name;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProductStatus status;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();


    @Builder(access = AccessLevel.PRIVATE)
    public Product(
            String name,
            UUID companyId,
            UUID hubId,
            String description,
            Integer price) {
        this.name = name;
        this.companyId = companyId;
        this.hubId = hubId;
        this.description = description;
        this.price = price;
        this.status = ProductStatus.ACTIVE;
    }

    public static Product create(
            String name,
            UUID companyId,
            UUID hubId,
            String description,
            Integer price
    ) {
        Product product = Product.builder()
                .name(name)
                .companyId(companyId)
                .hubId(hubId)
                .description(description)
                .price(price)
                .build();
        return product;
    }

    public void addOption(ProductOption option) {
        this.options.add(option);
    }

    public void addVariant(ProductVariant variant) {
        this.variants.add(variant);
    }

    public void updateDetails(String name, String description, Integer price) {
        if (name != null) {
            this.name = name;
        }

        this.description = description;

        if (price != null) {
            this.price = price;
        }
    }

    public void stopSelling() {
        this.status = ProductStatus.INACTIVE;

        this.variants.forEach(ProductVariant::stopSelling);
    }

    public void resumeSelling() {
        this.status = ProductStatus.ACTIVE;
        this.variants.forEach(ProductVariant::resumeSelling);
    }

    public void delete(String userId) {
        super.softDelete(userId);
        this.status = ProductStatus.DELETED;

        this.options.forEach(option -> option.delete(userId));
        this.variants.forEach(variant -> variant.delete(userId));
    }
}
