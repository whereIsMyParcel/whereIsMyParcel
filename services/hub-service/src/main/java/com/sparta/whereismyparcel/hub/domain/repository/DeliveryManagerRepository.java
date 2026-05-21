package com.sparta.whereismyparcel.hub.domain.repository;

import com.sparta.whereismyparcel.hub.domain.entity.DeliveryManager;
import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {

    // 특정 허브에서 가장 큰 배정 순번 조회 (등록 시 마지막 순번 + 1 계산용)
    @Query("SELECT MAX(dm.deliveryOrder) FROM DeliveryManager dm WHERE dm.hub = :hub")
    Optional<Integer> findMaxDeliveryOrderByHub(@Param("hub") Hub hub);

    // 특정 허브에서 배정 가능한 첫 번째 담당자 조회 (순번 오름차순)
    Optional<DeliveryManager> findFirstByHubAndStatusOrderByDeliveryOrderAsc(
            Hub hub, DeliveryManager.DeliveryManagerStatus status);
}
