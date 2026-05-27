package com.sparta.whereismyparcel.aislack.application.service;

import com.sparta.whereismyparcel.aislack.domain.entity.AnalysisStatus;
import com.sparta.whereismyparcel.aislack.domain.entity.SlackMessage;
import com.sparta.whereismyparcel.aislack.domain.entity.SlackStatus;
import com.sparta.whereismyparcel.aislack.domain.exception.AiSlackErrorCode;
import com.sparta.whereismyparcel.aislack.domain.repository.SlackMessageRepository;
import com.sparta.whereismyparcel.aislack.presentation.dto.request.SlackRequest;
import com.sparta.whereismyparcel.aislack.presentation.dto.response.SlackMessageResponse;
import com.sparta.whereismyparcel.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackMessageService {

    private final SlackMessageRepository slackMessageRepository;

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
     * AI 분석 성공 시 Slack 메시지 생성 (내부 호출용)
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

    // Slack 메시지 재시도 로직 (예: 스케줄러 또는 이벤트 리스너에서 호출)
    @Transactional
    public void retrySlackMessage(UUID messageId) {
        SlackMessage slackMessage = slackMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(AiSlackErrorCode.SLACK_MESSAGE_NOT_FOUND, "Slack message not found with id: " + messageId));

        if (!slackMessage.canRetry()) {
            slackMessage.permanentFailSending();
            slackMessageRepository.save(slackMessage);
            throw new BusinessException(AiSlackErrorCode.SLACK_MESSAGE_PERMANENT_FAILED, "Slack message permanent failed after multiple retries: " + messageId);
        }
        slackMessage.requeueForSending();
        slackMessageRepository.save(slackMessage);
    }
}
