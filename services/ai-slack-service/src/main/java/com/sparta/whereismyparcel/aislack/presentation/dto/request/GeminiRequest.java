package com.sparta.whereismyparcel.aislack.presentation.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/*
 * Google Gemini API 요청 DTO
 * Gemini의 표준 입력 스키마인 { "contents": [ { "parts": [ { "text": "..." } ] } ] } 구조를 따릅니다.
 */
public record GeminiRequest(
        @NotEmpty(message = "콘텐츠 바디는 비어있을 수 없습니다.")
        List<Content> contents // 💡 여기에 있던 세미콜론(;)을 제거했습니다!
) {
    public record Content(
            List<Part> parts
    ){}

    public record Part(
            String text
    ){}

    /*
     * OrderInternalRequest 데이터를 기반으로 Gemini 전용 프롬프트 조립하여
     * GeminiRequest 객체를 생성하는 팩토리 메서드
     * service에서 루프를 돌며 각 배송 건 당 이 메서드 호출해서 GeminiRequest 생성
     */
    public static GeminiRequest from(
            OrderInternalRequest orderInternalRequest,
            OrderInternalRequest.DeliveryInfo deliveryInfo
    ) {
        String prompt = String.format(
                """
                당신은 물류 전문 AI 어시스턴트입니다. 아래 제공된 배송 정보를 바탕으로, 
                물류를 요청한 업체가 원하는 시간에 목적지에 도착할 수 있도록 '최종 발송 시한(늦어도 언제 출발해야 하는지)'을 계산해 주세요.
                
                [제약 조건]
                - 배송 담당자의 근무 시간은 항시 '09:00 ~ 18:00'입니다. 근무 시간 외에는 배송이 진행되지 않거나 지연됨을 고려해야 합니다
                
                [주문 및 배송 데이터]
                1. 메인 주문 일시: %s
                2. 주문 요청 사항(납기 시한): %s
                3. 이번 배송 건 상품 정보: %s
                4. 발송지(허브): %s
                5. 경유지 목록: %s
                6. 도착지(대상 비즈니스): %s    
                        
                [출력 요구사항]
                - 위 정보를 모두 연산하여 납기를 맞추기 위한 최종 발송 시한을 계산하세요.
                - 응답 형식은 반드시 'YYYY-MM-DD HH:mm:ss' 형태를 포함한 명확한 최종 발송 시한 일시가 드러나야 합니다.
                """,
                orderInternalRequest.orderDateTime().toString(),
                orderInternalRequest.orderRequirement(),
                String.join(", ", deliveryInfo.products()),
                deliveryInfo.departureHub(),
                String.join(", ", deliveryInfo.pathInfo()),
                deliveryInfo.destinationBusiness()
        );
        return new GeminiRequest(List.of(new Content(List.of(new Part(prompt)))));
    }
}