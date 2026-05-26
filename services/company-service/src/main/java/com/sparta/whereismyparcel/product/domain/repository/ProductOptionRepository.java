package com.sparta.whereismyparcel.product.domain.repository;

import com.sparta.whereismyparcel.product.domain.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductOptionRepository extends JpaRepository<ProductOption, UUID> {
}
