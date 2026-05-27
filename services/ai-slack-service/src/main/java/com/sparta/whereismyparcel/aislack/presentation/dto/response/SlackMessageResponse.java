package com.sparta.whereismyparcel.aislack.presentation.dto.response;

import com.sparta.whereismyparcel.aislack.domain.entity.SlackMessage;
import com.sparta.whereismyparcel.aislack.domain.entity.SlackStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 슬랙 메시지 상세 정보를 반환하기 위한 Response DTO
 * SlackMessage 엔티티의 필드를 포함합니다.
 */
public record SlackMessageResponse(
        UUID id,
        String slackId,
        UUID receiverId,
        String message,
        SlackStatus slackStatus,
        int retryCount,
        LocalDateTime sentAt
) {
    /**
     * SlackMessage 엔티티로부터 SlackMessageResponse DTO를 생성하는 팩토리 메서드
     * @param slackMessage SlackMessage 엔티티
     * @return SlackMessageResponse DTO
     */
    public static SlackMessageResponse from(SlackMessage slackMessage) {
        return new SlackMessageResponse(
                slackMessage.getId(),
                slackMessage.getSlackId(),
                slackMessage.getReceiverId(),
                slackMessage.getMessage(),
                slackMessage.getSlackStatus(),
                slackMessage.getRetryCount(),
                slackMessage.getSentAt()
        );
    }
}
