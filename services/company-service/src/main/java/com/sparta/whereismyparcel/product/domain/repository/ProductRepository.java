package com.sparta.whereismyparcel.product.domain.repository;

import com.sparta.whereismyparcel.product.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);
}
