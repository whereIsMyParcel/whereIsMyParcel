package com.sparta.whereismyparcel.aislack.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiMessageTest {

    private final UUID TEST_ORDER_ID = UUID.randomUUID();
    private final String TEST_REQUEST_CONTENT = "Test AI request content";

    @Test
    @DisplayName("AiMessage 생성 시 초기 상태는 REQUESTED이다.")
    void createAiMessageInitialStateIsRequested() {
        // Given
        // When
        AiMessage aiMessage = AiMessage.create(TEST_ORDER_ID, TEST_REQUEST_CONTENT);

        // Then
        assertThat(aiMessage).isNotNull();
        assertThat(aiMessage.getOrderId()).isEqualTo(TEST_ORDER_ID);
        assertThat(aiMessage.getRequestContent()).isEqualTo(TEST_REQUEST_CONTENT);
        assertThat(aiMessage.getAnalysisStatus()).isEqualTo(AnalysisStatus.REQUESTED);
        assertThat(aiMessage.getResponseContent()).isNull();
        assertThat(aiMessage.getFinalDispatchDeadline()).isNull();
    }

    @Test
    @DisplayName("AI 분석 성공 시 상태는 AI_SUCCESS로 변경되고 응답 내용이 설정된다.")
    void succeedAnalysisChangesStateToAiSuccess() {
        // Given
        AiMessage aiMessage = AiMessage.create(TEST_ORDER_ID, TEST_REQUEST_CONTENT);
        String responseContent = "AI response success";
        LocalDateTime deadline = LocalDateTime.now();

        // When
        aiMessage.succeedAnalysis(responseContent, deadline);

        // Then
        assertThat(aiMessage.getAnalysisStatus()).isEqualTo(AnalysisStatus.AI_SUCCESS);
        assertThat(aiMessage.getResponseContent()).isEqualTo(responseContent);
        assertThat(aiMessage.getFinalDispatchDeadline()).isEqualTo(deadline);
    }

    @Test
    @DisplayName("AI 분석 실패 시 상태는 AI_FAIL로 변경되고 응답 내용이 초기화된다.")
    void failAnalysisChangesStateToAiFail() {
        // Given
        AiMessage aiMessage = AiMessage.create(TEST_ORDER_ID, TEST_REQUEST_CONTENT);
        aiMessage.succeedAnalysis("some response", LocalDateTime.now()); // 성공 상태로 변경 후 실패 테스트

        // When
        aiMessage.failAnalysis();

        // Then
        assertThat(aiMessage.getAnalysisStatus()).isEqualTo(AnalysisStatus.AI_FAIL);
        assertThat(aiMessage.getResponseContent()).isNull();
        assertThat(aiMessage.getFinalDispatchDeadline()).isNull();
    }

    @Test
    @DisplayName("AI 분석 실패 후 재시도를 위해 상태는 REQUESTED로 변경되고 응답 내용이 초기화된다.")
    void requeueForAnalysisChangesStateToRequested() {
        // Given
        AiMessage aiMessage = AiMessage.create(TEST_ORDER_ID, TEST_REQUEST_CONTENT);
        aiMessage.failAnalysis(); // 실패 상태로 변경 후 재시도 테스트

        // When
        aiMessage.requeueForAnalysis();

        // Then
        assertThat(aiMessage.getAnalysisStatus()).isEqualTo(AnalysisStatus.REQUESTED);
        assertThat(aiMessage.getResponseContent()).isNull();
        assertThat(aiMessage.getFinalDispatchDeadline()).isNull();
    }

    @Test
    @DisplayName("이미 성공 상태일 때 succeedAnalysis 호출 시 상태는 변하지 않는다.")
    void succeedAnalysisIdempotency() {
        // Given
        AiMessage aiMessage = AiMessage.create(TEST_ORDER_ID, TEST_REQUEST_CONTENT);
        String initialResponse = "Initial success";
        LocalDateTime initialDeadline = LocalDateTime.now().minusHours(1);
        aiMessage.succeedAnalysis(initialResponse, initialDeadline);

        String newResponse = "New success";
        LocalDateTime newDeadline = LocalDateTime.now();

        // When
        aiMessage.succeedAnalysis(newResponse, newDeadline);

        // Then
        assertThat(aiMessage.getAnalysisStatus()).isEqualTo(AnalysisStatus.AI_SUCCESS);
        assertThat(aiMessage.getResponseContent()).isEqualTo(initialResponse); // 응답 내용은 처음 성공 시 값 유지
        assertThat(aiMessage.getFinalDispatchDeadline()).isEqualTo(initialDeadline); // 마감 시한도 처음 성공 시 값 유지
    }

    @Test
    @DisplayName("이미 실패 상태일 때 failAnalysis 호출 시 상태는 변하지 않는다.")
    void failAnalysisIdempotency() {
        // Given
        AiMessage aiMessage = AiMessage.create(TEST_ORDER_ID, TEST_REQUEST_CONTENT);
        aiMessage.failAnalysis();

        // When
        aiMessage.failAnalysis();

        // Then
        assertThat(aiMessage.getAnalysisStatus()).isEqualTo(AnalysisStatus.AI_FAIL);
    }

    @Test
    @DisplayName("이미 요청 상태일 때 requeueForAnalysis 호출 시 상태는 변하지 않는다.")
    void requeueForAnalysisIdempotency() {
        // Given
        AiMessage aiMessage = AiMessage.create(TEST_ORDER_ID, TEST_REQUEST_CONTENT);

        // When
        aiMessage.requeueForAnalysis();

        // Then
        assertThat(aiMessage.getAnalysisStatus()).isEqualTo(AnalysisStatus.REQUESTED);
    }
}
