package com.sparta.whereismyparcel.aislack.presentation.controller;

import com.sparta.whereismyparcel.aislack.application.service.SlackMessageService;
import com.sparta.whereismyparcel.aislack.domain.entity.AnalysisStatus;
import com.sparta.whereismyparcel.aislack.presentation.dto.request.SlackRequest;
import com.sparta.whereismyparcel.aislack.presentation.dto.response.SlackMessageResponse;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "ai-slack", description = "AI & Slack 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai-slack")
/**
 * 관리자가 슬랙 메시지 CRUD를 수행하는 컨트롤러
 */
public class AiSlackController {

    private final SlackMessageService slackMessageService;

    @Operation(
            summary = "슬랙 메시지 생성",
            description = "AI 분석 결과를 배달자한테 슬랙 메시지 전송"
    )
    @GetMapping("/{aiMessageId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SlackMessageResponse> getAiMessage(@PathVariable UUID aiMessageId) {
        SlackMessageResponse response = slackMessageService.getSlackMessage(aiMessageId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "목록 조회",
            description = "AI 슬랙 메시지 목록 조회 (페이징 및 필터링 가능)"
    )
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

    @Operation(
            summary = "슬랙 메시지 수정",
            description = "슬랙 메시지 내용 수정"
    )
    @PutMapping("/{aiMessageId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SlackMessageResponse> updateAiMessage(
            @PathVariable UUID aiMessageId,
            @Valid @RequestBody SlackRequest request
    ) {
        SlackMessageResponse response = slackMessageService.updateAiMessage(aiMessageId, request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "슬랙 메시지 삭제",
            description = "슬랙 메시지 삭제"
    )
    @DeleteMapping("/{aiMessageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteAiMessage(@PathVariable UUID aiMessageId) {
        slackMessageService.deleteAiMessage(aiMessageId);
        return ApiResponse.ok();
    }
}
