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
        record DijkstraNodeService(UUID id, double distance) {}

        List<Hub> allHubs = hubRepository.findAll();
        List<HubRoute> allRoutes = hubRouteRepository.findAll();

        // [최적화] 인접 리스트 미리 생성 (O(V*E) -> O(V+E))
        Map<UUID, List<HubRoute>> adjacencyList = allRoutes.stream()
                .collect(Collectors.groupingBy(route -> route.getOriginHub().getHubId()));

        Map<UUID, Double> distances = new HashMap<>();
        Map<UUID, HubRoute> previousRoutes = new HashMap<>();
        Map<UUID, Integer> durations = new HashMap<>(); // [최적화] 소요 시간도 함께 관리
        
        PriorityQueue<DijkstraNodeService> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));

        for (Hub hub : allHubs) {
            distances.put(hub.getHubId(), Double.MAX_VALUE);
            durations.put(hub.getHubId(), Integer.MAX_VALUE);
        }

        if (!distances.containsKey(endNode)) {
            throw new NoPathBetweenHubsException();
        }

        distances.put(startNode, 0.0);
        durations.put(startNode, 0);
        pq.add(new DijkstraNodeService(startNode, 0.0));

        while (!pq.isEmpty()) {
            DijkstraNodeService current = pq.poll();

            if (current.distance > distances.getOrDefault(current.id, Double.MAX_VALUE)) continue;
            if (current.id.equals(endNode)) break;

            // [최적화] 필터링 대신 미리 구성된 인접 리스트 사용
            List<HubRoute> neighbors = adjacencyList.getOrDefault(current.id, Collections.emptyList());

            for (HubRoute route : neighbors) {
                UUID neighborId = route.getDestinationHub().getHubId();
                if (!distances.containsKey(neighborId)) continue;

                double newDist = distances.get(current.id) + route.getDistance();
                int newDuration = durations.get(current.id) + route.getDuration();

                if (newDist < distances.get(neighborId)) {
                    distances.put(neighborId, newDist);
                    durations.put(neighborId, newDuration);
                    previousRoutes.put(neighborId, route);
                    pq.add(new DijkstraNodeService(neighborId, newDist));
                }
            }
        }

        if (distances.get(endNode) == Double.MAX_VALUE) {
            throw new NoPathBetweenHubsException();
        }

        // 경로 복원 및 Sequence 부여 통합
        List<ShortestPathResponse.RouteSegmentResponse> finalSegments = new ArrayList<>();
        UUID currentId = endNode;
        while (previousRoutes.containsKey(currentId)) {
            HubRoute route = previousRoutes.get(currentId);
            finalSegments.add(new ShortestPathResponse.RouteSegmentResponse(
                    0, // 임시
                    route.getOriginHub().getHubId(),
                    route.getOriginHub().getName(),
                    route.getDestinationHub().getHubId(),
                    route.getDestinationHub().getName(),
                    route.getDistance(),
                    route.getDuration()
            ));
            currentId = route.getOriginHub().getHubId();
        }

        Collections.reverse(finalSegments);
        
        // [최적화] 리스트 순회하며 sequence만 다시 부여
        for (int i = 0; i < finalSegments.size(); i++) {
            ShortestPathResponse.RouteSegmentResponse s = finalSegments.get(i);
            finalSegments.set(i, new ShortestPathResponse.RouteSegmentResponse(
                    i + 1,
                    s.originHubId(),
                    s.originHubName(),
                    s.destinationHubId(),
                    s.destinationHubName(),
                    s.estimatedDistance(),
                    s.estimatedDuration()
            ));
        }

        // [최적화] 다익스트라 결과(distances, durations)에서 총합 직접 추출
        return new ShortestPathResponse(
                distances.get(endNode), 
                durations.get(endNode), 
                finalSegments
        );
    }
}
