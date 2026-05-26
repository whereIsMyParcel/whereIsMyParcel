package com.sparta.whereismyparcel.shipment.domain.entity;

import lombok.Getter;

@Getter
public enum ShipmentStatus {
    HUB_WAITING("허브 이동 대기 중"),
    HUB_MOVING("허브 이동 중"),
    HUB_ARRIVED("목적지 허브 도착"),
    COMPANY_MOVING("업체 이동 중"),
    DELIVERED("배송 완료"),
    CANCELLED("배송 취소");

    private final String description;

    ShipmentStatus(String description) {
        this.description = description;
    }

    public boolean canCancel() {
        return this == HUB_WAITING;
    }
}
