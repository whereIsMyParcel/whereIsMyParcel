package com.sparta.whereismyparcel.aislack.presentation.dto.response;

import java.time.LocalDateTime;

/**
 * 슬랙 메시지 발송 결과를 반환하기 위한 Response DTO
 */
public record SlackResponse(
        boolean isSuccess,              // 발송 성공 여부 (true/false)
        String slackId,                 // 메시지를 수신한 담당자의 슬랙 고유 ID
        String resultMessage,           // 성공 메시지 또는 에러 로그 메시지
        LocalDateTime timestamp         // 발송 처리 완료 일시
) {



    /**
     * 슬랙 발송 성공 시 응답 객체를 생성하는 팩토리 메서드
     */
    public static SlackResponse success(String slackId) {
        return new SlackResponse(
                true,
                slackId,
                "슬랙 알림 전송에 성공했습니다.",
                LocalDateTime.now()
        );
    }

    /**
     * 슬랙 발송 실패 시 에러 메시지를 담아 응답 객체를 생성하는 팩토리 메서드
     */
    public static SlackResponse fail(String slackId, String errorMessage) {
        return new SlackResponse(
                false,
                slackId,
                String.format("슬랙 알림 전송 실패: %s", errorMessage),
                LocalDateTime.now()
        );
    }
}