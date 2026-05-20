package com.sparta.whereismyparcel.hub.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hub_id", columnDefinition = "UUID")
    private UUID hubId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    /// 빌더 수정 !
    @Builder(access = AccessLevel.PRIVATE) // 내부 빌더
    private Hub(String name, String address, Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // 정적 팩토리 메서드
    public static Hub create(String name, String address, Double latitude, Double longitude) {
        return Hub.builder()
                .name(name)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    // 비즈니스 로직: 허브 정보 수정
    public void update(String name, String address, Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // 위경도 유효성 검증
    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("위도(latitude)는 -90에서 90 사이여야 합니다.");
        }
        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("경도(longitude)는 -180에서 180 사이여야 합니다.");
        }
    }
}
