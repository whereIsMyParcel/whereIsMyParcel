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
@Table(name = "p_ai_messages", schema = "notification_db")
public class AiMessage extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "ai_id",nullable = false,updatable = false)
    private UUID aiId;

    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false,length = 30)
    private AnalysisStatus analysisStatus;

    @Lob
    @Column(name = "request_content",nullable = false,columnDefinition = "TEXT")
    private String requestContent; // 프롬프트 전문

    @Lob
    @Column(name = "response_content", nullable = false,columnDefinition = "TEXT")
    private String responseContent; // AI 응답

    @Column(name = "final_dispatch_deadline",nullable = false)
    private LocalDateTime finalDispatchDeadline; // ai가 계산한 최종 발송 시한.

    private AiMessage(
            UUID orderId,
            String requestContent,
            String responseContent,
            LocalDateTime finalDispatchDeadline
    ) {
        this.orderId = orderId;
        this.analysisStatus = AnalysisStatus.AISUCCESS; // 분석 완료 시점에 엔티티가 생성된다고 가정
        this.requestContent = requestContent;
        this.responseContent = responseContent;
        this.finalDispatchDeadline = finalDispatchDeadline;
    }

    //TODO: 생성 메서드

    // 비지니스 로직

    // TODO: 상태 전이 메서드 구현




}
