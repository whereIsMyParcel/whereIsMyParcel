package com.sparta.whereismyparcel.shipment.domain.repository;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryManager;
import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {
    long countByHubIdAndType(UUID hubId, DeliveryType type);
    long countByType(DeliveryType type);

    @Query("""
        select coalesce(max(d.deliveryOrder), 0) + 1
        from DeliveryManager d
        where d.hubId is null
          and d.type = :type
    """)
    int findNextOrderByHub(@Param("type") DeliveryType type);

    @Query("""
        select coalesce(max(d.deliveryOrder), 0) + 1
        from DeliveryManager d
        where d.hubId = :hubId
          and d.type = :type
    """)
    int findNextOrderByCompany(@Param("hubId") UUID hubId,
                             @Param("type") DeliveryType type);
}
