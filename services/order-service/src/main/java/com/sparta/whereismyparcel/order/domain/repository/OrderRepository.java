package com.sparta.whereismyparcel.order.domain.repository;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderIdAndDeletedAtIsNull(UUID orderId);
}
