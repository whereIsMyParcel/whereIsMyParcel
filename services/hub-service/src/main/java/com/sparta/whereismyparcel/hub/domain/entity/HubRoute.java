package com.sparta.whereismyparcel.hub.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_hub_routes", schema = "hub_db")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class HubRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hub_route_id", columnDefinition = "UUID")
    private UUID hubRouteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_hub_id", nullable = false)
    private Hub originHub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_hub_id", nullable = false)
    private Hub destinationHub;

    @Column(nullable = false)
    private Double distance; // 거리 (km)

    @Column(nullable = false)
    private Integer duration; // 소요 시간 (분)

    @Builder(access = AccessLevel.PRIVATE)
    private HubRoute(Hub originHub, Hub destinationHub, Double distance, Integer duration) {
        validateHubs(originHub, destinationHub);
        validateMetrics(distance, duration);
        this.originHub = originHub;
        this.destinationHub = destinationHub;
        this.distance = distance;
        this.duration = duration;
    }

    // 정적 팩토리 메서드
    public static HubRoute create(Hub originHub, Hub destinationHub, Double distance, Integer duration) {
        return HubRoute.builder()
                .originHub(originHub)
                .destinationHub(destinationHub)
                .distance(distance)
                .duration(duration)
                .build();
    }

    // 비즈니스 로직: 경로 정보 수정
    public void update(Double distance, Integer duration) {
        validateMetrics(distance, duration);
        this.distance = distance;
        this.duration = duration;
    }

    private void validateHubs(Hub origin, Hub dest) {
        if (origin == null || dest == null) {
            throw new IllegalArgumentException("출발지와 목적지 허브는 필수입니다.");
        }
        if (origin.equals(dest)) {
            throw new IllegalArgumentException("출발지와 목적지 허브가 같을 수 없습니다.");
        }
    }

    private void validateMetrics(Double distance, Integer duration) {
        if (distance == null || distance <= 0) {
            throw new IllegalArgumentException("거리는 0보다 커야 합니다.");
        }
        if (duration == null || duration <= 0) {
            throw new IllegalArgumentException("소요 시간은 0보다 커야 합니다.");
        }
    }
}
