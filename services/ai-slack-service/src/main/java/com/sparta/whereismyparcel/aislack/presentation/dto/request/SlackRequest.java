package com.sparta.whereismyparcel.aislack.presentation.dto.request;

import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.OrderResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.ShipmentResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.UserResponse;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.stream.Collectors;

/*
 * 슬랙 메시지 발송 요청
 * 배송건들을 slackId에게 AI message 전달
 */
public record SlackRequest(
        @NotBlank(message = "슬랙 수신자 Id는 필수입니다.")
        String slackId,

        @NotBlank(message = "발송할 메시지 내용은 필수입니다.")
        String message
) {
    /**
     * Gemini가 계산한 최종 발송 시한 결과와 배송 정보를 조합하여
     * 담당자 맞춤형 슬랙 요청 객체를 생성하는 팩토리 메서드
     * 서비스에선 'SlackRequest slackRequest = SlackRequest.of(orderResponse, shipmentResponses, userResponse, aiEstimatedTime);' 사용.
     */
    public static SlackRequest of(OrderResponse orderResponse, List<ShipmentResponse> shipmentResponses, UserResponse userResponse, String aiEstimatedTime) {
        // 배송 품목은 OrderResponse의 shipmentItems 필드를 사용합니다.
        // 모든 배송의 품목을 합쳐서 표시합니다.
        List<String> allItems = orderResponse.shipmentItems().stream()
                .flatMap(osi -> osi.items().stream())
                .distinct() // 중복 제거
                .collect(Collectors.toList());
        String products = allItems.isEmpty() ? "정보 없음" : String.join(", ", allItems);

        // 출발 허브와 최종 목적지는 ShipmentResponse에서 가져올 수 있습니다.
        // 여기서는 첫 번째 ShipmentResponse를 기준으로 합니다.
        // 실제로는 여러 ShipmentResponse가 있을 수 있으므로, 로직을 더 정교하게 다듬어야 할 수 있습니다.
        String departureHub = shipmentResponses.isEmpty() ? "정보 없음" : shipmentResponses.get(0).originHubId().toString(); // TODO: Hub ID를 이름으로 변환하는 로직 필요
        String destinationBusiness = orderResponse.recipientAddress(); // OrderResponse에서 최종 목적지 주소 사용


        // deliveryManagerName은 UserResponse의 name을 사용하고, deliveryManagerSlackId는 UserResponse의 slackId를 사용합니다.
        String deliveryManagerName = userResponse.name();
        String deliveryManagerSlackId = userResponse.slackId();


        String slackMessage = String.format(
                """
                🔔 [배송 출발 시한 알림]
                
                안녕하세요, %s 담당자님!
                Gemini AI가 분석한 해당 배송 건의 '최종 발송 시한'을 안내해 드립니다.
                납기 조율 및 허브 출차에 참고하시기 바랍니다.
                
                ■ 배송 정보
                - 출발 허브: %s
                - 최종 목적지: %s
                - 배송 품목: %s
                
                🚨 [AI 분석 결과] 최종 발송 시한
                👉 %s
                
                * 본 시간은 담당자님의 근무 시간(09:00 ~ 18:00) 및 경유지 배송 적재 시간을 고려하여 연산된 결과입니다. 늦지 않게 출차를 준비해 주세요!
                """,
                deliveryManagerName,
                departureHub,
                destinationBusiness,
                products,
                aiEstimatedTime
        );

        return new SlackRequest(deliveryManagerSlackId, slackMessage);
    }
}
