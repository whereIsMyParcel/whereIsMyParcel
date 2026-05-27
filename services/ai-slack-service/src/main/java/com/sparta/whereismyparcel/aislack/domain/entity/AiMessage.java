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
    @Column(name = "response_content", columnDefinition = "TEXT") // nullable = false 제거
    private String responseContent; // AI 응답

    @Column(name = "final_dispatch_deadline") // nullable = false 제거
    private LocalDateTime finalDispatchDeadline; // ai가 계산한 최종 발송 시한.

    // Constructor for initial creation (REQUESTED state)
    @Builder(access = AccessLevel.PRIVATE)
    private AiMessage(
            UUID orderId,
            String requestContent
    ) {
        this.orderId = orderId;
        this.analysisStatus = AnalysisStatus.REQUESTED; // 생성 시 REQUESTED 상태
        this.requestContent = requestContent;
        // responseContent and finalDispatchDeadline are null initially
    }

    public static AiMessage create(
            UUID orderId,
            String requestContent
    ){
        return AiMessage.builder()
                .orderId(orderId)
                .requestContent(requestContent)
                .build();
    }

    // Business logic for state transitions

    /**
     * AI 분석 성공 시 상태를 AI_SUCCESS로 변경하고 응답 내용을 업데이트합니다.
     * @param responseContent AI 응답 내용
     * @param finalDispatchDeadline 최종 발송 시한
     */
    public void succeedAnalysis(String responseContent, LocalDateTime finalDispatchDeadline) {
        if (this.analysisStatus == AnalysisStatus.AI_SUCCESS) {
            // 이미 성공 상태이므로 추가 처리 없음 (멱등성 보장)
            return;
        }
        this.analysisStatus = AnalysisStatus.AI_SUCCESS;
        this.responseContent = responseContent;
        this.finalDispatchDeadline = finalDispatchDeadline;
    }

    /**
     * AI 분석 실패 시 상태를 AI_FAIL로 변경합니다.
     */
    public void failAnalysis() {
        if (this.analysisStatus == AnalysisStatus.AI_FAIL) {
            // 이미 실패 상태이므로 추가 처리 없음 (멱등성 보장)
            return;
        }
        this.analysisStatus = AnalysisStatus.AI_FAIL;
        this.responseContent = null; // 실패 시 이전 응답 내용 초기화
        this.finalDispatchDeadline = null; // 실패 시 이전 마감 시한 초기화
    }

    /**
     * AI 분석 실패 후 재시도를 위해 상태를 REQUESTED로 변경합니다.
     */
    public void requeueForAnalysis() {
        if (this.analysisStatus == AnalysisStatus.REQUESTED) {
            // 이미 요청 상태이므로 추가 처리 없음 (멱등성 보장)
            return;
        }
        this.analysisStatus = AnalysisStatus.REQUESTED;
        this.responseContent = null; // 재시도 시 이전 응답 내용 초기화
        this.finalDispatchDeadline = null; // 재시도 시 이전 마감 시한 초기화
    }
}
