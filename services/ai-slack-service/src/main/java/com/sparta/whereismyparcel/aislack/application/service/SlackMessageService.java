package com.sparta.whereismyparcel.aislack.application.service;

import com.sparta.whereismyparcel.aislack.domain.entity.SlackMessage;
import com.sparta.whereismyparcel.aislack.domain.repository.SlackMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackMessageService {

    private final SlackMessageRepository slackMessageRepository;

    // Slack 메시지 단건 조회
    public SlackMessage getSlackMessage(UUID messageId) {
        return slackMessageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Slack message not found with id: " + messageId));
    }

    // Slack 메시지 전체 조회 (필요에 따라 페이징/필터링 추가 가능)
    public List<SlackMessage> getAllSlackMessages() {
        return slackMessageRepository.findAll();
    }

    // Slack 메시지 업데이트
    @Transactional
    public SlackMessage updateSlackMessage(UUID messageId, String newMessageContent) {
        SlackMessage slackMessage = getSlackMessage(messageId);
        // SlackMessage 엔티티에 메시지 내용을 업데이트하는 메서드가 필요합니다.
        // 예: slackMessage.updateMessage(newMessageContent);
        // 현재 SlackMessage 엔티티에는 updateMessage 메서드가 없으므로, 추가해야 합니다.
        // 일단은 임시로 필드를 직접 설정하는 것으로 가정합니다.
        // TODO: SlackMessage 엔티티에 updateMessage 메서드 추가 필요
        // slackMessage.setMessage(newMessageContent); // Lombok @Setter를 사용하지 않는다면 엔티티에 메서드 필요
        return slackMessageRepository.save(slackMessage); // 변경 감지 후 저장
    }

    // Slack 메시지 삭제
    @Transactional
    public void deleteSlackMessage(UUID messageId) {
        slackMessageRepository.deleteById(messageId);
    }

    // AI 분석 성공 시 Slack 메시지 생성 (내부 호출용)
    @Transactional
    public SlackMessage createSlackMessage(String slackId, UUID receiverId, String messageContent) {
        SlackMessage newSlackMessage = SlackMessage.create(slackId, receiverId, messageContent);
        return slackMessageRepository.save(newSlackMessage);
    }

    // Slack 메시지 재시도 로직 (예: 스케줄러 또는 이벤트 리스너에서 호출)
    @Transactional
    public void retrySlackMessage(UUID messageId) {
        SlackMessage slackMessage = getSlackMessage(messageId);
        slackMessage.retry(); // SlackMessage 엔티티의 retry 로직 호출
        slackMessageRepository.save(slackMessage);
    }
}
