package com.sparta.whereismyparcel.aislack.application.service;

import com.sparta.whereismyparcel.aislack.domain.entity.AiMessage;
import com.sparta.whereismyparcel.aislack.domain.entity.AnalysisStatus;
import com.sparta.whereismyparcel.aislack.domain.exception.AiProcessingFailedException;
import com.sparta.whereismyparcel.aislack.domain.exception.AiSlackErrorCode;
import com.sparta.whereismyparcel.aislack.domain.exception.InvalidAiRequestDataException;
import com.sparta.whereismyparcel.aislack.domain.repository.AiMessageRepository;
import com.sparta.whereismyparcel.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageTransactionService {

    private final AiMessageRepository aiMessageRepository;

    /**
     * AiMessage를 조회하고, 필요한 경우 재시도를 위해 상태를 REQUESTED로 변경합니다.
     * 이 메서드는 새로운 트랜잭션에서 실행됩니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiMessage getAndPrepareAiMessageForAnalysis(UUID aiMessageId) {
        AiMessage aiMessage = aiMessageRepository.findById(aiMessageId)
                .orElseThrow(() -> new InvalidAiRequestDataException());

        // 이미 처리되었거나 영구 실패한 메시지는 다시 분석하지 않음 (멱등성)
        if (aiMessage.getAnalysisStatus() == AnalysisStatus.AI_SUCCESS) {
            log.info("AiMessage {}는 이미 AI_SUCCESS 상태입니다. 분석 하지않습니다..", aiMessageId);
            throw new AiProcessingFailedException();
        }

        // AI 분석 재시도 시 상태를 REQUESTED로 변경
        if (aiMessage.getAnalysisStatus() == AnalysisStatus.AI_FAIL) {
            aiMessage.requeueForAnalysis(); // 상태를 REQUESTED로 변경하고 응답 필드 초기화
            aiMessageRepository.save(aiMessage);
            log.info("AiMessage {} 재분석을 위해 REQUESTED 상태로 변경되었습니다.", aiMessageId);
        }
        return aiMessage;
    }

    /**
     * AI 분석 성공 시 AiMessage의 상태를 업데이트합니다.
     * 이 메서드는 새로운 트랜잭션에서 실행됩니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAiMessageStatusOnSuccess(UUID aiMessageId, String responseContent, LocalDateTime finalDispatchDeadline) {
        AiMessage aiMessage = aiMessageRepository.findById(aiMessageId)
                .orElseThrow(() -> new InvalidAiRequestDataException());
        aiMessage.succeedAnalysis(responseContent, finalDispatchDeadline);
        aiMessageRepository.save(aiMessage);
    }

    /**
     * AI 분석 실패 시 AiMessage의 상태를 업데이트합니다.
     * 이 메서드는 새로운 트랜잭션에서 실행됩니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAiMessageStatusOnFailure(UUID aiMessageId) {
        AiMessage aiMessage = aiMessageRepository.findById(aiMessageId)
                .orElseThrow(() -> new InvalidAiRequestDataException());
        aiMessage.failAnalysis();
        aiMessageRepository.save(aiMessage);
    }

}
