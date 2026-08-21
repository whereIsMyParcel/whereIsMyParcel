package com.sparta.whereismyparcel.order.presentation.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 상태 기준 orderId 목록 조회 응답(internal). logistics-agent scheduled scan(design §16.2)이
 * 고장 후보를 열거하는 데 쓴다. 상세는 담지 않고 orderId만 반환한다(방향 A, 경량 계약).
 */
public record OrderIdsResponse(List<UUID> orderIds) {

    public static OrderIdsResponse from(List<UUID> orderIds) {
        return new OrderIdsResponse(orderIds);
    }
}
