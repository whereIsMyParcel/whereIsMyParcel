package com.sparta.whereismyparcel.inventory.domain.repository;

import com.sparta.whereismyparcel.inventory.domain.entity.Inventory;
import com.sparta.whereismyparcel.product.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByHubIdAndProductVariant(UUID hubId, ProductVariant productVariant);
}

