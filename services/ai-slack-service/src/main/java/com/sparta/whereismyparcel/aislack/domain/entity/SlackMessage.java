package com.sparta.whereismyparcel.aislack.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt; // 발송 시간

    private SlackMessage(
            String slackId,
            UUID receiverId,
            String message
    ) {
        this.slackId = slackId;
        this.receiverId = receiverId;
        this.message = message;
        this.slackStatus = SlackStatus.MASSAGE_SENT; // 성공 시 생성
        this.retryCount = 0;
        this.sentAt = LocalDateTime.now();
    }

    // TODO: 생성 메서드

    // 비지니스 로직

    // TODO: 상태 전이 메서드 구현
    /*
    발송 실패시 재시도 횟수 증가 및 상태 관리
     */
    public void retry() {
        if(this.retryCount < 3)  {
            this.retryCount++;
            this.slackStatus = SlackStatus.MASSAGE_FAILED;
        } else {
            // 최대 재시도 횟수 도달 시 알림.
            this.slackStatus = SlackStatus.PERMANENT_FAILED;
        }
    }

}
