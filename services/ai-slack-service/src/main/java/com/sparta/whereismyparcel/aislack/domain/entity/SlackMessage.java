package com.sparta.whereismyparcel.aislack.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_slack_messages", schema = "notification_db")
public class SlackMessage extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "slack_id", nullable = false, length = 100)
    private String slackId; //슬랙 사용자 id

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId; //수신자 id

    @Lob
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false, length = 30)
    private SlackStatus slackStatus;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "sent_at") // nullable = false 제거
    private LocalDateTime sentAt; // 발송 시간

    @Builder(access = AccessLevel.PRIVATE)
    private SlackMessage(
            String slackId,
            UUID receiverId,
            String message
    ) {
        this.slackId = slackId;
        this.receiverId = receiverId;
        this.message = message;
        this.slackStatus = SlackStatus.READY_TO_SEND; // 초기 상태 READY_TO_SEND
        this.retryCount = 0;
        // sentAt은 초기에는 null
    }

    public static SlackMessage create(
            String slackId,
            UUID receiverId,
            String message
    ) {
        return SlackMessage.builder()
                .slackId(slackId)
                .receiverId(receiverId)
                .message(message)
                .build();
    }

    // Business logic for state transitions

    /**
     * Slack 메시지 전송 성공 시 상태를 MESSAGE_SENT로 변경합니다.
     */
    public void succeedSending() {
        if (this.slackStatus == SlackStatus.MESSAGE_SENT) {
            // 이미 성공 상태이므로 추가 처리 없음 (멱등성 보장)
            return;
        }
        this.slackStatus = SlackStatus.MESSAGE_SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * Slack 메시지 전송 실패 시 상태를 MESSAGE_FAILED로 변경하고 재시도 횟수를 증가시킵니다.
     */
    public void failSending() {
        if (this.slackStatus == SlackStatus.PERMANENT_FAILED) {
            // 이미 영구 실패 상태이므로 더 이상 재시도 불가
            return;
        }
        this.slackStatus = SlackStatus.MESSAGE_FAILED;
        this.retryCount++;
    }

    /**
     * 최대 재시도 횟수 초과 시 상태를 PERMANENT_FAILED로 변경합니다.
     */
    public void permanentFailSending() {
        this.slackStatus = SlackStatus.PERMANENT_FAILED;
    }

    /**
     * PERMANENT_FAILED 상태의 메시지를 재전송하기 위해 READY_TO_SEND 상태로 변경하고 재시도 횟수를 초기화합니다.
     */
    public void requeueForSending() {
        this.slackStatus = SlackStatus.READY_TO_SEND;
        this.retryCount = 0;
        this.sentAt = null; // 재전송을 위해 발송 시간 초기화
    }

    public void updateMessage(String newMessageContent) {
        this.message = newMessageContent;
    }

    public boolean canRetry() {
        return this.retryCount < 3; // 최대 재시도 횟수 3번으로 가정
    }
}
