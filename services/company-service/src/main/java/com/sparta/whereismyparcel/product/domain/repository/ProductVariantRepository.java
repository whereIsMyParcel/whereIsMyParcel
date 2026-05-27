package com.sparta.whereismyparcel.product.domain.repository;

import com.sparta.whereismyparcel.product.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findProductBySkuCode(String skuCode);


    @Query("SELECT pv FROM ProductVariant pv " +
    "JOIN FETCH  pv.product p " +
    "WHERE pv.id IN :variantIds")
    List<ProductVariant> findAllWithProductByIdIn(@Param("variantIds") List<UUID> variantIds);
}
