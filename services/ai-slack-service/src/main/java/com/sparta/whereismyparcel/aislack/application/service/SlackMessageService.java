package com.sparta.whereismyparcel.aislack.application.service;

import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.sparta.whereismyparcel.aislack.domain.entity.AnalysisStatus;
import com.sparta.whereismyparcel.aislack.domain.entity.SlackMessage;
import com.sparta.whereismyparcel.aislack.domain.entity.SlackStatus;
import com.sparta.whereismyparcel.aislack.domain.exception.AiSlackErrorCode;
import com.sparta.whereismyparcel.aislack.domain.exception.InvalidInputValueException;
import com.sparta.whereismyparcel.aislack.domain.exception.SlackMessageNotFoundException;
import com.sparta.whereismyparcel.aislack.domain.exception.SlackMessagePermanentFailedException;
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
                .orElseThrow(() -> new SlackMessageNotFoundException());
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
                throw new InvalidInputValueException();
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
                .orElseThrow(() -> new SlackMessageNotFoundException());

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
            throw  new SlackMessageNotFoundException();
        }
        slackMessageRepository.deleteById(messageId);
    }

    /**
     * AI 분석 성공 시 Slack 메시지 생성 및 발송을 시도합니다.
     * 이 메서드는 트랜잭션 오케스트레이션 역할을 하며, 실제 DB 작업은 별도의 트랜잭션 메서드에서 처리합니다.
     * @param aiMessageId 관련 AI 메시지 ID
     * @param orderResponse 주문 정보
     * @param shipmentResponses 배송 정보 목록
     * @param recipientUser 수령인 사용자 정보
     * @param finalDispatchDeadline 최종 발송 시한
     * @return 생성된 Slack 메시지 정보 DTO
     */
    public SlackMessageResponse sendSlackNotification( // @Transactional 제거
                                                       UUID aiMessageId,
                                                       OrderResponse orderResponse,
                                                       List<ShipmentResponse> shipmentResponses,
                                                       UserResponse recipientUser,
                                                       LocalDateTime finalDispatchDeadline
    ) {
        String formattedDispatchDeadline = finalDispatchDeadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        SlackRequest slackRequest = SlackRequest.of(orderResponse, shipmentResponses, recipientUser, formattedDispatchDeadline);

        SlackMessage savedMessage = null;
        boolean apiSuccess = false;
        String apiError = null;

        try {
            // 1. Slack 메시지 엔티티 저장 (새로운 트랜잭션)
            savedMessage = saveNewSlackMessage(slackRequest.slackId(), aiMessageId, slackRequest.message());

            // 2. Slack API 호출 (트랜잭션 외부)
            ChatPostMessageResponse response = Slack.getInstance().methods(slackToken).chatPostMessage(req -> req
                    .channel(slackRequest.slackId())
                    .text(slackRequest.message())
            );

            apiSuccess = response.isOk();
            if (!apiSuccess) {
                apiError = response.getError();
            }

        } catch (Exception e) {
            log.error("Slack 알림 발송 중 예외 발생: aiMessageId={}, error={}", aiMessageId, e.getMessage(), e);
            apiSuccess = false;
            apiError = e.getMessage();
        } finally {
            // 3. Slack API 호출 결과에 따라 메시지 상태 업데이트 (새로운 트랜잭션)
            if (savedMessage != null) {
                updateSlackMessageStatusAfterSend(savedMessage.getId(), apiSuccess, apiError);
            }
        }

        // savedMessage가 null이 아닐 때만 from 메서드 호출
        return savedMessage != null ? SlackMessageResponse.from(savedMessage) : null; // 또는 예외 처리
    }

    /**
     * 새로운 Slack 메시지 엔티티를 생성하고 저장합니다.
     * 이 메서드는 새로운 트랜잭션에서 실행됩니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected SlackMessage saveNewSlackMessage(String slackId, UUID receiverId, String messageContent) {
        SlackMessage newSlackMessage = SlackMessage.create(slackId, receiverId, messageContent);

        newSlackMessage.markAsRequested(); // Initial status
        return slackMessageRepository.save(newSlackMessage);
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
     * 이 메서드는 트랜잭션 오케스트레이션 역할을 하며, 실제 DB 작업은 별도의 트랜잭션 메서드에서 처리합니다.
     */
    public void retrySlackMessage(UUID messageId) { // @Transactional 제거
        SlackMessage slackMessage = null;
        boolean apiSuccess = false;
        String apiError = null;

        try {
            // 1. 메시지를 재시도 상태로 준비 (새로운 트랜잭션)
            slackMessage = prepareSlackMessageForRetry(messageId);

            // 2. Slack API 호출 (트랜잭션 외부)
            SlackMessage finalSlackMessage = slackMessage;
            ChatPostMessageResponse response = Slack.getInstance().methods(slackToken).chatPostMessage(req -> req
                    .channel(finalSlackMessage.getSlackId())
                    .text(finalSlackMessage.getMessage())
            );

            apiSuccess = response.isOk();
            if (!apiSuccess) {
                apiError = response.getError();
            }

        } catch (BusinessException e) {
            log.error("Slack 메시지 재시도 준비 중 오류: messageId={}, error={}", messageId, e.getMessage());
            // prepareSlackMessageForRetry에서 이미 상태가 업데이트되었거나 예외가 던져졌을 것임
            return; // 추가 처리 없이 종료
        } catch (Exception e) {
            log.error("Slack API 호출 중 예외 발생: messageId={}, error={}", messageId, e.getMessage(), e);
            apiSuccess = false;
            apiError = e.getMessage();
        } finally {
            // 3. Slack API 호출 결과에 따라 메시지 상태 업데이트 (새로운 트랜잭션)
            if (slackMessage != null) {
                updateSlackMessageStatusAfterSend(messageId, apiSuccess, apiError);
            }
        }
    }

    /**
     * Slack 메시지를 재시도 상태로 준비하고, 재시도 횟수 초과 시 영구 실패 처리합니다.
     * 이 메서드는 새로운 트랜잭션에서 실행됩니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected SlackMessage prepareSlackMessageForRetry(UUID messageId) {
        SlackMessage slackMessage = slackMessageRepository.findById(messageId)
                .orElseThrow(() ->  new SlackMessageNotFoundException());

        // 1. 최대 재시도 횟수(3회)를 초과한 경우 영구 실패 처리
        if (!slackMessage.canRetry()) {
            slackMessage.permanentFailSending();
            slackMessageRepository.save(slackMessage);
            log.warn("Slack 메시지 발송 영구 실패 확정 (재시도 한도 초과): messageId={}", messageId);
            throw new SlackMessagePermanentFailedException();
        }

        // 2. 재시도를 위해 상태를 READY_TO_SEND로 변경 (retryCount는 초기화하지 않음)
        slackMessage.prepareForRetry();
        slackMessageRepository.save(slackMessage);
        log.info("Slack 메시지 재시도 준비 완료: messageId={}", messageId);
        return slackMessage;
    }

    /**
     * Slack API 호출 결과에 따라 메시지 상태를 업데이트합니다.
     * 이 메서드는 새로운 트랜잭션에서 실행됩니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateSlackMessageStatusAfterSend(UUID messageId, boolean success, String errorDetails) {
        SlackMessage slackMessage = slackMessageRepository.findById(messageId)
                .orElseThrow(() ->  new SlackMessageNotFoundException());

        if (success) {
            slackMessage.succeedSending();
            log.info("Slack 메시지 재발송 성공: messageId={}", messageId);
        } else {
            slackMessage.failSending();
            log.warn("Slack 메시지 재발송 실패 (API 에러): messageId={}, error={}", messageId, errorDetails);
        }
        slackMessageRepository.save(slackMessage);
    }
}
