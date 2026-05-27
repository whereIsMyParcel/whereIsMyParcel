package com.sparta.whereismyparcel.order.domain.repository;

import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    Optional<OrderItem> findByOrderItemId(UUID orderItemId);
}
