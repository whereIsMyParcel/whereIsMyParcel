package com.sparta.whereismyparcel.product.domain.repository;

import com.sparta.whereismyparcel.product.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findProductBySkuCode(String skuCode);
}
