package com.sparta.whereismyparcel.shipment.domain.repository;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryManager;
import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {
    long countByHubId(UUID hubId);

    //TODO 동시성 이슈 발생 가능성 높으므로 변경 필요
    @Query("""
        select coalesce(max(d.deliveryOrder), 0) + 1
        from DeliveryManager d
        where d.hubId = :hubId
          and d.type = :type
    """)
    int findNextDeliveryOrder(@Param("hubId") UUID hubId,
                              @Param("type") DeliveryType type);
}
