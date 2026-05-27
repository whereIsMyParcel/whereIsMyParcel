package com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/*
필수 요구사항:
Order 서비스에서 AI-Slack 서비스로 주문 ID를 전달하기 위한 내부 요청 DTO
AI-Slack 서비스는 이 주문 ID를 사용하여 Order 서비스로부터 주문 및 배송 데이터를 조회합니다.
 */
public record OrderInternalRequest(

        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId
) {
}
