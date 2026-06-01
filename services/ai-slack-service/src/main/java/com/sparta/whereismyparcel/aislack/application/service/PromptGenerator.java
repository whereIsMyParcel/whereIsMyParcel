package com.sparta.whereismyparcel.aislack.application.service;

import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.OrderResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.ShipmentResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptGenerator {
    public String createGeminiPrompt(OrderResponse order, List<ShipmentResponse> shipments, UserResponse recipientUser) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(
                """
                당신은 물류 전문 AI 어시스턴트입니다. 아래 제공된 배송 정보를 바탕으로,
                물류를 요청한 업체가 원하는 시간에 목적지에 도착할 수 있도록 '최종 발송 시한(늦어도 언제 출발해야 하는지)'을 계산해 주세요.
                
                [제약 조건]
                - 배송 담당자의 근무 시간은 항시 '09:00 ~ 18:00'입니다. 근무 시간 외에는 배송이 진행되지 않거나 지연됨을 고려해야 합니다
                 [출력 요구사항]
                - 위 정보를 모두 연산하여 납기를 맞추기 위한 최종 발송 시한을 계산하세요.
                - 응답 형식은 반드시 'DEADLINE:YYYY-MM-DDTHH:mm:ss' 형태를 포함한 명확한 최종 발송 시한 일시가 드러나야 합니다.
                """);

        promptBuilder.append("Order Details:\n");
        promptBuilder.append("  - Order ID: ").append(order.orderId()).append("\n");
        promptBuilder.append("  - Order Number: ").append(order.orderNumber()).append("\n");
        promptBuilder.append("  - Recipient Name: ").append(order.recipientName()).append("\n");
        promptBuilder.append("  - Recipient Address: ").append(order.recipientAddress()).append("\n");
        promptBuilder.append("  - Requested Delivery At: ").append(order.requestedDeliveryAt()).append("\n");
        promptBuilder.append("  - Order Status: ").append(order.orderStatus()).append("\n"); //append("\n\n")
        promptBuilder.append("  - Order requestMemo: ").append(order.requestMemo()).append("\n");
        promptBuilder.append("  - Order orderAt: ").append(order.orderedAt()).append("\n");
        promptBuilder.append("  - Order items: ").append(
                promptBuilder.append("  - Order items: ").append(
                                order.shipmentItems() == null ? "정보 없음" :
                                        String.join(", ",
                                                order.shipmentItems().stream()
                                                        .filter(osi -> osi != null && osi.items() != null)
                                                        .flatMap(osi -> osi.items().stream())
                                                        .distinct().toList())).append("\n\n"));

        promptBuilder.append("Shipment Details:\n");
        shipments.forEach(shipment -> {
            promptBuilder.append("  - Shipment ID: ").append(shipment.id()).append("\n");
            promptBuilder.append("  - Shipment Number: ").append(shipment.shipmentNumber()).append("\n");
            promptBuilder.append("  - Origin Hub ID: ").append(shipment.originHubId()).append("\n");
            promptBuilder.append("  - Destination Hub ID: ").append(shipment.destinationHubId()).append("\n");
            promptBuilder.append("  - Delivery Address: ").append(shipment.deliveryAddress()).append("\n");
            promptBuilder.append("  - Estimated Delivery At: ").append(shipment.estimatedDeliveryAt()).append("\n");
            promptBuilder.append("  - Shipment Status: ").append(shipment.shipmentStatus()).append("\n");
            promptBuilder.append("  - Recipient Slack ID: ").append(shipment.recipientSlackId()).append("\n");
            promptBuilder.append("  - Company Delivery Manager ID: ").append(shipment.companyDeliveryManagerId()).append("\n");
            promptBuilder.append("---\n");
        });

        promptBuilder.append("\n");

        promptBuilder.append("Recipient User Details (for Slack message):\n");
        promptBuilder.append("  - User ID: ").append(recipientUser.userId()).append("\n");
        promptBuilder.append("  - Username: ").append(recipientUser.username()).append("\n");
        promptBuilder.append("  - Slack ID: ").append(recipientUser.slackId()).append("\n\n");

        promptBuilder.append("Please provide the Slack message and the final dispatch deadline.\n");

        return promptBuilder.toString();
    }

    /*
     * 비동기 방식
     *
     * OrderInternalRequest 데이터를 기반으로 Gemini 전용 프롬프트 조립하여
     * GeminiRequest 객체를 생성하는 팩토리 메서드
     * service에서 루프를 돌며 각 배송 건 당 이 메서드 호출해서 GeminiRequest 생성
     */
    /*public GeminiRequest from(
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
        return new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));
    }*/
}