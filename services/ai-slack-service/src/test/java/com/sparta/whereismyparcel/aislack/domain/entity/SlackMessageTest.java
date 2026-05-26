package com.sparta.whereismyparcel.aislack.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SlackMessageTest {

    private final String TEST_SLACK_ID = "U1234567890";
    private final UUID TEST_RECEIVER_ID = UUID.randomUUID();
    private final String TEST_MESSAGE_CONTENT = "Hello, Delivery Manager!";

    @Test
    @DisplayName("SlackMessage 생성 시 초기 상태는 READY_TO_SEND이다.")
    void createSlackMessageInitialStateIsReadyToSend() {
        // Given
        // When
        SlackMessage slackMessage = SlackMessage.create(TEST_SLACK_ID, TEST_RECEIVER_ID, TEST_MESSAGE_CONTENT);

        // Then
        assertThat(slackMessage).isNotNull();
        assertThat(slackMessage.getSlackId()).isEqualTo(TEST_SLACK_ID);
        assertThat(slackMessage.getReceiverId()).isEqualTo(TEST_RECEIVER_ID);
        assertThat(slackMessage.getMessage()).isEqualTo(TEST_MESSAGE_CONTENT);
        assertThat(slackMessage.getSlackStatus()).isEqualTo(SlackStatus.READY_TO_SEND);
        assertThat(slackMessage.getRetryCount()).isZero();
        assertThat(slackMessage.getSentAt()).isNull();
    }

    @Test
    @DisplayName("메시지 전송 성공 시 상태는 MESSAGE_SENT로 변경되고 sentAt이 설정된다.")
    void succeedSendingChangesStateToMessageSent() {
        // Given
        SlackMessage slackMessage = SlackMessage.create(TEST_SLACK_ID, TEST_RECEIVER_ID, TEST_MESSAGE_CONTENT);

        // When
        slackMessage.succeedSending();

        // Then
        assertThat(slackMessage.getSlackStatus()).isEqualTo(SlackStatus.MESSAGE_SENT);
        assertThat(slackMessage.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("메시지 전송 실패 시 상태는 MESSAGE_FAILED로 변경되고 retryCount가 증가한다.")
    void failSendingChangesStateToMessageFailedAndIncrementsRetryCount() {
        // Given
        SlackMessage slackMessage = SlackMessage.create(TEST_SLACK_ID, TEST_RECEIVER_ID, TEST_MESSAGE_CONTENT);

        // When
        slackMessage.failSending();

        // Then
        assertThat(slackMessage.getSlackStatus()).isEqualTo(SlackStatus.MESSAGE_FAILED);
        assertThat(slackMessage.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("최대 재시도 횟수 초과 시 permanentFailSending 호출로 상태는 PERMANENT_FAILED로 변경된다.")
    void permanentFailSendingChangesStateToPermanentFailed() {
        // Given
        SlackMessage slackMessage = SlackMessage.create(TEST_SLACK_ID, TEST_RECEIVER_ID, TEST_MESSAGE_CONTENT);
        slackMessage.failSending(); // 1
        slackMessage.failSending(); // 2
        slackMessage.failSending(); // 3

        // When
        slackMessage.permanentFailSending();

        // Then
        assertThat(slackMessage.getSlackStatus()).isEqualTo(SlackStatus.PERMANENT_FAILED);
        assertThat(slackMessage.getRetryCount()).isEqualTo(3); // retryCount는 failSending에서 이미 증가
    }

    @Test
    @DisplayName("PERMANENT_FAILED 상태에서 재전송을 위해 requeueForSending 호출 시 READY_TO_SEND로 변경되고 재시도 횟수가 초기화된다.")
    void requeueForSendingChangesStateToReadyToSendAndResetsRetryCount() {
        // Given
        SlackMessage slackMessage = SlackMessage.create(TEST_SLACK_ID, TEST_RECEIVER_ID, TEST_MESSAGE_CONTENT);
        slackMessage.failSending(); // 1
        slackMessage.failSending(); // 2
        slackMessage.failSending(); // 3
        slackMessage.permanentFailSending(); // PERMANENT_FAILED 상태

        // When
        slackMessage.requeueForSending();

        // Then
        assertThat(slackMessage.getSlackStatus()).isEqualTo(SlackStatus.READY_TO_SEND);
        assertThat(slackMessage.getRetryCount()).isZero();
        assertThat(slackMessage.getSentAt()).isNull();
    }

    @Test
    @DisplayName("이미 MESSAGE_SENT 상태일 때 succeedSending 호출 시 상태는 변하지 않는다.")
    void succeedSendingIdempotency() {
        // Given
        SlackMessage slackMessage = SlackMessage.create(TEST_SLACK_ID, TEST_RECEIVER_ID, TEST_MESSAGE_CONTENT);
        slackMessage.succeedSending();
        LocalDateTime initialSentAt = slackMessage.getSentAt();

        // When
        slackMessage.succeedSending();

        // Then
        assertThat(slackMessage.getSlackStatus()).isEqualTo(SlackStatus.MESSAGE_SENT);
        assertThat(slackMessage.getSentAt()).isEqualTo(initialSentAt); // sentAt은 처음 성공 시 값 유지
    }

    @Test
    @DisplayName("PERMANENT_FAILED 상태일 때 failSending 호출 시 상태는 변하지 않는다.")
    void failSendingWhenPermanentFailed() {
        // Given
        SlackMessage slackMessage = SlackMessage.create(TEST_SLACK_ID, TEST_RECEIVER_ID, TEST_MESSAGE_CONTENT);
        slackMessage.failSending(); // 1
        slackMessage.failSending(); // 2
        slackMessage.failSending(); // 3
        slackMessage.permanentFailSending(); // PERMANENT_FAILED 상태

        // When
        slackMessage.failSending(); // 다시 실패 호출

        // Then
        assertThat(slackMessage.getSlackStatus()).isEqualTo(SlackStatus.PERMANENT_FAILED);
        assertThat(slackMessage.getRetryCount()).isEqualTo(3); // retryCount는 더 이상 증가하지 않음
    }

    @Test
    @DisplayName("canRetry는 retryCount가 3 미만일 때 true를 반환한다.")
    void canRetryReturnsTrueWhenRetryCountLessThanThree() {
        // Given
        SlackMessage slackMessage = SlackMessage.create(TEST_SLACK_ID, TEST_RECEIVER_ID, TEST_MESSAGE_CONTENT);
        assertThat(slackMessage.canRetry()).isTrue(); // 0
        slackMessage.failSending(); // 1
        assertThat(slackMessage.canRetry()).isTrue();
        slackMessage.failSending(); // 2
        assertThat(slackMessage.canRetry()).isTrue();
    }

    @Test
    @DisplayName("canRetry는 retryCount가 3 이상일 때 false를 반환한다.")
    void canRetryReturnsFalseWhenRetryCountIsThreeOrMore() {
        // Given
        SlackMessage slackMessage = SlackMessage.create(TEST_SLACK_ID, TEST_RECEIVER_ID, TEST_MESSAGE_CONTENT);
        slackMessage.failSending(); // 1
        slackMessage.failSending(); // 2
        slackMessage.failSending(); // 3

        // When & Then
        assertThat(slackMessage.canRetry()).isFalse();

        slackMessage.permanentFailSending(); // PERMANENT_FAILED 상태
        assertThat(slackMessage.canRetry()).isFalse();
    }
}
