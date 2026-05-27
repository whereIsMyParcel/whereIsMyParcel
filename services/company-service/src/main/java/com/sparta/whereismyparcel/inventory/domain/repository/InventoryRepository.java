package com.sparta.whereismyparcel.inventory.domain.repository;

import com.sparta.whereismyparcel.inventory.domain.entity.Inventory;
import com.sparta.whereismyparcel.product.domain.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByHubIdAndProductVariant(UUID hubId, ProductVariant productVariant);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findByProductVariant(@Param("productVariant") ProductVariant productVariant);
}

