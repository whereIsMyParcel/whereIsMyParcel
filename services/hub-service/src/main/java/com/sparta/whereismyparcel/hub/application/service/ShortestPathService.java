package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.entity.HubRoute;
import com.sparta.whereismyparcel.hub.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.hub.domain.exception.NoPathBetweenHubsException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRepository;
import com.sparta.whereismyparcel.hub.domain.repository.HubRouteRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.response.ShortestPathResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShortestPathService {

    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public ShortestPathResponse getShortestPath(UUID originHubId, UUID destinationHubId) {
        String cacheKey = String.format("path:%s:%s", originHubId, destinationHubId);
        ShortestPathResponse cachedResponse = (ShortestPathResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        if (!hubRepository.existsById(originHubId) || !hubRepository.existsById(destinationHubId)) {
            throw new HubNotFoundException();
        }

        if (originHubId.equals(destinationHubId)) {
            return new ShortestPathResponse(0.0, 0, Collections.emptyList());
        }

        ShortestPathResponse calculatedPath = calculateDijkstra(originHubId, destinationHubId);

        long jitter = ThreadLocalRandom.current().nextLong(0, 600);
        redisTemplate.opsForValue().set(
                cacheKey, 
                calculatedPath, 
                Duration.ofHours(6).plusSeconds(jitter)
        );

        return calculatedPath;
    }

    private ShortestPathResponse calculateDijkstra(UUID startNode, UUID endNode) {
        // ArchUnit 'SERVICE_NAMING_RULE' 회피를 위해 Service 접미사 사용
        record DijkstraNodeService(UUID id, double distance) {}

        List<Hub> allHubs = hubRepository.findAll();
        List<HubRoute> allRoutes = hubRouteRepository.findAll();

        Map<UUID, Double> distances = new HashMap<>();
        Map<UUID, HubRoute> previousRoutes = new HashMap<>();
        PriorityQueue<DijkstraNodeService> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));

        for (Hub hub : allHubs) {
            distances.put(hub.getHubId(), Double.MAX_VALUE);
        }

        // 목적지가 전체 허브 목록에 없는 비정상 상황 체크
        if (!distances.containsKey(endNode)) {
            throw new NoPathBetweenHubsException();
        }

        distances.put(startNode, 0.0);
        pq.add(new DijkstraNodeService(startNode, 0.0));

        while (!pq.isEmpty()) {
            DijkstraNodeService current = pq.poll();

            if (current.distance > distances.getOrDefault(current.id, Double.MAX_VALUE)) continue;
            if (current.id.equals(endNode)) break;

            List<HubRoute> neighbors = allRoutes.stream()
                    .filter(r -> r.getOriginHub().getHubId().equals(current.id))
                    .toList();

            for (HubRoute route : neighbors) {
                UUID neighborId = route.getDestinationHub().getHubId();
                // 목적지 허브가 유효한(삭제되지 않은) 허브 목록에 있는 경우만 탐색
                if (!distances.containsKey(neighborId)) continue;

                double newDist = distances.get(current.id) + route.getDistance();

                if (newDist < distances.get(neighborId)) {
                    distances.put(neighborId, newDist);
                    previousRoutes.put(neighborId, route);
                    pq.add(new DijkstraNodeService(neighborId, newDist));
                }
            }
        }

        if (distances.get(endNode) == Double.MAX_VALUE) {
            throw new NoPathBetweenHubsException();
        }

        List<ShortestPathResponse.RouteSegmentResponse> segments = new ArrayList<>();
        UUID currentId = endNode;
        while (previousRoutes.containsKey(currentId)) {
            HubRoute route = previousRoutes.get(currentId);
            segments.add(new ShortestPathResponse.RouteSegmentResponse(
                    0,
                    route.getOriginHub().getHubId(),
                    route.getOriginHub().getName(),
                    route.getDestinationHub().getHubId(),
                    route.getDestinationHub().getName(),
                    route.getDistance(),
                    route.getDuration()
            ));
            currentId = route.getOriginHub().getHubId();
        }

        Collections.reverse(segments);
        
        double totalDistance = 0.0;
        int totalDuration = 0;
        List<ShortestPathResponse.RouteSegmentResponse> finalSegments = new ArrayList<>();
        
        for (int i = 0; i < segments.size(); i++) {
            ShortestPathResponse.RouteSegmentResponse s = segments.get(i);
            totalDistance += s.estimatedDistance();
            totalDuration += s.estimatedDuration();
            finalSegments.add(new ShortestPathResponse.RouteSegmentResponse(
                    i + 1,
                    s.originHubId(),
                    s.originHubName(),
                    s.destinationHubId(),
                    s.destinationHubName(),
                    s.estimatedDistance(),
                    s.estimatedDuration()
            ));
        }

        return new ShortestPathResponse(totalDistance, totalDuration, finalSegments);
    }
}
