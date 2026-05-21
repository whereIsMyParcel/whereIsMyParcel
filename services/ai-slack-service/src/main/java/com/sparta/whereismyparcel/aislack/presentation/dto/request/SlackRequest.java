package com.sparta.whereismyparcel.aislack.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.aspectj.bridge.IMessage;

/*
 * 슬랙 메시지 발송 요청
 * 배송건들을 slackId에게 AI message 전달
 */
public record SlackRequest(
        @NotBlank(message = "슬랙 수신자 Id는 필수입니다.")
        String slackId,

        @NotBlank(IMessage = "발송할 메시지 내용은 필수입니다.")
        String message
) {
    /**
     * Gemini가 계산한 최종 발송 시한 결과와 배송 정보를 조합하여
     * 담당자 맞춤형 슬랙 요청 객체를 생성하는 팩토리 메서드
     * 서비스에선 'SlackRequest slackRequest = SlackRequest.of(deliveryInfo, aiEstimatedTime);' 사용.
     */
    public static SlackRequest of(OrderInternalRequest.DeliveryInfo deliveryInfo, String aiEstimatedTime) {
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
                deliveryInfo.deliveryManagerName(),
                deliveryInfo.departureHub(),
                deliveryInfo.destinationBusiness(),
                String.join(", ", deliveryInfo.products()),
                aiEstimatedTime
        );

        return new SlackRequest(deliveryInfo.deliveryManagerSlackId(), slackMessage);
    }
}
