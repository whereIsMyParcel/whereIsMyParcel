package com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response;

import com.sparta.whereismyparcel.aislack.presentation.dto.response.SlackResponse;

import java.util.List;
import java.util.UUID;

/**
 * 벌크 배송 건들에 대한 AI 분석 및 슬랙 전송 결과를 총괄하여 반환하는 응답 DTO
 */
public record AnalysisResponse(
        UUID orderId,                           // 대상 메인 주문 ID
        int totalCount,                         // 전체 요청된 배송 건수
        int successCount,                       // 슬랙 전송 성공 건수
        int failCount,                          // 슬랙 전송 실패 건수
        List<SlackResponse> details             // 각 배송 건별 상세 전송 결과 목록
) {
    /**
     * 상세 결과 목록(SlackResponse List)을 받아 총괄 통계를 자동 계산하여
     * AnalysisResponse 객체를 생성하는 팩토리 메서드
     */
    public static AnalysisResponse of(UUID orderId, List<SlackResponse> details) {
        int total = details.size();

        int success = (int) details.stream().filter(SlackResponse::isSuccess).count();
        int fail = total - success;

        return new AnalysisResponse(orderId, total, success, fail, details);
    }
}