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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShortestPathService {

    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 다익스트라 알고리즘을 사용하여 두 허브 간의 최단 경로를 계산합니다.
     * 결과는 Redis에 캐싱되며, Cache Stampede 방지를 위해 Jitter가 적용됩니다.
     */
    public ShortestPathResponse getShortestPath(UUID originHubId, UUID destinationHubId) {
        // 1. 캐시 확인
        String cacheKey = String.format("path:%s:%s", originHubId, destinationHubId);
        ShortestPathResponse cachedResponse = (ShortestPathResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        // 2. 허브 존재 확인
        if (!hubRepository.existsById(originHubId) || !hubRepository.existsById(destinationHubId)) {
            throw new HubNotFoundException();
        }

        if (originHubId.equals(destinationHubId)) {
            return new ShortestPathResponse(0.0, 0, Collections.emptyList());
        }

        // 3. 다익스트라 알고리즘 수행
        ShortestPathResponse calculatedPath = calculateDijkstra(originHubId, destinationHubId);

        // 4. 결과 캐싱 (Jitter 적용: TTL 6시간 + 0~10분 무작위)
        long jitter = ThreadLocalRandom.current().nextLong(0, 600);
        redisTemplate.opsForValue().set(
                cacheKey, 
                calculatedPath, 
                Duration.ofHours(6).plusSeconds(jitter)
        );

        return calculatedPath;
    }

    private ShortestPathResponse calculateDijkstra(UUID startNode, UUID endNode) {
        // 모든 허브와 경로 정보를 가져와서 그래프 구성
        List<Hub> allHubs = hubRepository.findAll();
        List<HubRoute> allRoutes = hubRouteRepository.findAll();

        Map<UUID, Double> distances = new HashMap<>();
        Map<UUID, HubRoute> previousRoutes = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));

        for (Hub hub : allHubs) {
            distances.put(hub.getHubId(), Double.MAX_VALUE);
        }

        distances.put(startNode, 0.0);
        pq.add(new Node(startNode, 0.0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            if (current.distance > distances.get(current.id)) continue;
            if (current.id.equals(endNode)) break;

            // 현재 노드에서 출발하는 모든 경로 탐색
            List<HubRoute> neighbors = allRoutes.stream()
                    .filter(r -> r.getOriginHub().getHubId().equals(current.id))
                    .toList();

            for (HubRoute route : neighbors) {
                UUID neighborId = route.getDestinationHub().getHubId();
                double newDist = distances.get(current.id) + route.getDistance();

                if (newDist < distances.get(neighborId)) {
                    distances.put(neighborId, newDist);
                    previousRoutes.put(neighborId, route);
                    pq.add(new Node(neighborId, newDist));
                }
            }
        }

        if (distances.get(endNode) == Double.MAX_VALUE) {
            throw new NoPathBetweenHubsException();
        }

        // 경로 복원
        List<ShortestPathResponse.RouteSegmentResponse> segments = new ArrayList<>();
        UUID current = endNode;
        while (previousRoutes.containsKey(current)) {
            HubRoute route = previousRoutes.get(current);
            segments.add(new ShortestPathResponse.RouteSegmentResponse(
                    0, // 나중에 sequence 정렬 후 부여
                    route.getOriginHub().getHubId(),
                    route.getOriginHub().getName(),
                    route.getDestinationHub().getHubId(),
                    route.getDestinationHub().getName(),
                    route.getDistance(),
                    route.getDuration()
            ));
            current = route.getOriginHub().getHubId();
        }

        Collections.reverse(segments);
        
        // sequence 부여 및 총합 계산
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

    private record Node(UUID id, double distance) {}
}
