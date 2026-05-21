package com.sparta.whereismyparcel.hub.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_delivery_managers", schema = "hub_db")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class DeliveryManager extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "delivery_manager_id", columnDefinition = "UUID")
    private UUID deliveryManagerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @Column(nullable = false, unique = true)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryManagerRole type; // HUB, COMPANY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryManagerStatus status; // AVAILABLE, ASSIGNED

    @Column(nullable = false)
    private Integer deliveryOrder; // 배정 순번

    public enum DeliveryManagerRole {
        HUB, COMPANY
    }

    public enum DeliveryManagerStatus {
        AVAILABLE, ASSIGNED
    }

    @Builder(access = AccessLevel.PRIVATE)
    private DeliveryManager(Hub hub, String slackId, DeliveryManagerRole type, DeliveryManagerStatus status, Integer deliveryOrder) {
        this.hub = hub;
        this.slackId = slackId;
        this.type = type;
        this.status = status;
        this.deliveryOrder = deliveryOrder;
    }

    // 정적 팩토리 메서드
    public static DeliveryManager create(Hub hub, String slackId, DeliveryManagerRole type, Integer deliveryOrder) {
        return DeliveryManager.builder()
                .hub(hub)
                .slackId(slackId)
                .type(type)
                .status(DeliveryManagerStatus.AVAILABLE) // 초기 상태는 항상 AVAILABLE
                .deliveryOrder(deliveryOrder)
                .build();
    }

    // 비즈니스 로직: 상태 변경
    public void assign() {
        this.status = DeliveryManagerStatus.ASSIGNED;
    }

    public void unassign() {
        this.status = DeliveryManagerStatus.AVAILABLE;
    }
}
