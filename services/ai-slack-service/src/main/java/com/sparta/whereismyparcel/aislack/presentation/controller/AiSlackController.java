package com.sparta.whereismyparcel.aislack.presentation.controller;

import com.sparta.whereismyparcel.aislack.application.service.SlackMessageService;
import com.sparta.whereismyparcel.aislack.domain.entity.AnalysisStatus;
import com.sparta.whereismyparcel.aislack.presentation.dto.request.SlackRequest;
import com.sparta.whereismyparcel.aislack.presentation.dto.response.SlackMessageResponse;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai-slack")
/**
 * 관리자가 슬랙 메시지 CRUD를 수행하는 컨트롤러
 */
public class AiSlackController {

    private final SlackMessageService slackMessageService;

    @GetMapping("/{aiMessageId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SlackMessageResponse> getAiMessage(@PathVariable UUID aiMessageId) {
        SlackMessageResponse response = slackMessageService.getSlackMessage(aiMessageId);
        return ApiResponse.success(response);
    }

    /**
     * AI 슬랙 메시지 목록 조회 (페이징 및 필터링 가능)
     * @param orderId (선택 사항) 특정 주문 ID에 해당하는 메시지 필터링
     * @param status (선택 사항) 특정 분석 상태에 해당하는 메시지 필터링
     * @param pageable 페이징 정보 (page, size, sort)
     * @return AI 메시지 목록 (Page 객체)
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Page<SlackMessageResponse>> getAiMessages(
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) AnalysisStatus status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<SlackMessageResponse> responses = slackMessageService.getAiMessages(orderId, status, pageable);
        return ApiResponse.success(responses);
    }

    @PutMapping("/{aiMessageId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SlackMessageResponse> updateAiMessage(
            @PathVariable UUID aiMessageId,
            @Valid @RequestBody SlackRequest request
    ) {
        SlackMessageResponse response = slackMessageService.updateAiMessage(aiMessageId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{aiMessageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteAiMessage(@PathVariable UUID aiMessageId) {
        slackMessageService.deleteAiMessage(aiMessageId);
        return ApiResponse.ok();
    }
}
