package com.sparta.whereismyparcel.hub.domain.repository;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.entity.HubRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {

    // 출발지와 목적지 기반 경로 조회
    Optional<HubRoute> findByOriginHubAndDestinationHub(Hub originHub, Hub destinationHub);

    // 특정 허브에서 출발하는 모든 경로 조회 (Dijkstra 탐색용)
    List<HubRoute> findAllByOriginHub(Hub originHub);

    // 전체 경로 및 연관된 허브(출발/도착)를 N+1 없이 한 번에 조회 (최적화)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM HubRoute r JOIN FETCH r.originHub JOIN FETCH r.destinationHub")
    List<HubRoute> findAllWithHubs();
}
