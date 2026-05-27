package com.sparta.whereismyparcel.shipment.domain.repository;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryManager;
import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {
    long countByHubIdAndType(UUID hubId, DeliveryType type);

    long countByType(DeliveryType type);

    //region [배송담당자 순번 채번]
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
    //endregion

    //region [배송 담당자 배정 대상 조회]
    //업체 배송 담담자 배정 대상 조회
    @Query("""
                SELECT d
                FROM DeliveryManager d
                WHERE d.hubId = :hubId
                  AND d.type = :type
                ORDER BY d.deliveryOrder ASC
            """)
    List<DeliveryManager> findNextCompanyDeliveryManagers(
            UUID hubId,
            DeliveryType type,
            Pageable pageable
    );

    //허브 배송 담담자 배정 대상 조회
    @Query("""
                 SELECT d
                 FROM DeliveryManager d
                 WHERE d.hubId is null
                   AND d.type = :type
                 ORDER BY d.deliveryOrder ASC
            """)
    List<DeliveryManager> findNextHubDeliveryManagers(
            DeliveryType type,
            Pageable pageable
    );
    //endregion

    @Query("""
                SELECT d
                FROM DeliveryManager d
                WHERE (:#{#req.hubId} IS NULL OR d.hubId = :#{#req.hubId})
                  AND (:#{#req.type} IS NULL OR d.type = :#{#req.type})
                  AND (:#{#req.slackId} IS NULL OR d.slackId LIKE %:#{#req.slackId}%)
            """)
    Page<DeliveryManager> search(
            @Param("req") DeliveryManagerSearchRequest req,
            Pageable pageable
    );
}
