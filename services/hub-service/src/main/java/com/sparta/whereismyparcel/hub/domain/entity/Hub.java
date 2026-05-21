package com.sparta.whereismyparcel.hub.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import com.sparta.whereismyparcel.hub.presentation.dto.request.CreateHubRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.request.UpdateHubRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_hubs", schema = "hub_db")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Hub extends BaseEntity {

    @Id
    @Column(name = "hub_id", updatable = false)
    private UUID hubId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Builder(access = AccessLevel.PRIVATE)
    private Hub(UUID hubId, String name, String address, Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        this.hubId = hubId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // 정적 팩토리 메서드
    public static Hub create(CreateHubRequest request) {
        return Hub.builder()
                .hubId(UUID.randomUUID())
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
    }


    public void update(UpdateHubRequest request) {
        validateCoordinates(request.latitude(), request.longitude());
        this.name = request.name();
        this.address = request.address();
        this.latitude = request.latitude();
        this.longitude = request.longitude();
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("위도(latitude)는 -90에서 90 사이여야 합니다.");
        }
        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("경도(longitude)는 -180에서 180 사이여야 합니다.");
        }
    }
}
