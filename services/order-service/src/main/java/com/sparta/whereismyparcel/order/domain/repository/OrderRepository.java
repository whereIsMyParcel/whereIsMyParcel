package com.sparta.whereismyparcel.order.domain.repository;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findWithOrderItemsByOrderIdAndDeletedAtIsNull(UUID orderId);

    @Query("""
            SELECT o
            FROM Order o
            WHERE o.deletedAt IS NULL
              AND (:isMaster = true OR o.orderedBy = :userId)
              AND (:status IS NULL OR o.orderStatus = :status)
              AND (
                    :keyword IS NULL OR :keyword = ''
                    OR o.orderNumber LIKE UPPER(CONCAT('%', :keyword, '%'))
                    OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR o.recipientPhone LIKE CONCAT('%', :keyword, '%')
              )
              AND (:startDate IS NULL OR o.orderedAt >= :startDate)
              AND (:endDate IS NULL OR o.orderedAt <= :endDate)
            """)
    Page<Order> searchOrders(
            @Param("userId") String userId,
            @Param("isMaster") boolean isMaster,
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "orderItems")
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.orderId = :orderId
              AND o.deletedAt IS NULL
              AND (:isMaster = true OR o.orderedBy = :userId)
            """)
    Optional<Order> findDetailByOrderId(
            @Param("orderId") UUID orderId,
            @Param("userId") String userId,
            @Param("isMaster") boolean isMaster
    );
}
