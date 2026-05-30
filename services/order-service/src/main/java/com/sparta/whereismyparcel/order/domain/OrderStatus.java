package com.sparta.whereismyparcel.order.domain;

public enum OrderStatus {
    PENDING,
    STOCK_RESERVED,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    FAILED,
    COMPENSATION_FAILED
}
