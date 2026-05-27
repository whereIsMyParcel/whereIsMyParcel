package com.sparta.whereismyparcel.aislack.application.service;

import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.sparta.whereismyparcel.aislack.domain.entity.AnalysisStatus;
import com.sparta.whereismyparcel.aislack.domain.entity.SlackMessage;
import com.sparta.whereismyparcel.aislack.domain.entity.SlackStatus;
import com.sparta.whereismyparcel.aislack.domain.exception.AiSlackErrorCode;
import com.sparta.whereismyparcel.aislack.domain.repository.SlackMessageRepository;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.OrderResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.ShipmentResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.UserResponse;
import com.sparta.whereismyparcel.aislack.presentation.dto.request.SlackRequest;
import com.sparta.whereismyparcel.aislack.presentation.dto.response.SlackMessageResponse;
import com.sparta.whereismyparcel.common.exception.BusinessException;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackMessageService {

    private final SlackMessageRepository slackMessageRepository;

    @Value("${slack.bot.token}") // 토큰을 클래스 내부로 직접 주입
    private String slackToken;

    /**
     * 특정 Slack 메시지 단건 조회
     * @param messageId 조회할 Slack 메시지 ID
     * @return Slack 메시지 정보 DTO
     */
    public SlackMessageResponse getSlackMessage(UUID messageId) {
        SlackMessage slackMessage = slackMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(AiSlackErrorCode.SLACK_MESSAGE_NOT_FOUND, "Slack message not found with id: " + messageId));
        return SlackMessageResponse.from(slackMessage);
    }

    /**
     * Slack 메시지 목록 조회 (페이징 및 필터링 가능)
     * @param orderId (선택 사항) 특정 주문 ID에 해당하는 메시지 필터링 (현재 SlackMessage 엔티티에 orderId 필드가 없으므로, 이 부분은 로직 추가 필요)
     * @param status (선택 사항) 특정 SlackStatus에 해당하는 메시지 필터링
     * @param pageable 페이징 정보
     * @return Slack 메시지 목록 DTO (Page 객체)
     */
    public Page<SlackMessageResponse> getAiMessages(UUID orderId, AnalysisStatus status, Pageable pageable) {
        Page<SlackMessage> slackMessagesPage;

        // SlackMessage 엔티티에 orderId 필드가 없으므로, orderId 필터링은 현재 불가능합니다.
        // 여기서는 status 필터링만 구현합니다.
        if (status != null) {
            try {
                SlackStatus slackStatus = SlackStatus.valueOf(status.name());
                slackMessagesPage = slackMessageRepository.findBySlackStatus(slackStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(AiSlackErrorCode.INVALID_INPUT_VALUE, "Invalid SlackStatus value: " + status.name());
            }
        } else {
            slackMessagesPage = slackMessageRepository.findAll(pageable);
        }

        return slackMessagesPage.map(SlackMessageResponse::from);
    }

    /**
     * Slack 메시지 업데이트
     * AiSlackController의 updateAiMessage 메서드에 맞춰 시그니처 변경
     * @param messageId 업데이트할 Slack 메시지 ID
     * @param request 업데이트할 정보 DTO
     * @return 업데이트된 Slack 메시지 정보 DTO
     */
    @Transactional
    public SlackMessageResponse updateAiMessage(UUID messageId, SlackRequest request) {
        SlackMessage slackMessage = slackMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(AiSlackErrorCode.SLACK_MESSAGE_NOT_FOUND, "Slack message not found with id: " + messageId));

        slackMessage.updateMessage(request.message());

        SlackMessage updatedMessage = slackMessageRepository.save(slackMessage);
        return SlackMessageResponse.from(updatedMessage);
    }

    /**
     * Slack 메시지 삭제
     * AiSlackController의 deleteAiMessage 메서드에 맞춰 시그니처 변경
     * @param messageId 삭제할 Slack 메시지 ID
     */
    @Transactional
    public void deleteAiMessage(UUID messageId) {
        if (!slackMessageRepository.existsById(messageId)) {
            throw new BusinessException(AiSlackErrorCode.SLACK_MESSAGE_NOT_FOUND, "Slack message not found with id: " + messageId);
        }
        slackMessageRepository.deleteById(messageId);
    }

    /**
     * AI 분석 성공 시 Slack 메시지 생성 및 발송을 시도합니다.
     * @param aiMessageId 관련 AI 메시지 ID
     * @param orderResponse 주문 정보
     * @param shipmentResponses 배송 정보 목록
     * @param recipientUser 수령인 사용자 정보
     * @param finalDispatchDeadline 최종 발송 시한
     * @return 생성된 Slack 메시지 정보 DTO
     */
    @Transactional
    public SlackMessageResponse sendSlackNotification(
            UUID aiMessageId,
            OrderResponse orderResponse,
            List<ShipmentResponse> shipmentResponses,
            UserResponse recipientUser,
            LocalDateTime finalDispatchDeadline
    ) {
        String formattedDispatchDeadline = finalDispatchDeadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        SlackRequest slackRequest = SlackRequest.of(orderResponse, shipmentResponses, recipientUser, formattedDispatchDeadline);

        //  수정: 다른 메서드들과 파라미터 순서 정합성 통일 (receiverId 위치에 aiMessageId 배치)
        SlackMessage newSlackMessage = SlackMessage.create(
                slackRequest.slackId(),
                aiMessageId,
                slackRequest.message()
        );
        SlackMessage savedMessage = slackMessageRepository.save(newSlackMessage);

        try {
            //  수정: 외부 Client 클래스 없이 클래스 상단에 주입된 slackToken으로 즉석 발송
            ChatPostMessageResponse response = Slack.getInstance().methods(slackToken).chatPostMessage(req -> req
                    .channel(slackRequest.slackId())
                    .text(slackRequest.message())
            );

            //  수정: 슬랙 공식 응답 규격인 response.isOk()로 성공 여부 판단
            if (response.isOk()) {
                savedMessage.succeedSending();
                log.info("Slack 토큰 기반 알림 발송 성공: aiMessageId={}, slackId={}", aiMessageId, slackRequest.slackId());
            } else {
                savedMessage.failSending();
                log.warn("Slack 토큰 발송 실패 (API 에러): aiMessageId={}, error={}", aiMessageId, response.getError());
            }
        } catch (Exception e) {
            savedMessage.failSending();
            log.error("Slack 알림 발송 중 예외 발생: aiMessageId={}, error={}", aiMessageId, e.getMessage(), e);
        } finally {
            slackMessageRepository.save(savedMessage);
        }

        return SlackMessageResponse.from(savedMessage);
    }

    /**
     * Slack 메시지 생성 (내부 호출용) - 기존 메서드는 sendSlackNotification으로 대체될 수 있음
     * @param slackId 슬랙 사용자 ID
     * @param receiverId 수신자 ID
     * @param messageContent 메시지 내용
     * @return 생성된 Slack 메시지 정보 DTO
     */
    @Transactional
    public SlackMessageResponse createSlackMessage(String slackId, UUID receiverId, String messageContent) {
        SlackMessage newSlackMessage = SlackMessage.create(slackId, receiverId, messageContent);
        SlackMessage savedMessage = slackMessageRepository.save(newSlackMessage);
        return SlackMessageResponse.from(savedMessage);
    }

    /**
     * Slack 메시지 재시도 로직 (스케줄러 또는 이벤트 리스너에서 호출)
     * * 💡 REQUIRES_NEW를 사용해 하나의 메시지 재시도가 실패하더라도
     * 다른 대기열 메시지들의 트랜잭션까지 롤백되지 않도록 완전히 격리합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retrySlackMessage(UUID messageId) {
        SlackMessage slackMessage = slackMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(AiSlackErrorCode.SLACK_MESSAGE_NOT_FOUND, "Slack message not found with id: " + messageId));

        // 1. 최대 재시도 횟수(3회)를 초과한 경우 영구 실패 처리
        if (!slackMessage.canRetry()) {
            slackMessage.permanentFailSending();
            slackMessageRepository.save(slackMessage);

            // 무조건 예외를 던지기보다 로그를 남기고 리턴하는 것이 스케줄러(배치) 루프 안정성에 훨씬 좋습니다.
            log.warn("Slack 메시지 발송 영구 실패 확정 (재시도 한도 초과): messageId={}", messageId);
            return;
        }

        // 2. 재시도를 위해 상태를 READY_TO_SEND로 변경하고 카운트 증가 준비
        slackMessage.prepareForRetry(); // CHANGED: Use prepareForRetry() instead of requeueForSending()
        slackMessageRepository.save(slackMessage);

        // 3. 주입받은 전역 slackToken을 활용해 실발송 재시도 오케스트레이션
        try {
            ChatPostMessageResponse response = Slack.getInstance().methods(slackToken).chatPostMessage(req -> req
                    .channel(slackMessage.getSlackId())   // 엔티티의 getter 사용
                    .text(slackMessage.getMessage())      // 🛠️ 수정: messageContent -> message 필드명 일치화
            );

            if (response.isOk()) {
                slackMessage.succeedSending(); // 성공 상태로 전이 (MESSAGE_SENT)
                log.info("Slack 메시지 재발송 성공: messageId={}", messageId);
            } else {
                slackMessage.failSending();    // 실패 상태로 전이 (MESSAGE_FAILED 및 카운트 +1)
                log.warn("Slack 메시지 재발송 실패 (API 에러): messageId={}, error={}", messageId, response.getError());
            }
        } catch (Exception e) {
            slackMessage.failSending();        // 통신 예외 발생 시에도 실패 처리 및 카운트 +1
            log.error("Slack 메시지 재발송 중 예외 발생: messageId={}, error={}", messageId, e.getMessage(), e);
        } finally {
            slackMessageRepository.save(slackMessage); // 최종 상태 반영
        }
    }
}
