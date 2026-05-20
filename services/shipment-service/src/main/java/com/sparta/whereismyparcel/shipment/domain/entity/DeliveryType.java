package com.sparta.whereismyparcel.shipment.domain.entity;

import lombok.Getter;

@Getter
public enum DeliveryType {

    HUB_DELIVERY("허브 배송 담당자"),
    COMPANY_DELIVERY("업체 배송 담당자");

    private final String description;

    DeliveryType(String description) {
        this.description = description;
    }
}
