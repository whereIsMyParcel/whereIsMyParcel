package com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
/*
필수 요구사항:
주문 생성 시 발송 허브 담당자 알림 및 AI 시한 계산을 위한 내부 요청 DTO
배송 서비스가 FeignClient를 통해 AI-Slack 서비스로 데이터를 넘겨줄 때 사용합니다.
 */
public record OrderInternalRequest(

        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @Valid
        @NotNull(message = "주문 회사 정보는 필수입니다.")
        String businessCustomerName,

        @NotNull(message = "주문자 이메일은 필수입니다.")
        String businessCustomerEmail,

        @NotNull(message = "주문 시간은 필수입니다.")
        LocalDateTime orderDateTime,

        @Valid
        @NotNull(message = "상품 정보 목록은 필수입니다.")
        @Size(min = 1, message = "최소 하나 이상의 상품 정보가 필요합니다.")
        List<String> products,

        @Size(max = 500, message = "주문 요청 사항은 최대 500자까지 가능합니다.")
        String orderRequirement,

        @Valid
        @NotNull(message = "배송 건 목록은 필수입니다.")
        @Size(min = 1, message = "최소 하나 이상의 배송 건이 필요합니다.")
        List<String> delieveriesInfo

) {
    /*
     * 각각의 독립된 배송 1건에 대한 정보
     */
    public record  DeliveryInfo(
            @NotNull(message = "상품 정보 목록은 필수입니다.")
            @Size(min = 1, message = "최소 하나 이상의 상품 정보가 필요합니다.")
            List<String> products,

            @NotNull(message = "발송지(허브) 정보는 필수입니다.")
            String departureHub,

            @Valid
            @NotNull(message = "경유지 목록은 필수입니다.")
            List<String> pathInfo,

            @NotNull(message = "도착지 정보는 필수입니다.")
            String destinationBusiness,

            @Valid
            @NotNull(message = "배송 담당자 이름는 필수입니다.")
            String deliveryManagerName,

            @NotNull(message = "배송 담당자 이메일은 필수입니다.")
            String deliveryManagerEmail,

            @NotNull(message = "배송 담당자 슬랙 ID는 필수입니다.")
            String deliveryManagerSlackId
    ){}

}
