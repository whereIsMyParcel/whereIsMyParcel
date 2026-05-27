package com.sparta.whereismyparcel.product.domain.repository;

import com.sparta.whereismyparcel.product.domain.entity.ProductOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValue, UUID> {
}
