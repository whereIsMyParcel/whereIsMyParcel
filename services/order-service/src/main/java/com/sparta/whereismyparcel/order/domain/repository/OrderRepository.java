package com.sparta.whereismyparcel.order.domain.repository;

import com.sparta.whereismyparcel.order.domain.OrderStatus;
import com.sparta.whereismyparcel.order.domain.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findWithOrderItemsByOrderId(UUID orderId);

    // logistics-agent scheduled scan(design §16.2)이 상태별 고장 후보를 열거하는 데 쓴다.
    // orderId 컬럼만 프로젝션해 경량 조회한다.
    @Query("SELECT o.orderId FROM Order o WHERE o.orderStatus = :status")
    List<UUID> findOrderIdsByOrderStatus(@Param("status") OrderStatus status);

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
