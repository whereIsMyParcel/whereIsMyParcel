package com.sparta.whereismyparcel.order.domain.repository;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findWithOrderItemsByOrderId(UUID orderId);

    @EntityGraph(attributePaths = "orderItems")
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.orderId = :orderId
              AND (:isMaster = true OR o.orderedBy = :userId)
            """)
    Optional<Order> findDetailByOrderId(
            @Param("orderId") UUID orderId,
            @Param("userId") String userId,
            @Param("isMaster") boolean isMaster
    );
}
