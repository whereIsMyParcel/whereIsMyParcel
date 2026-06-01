package com.sparta.whereismyparcel.aislack.presentation.controller;

import com.sparta.whereismyparcel.aislack.application.service.AiMessageService;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request.OrderInternalRequest;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "ai-slack-internal", description = "AI & Slack 내부 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/ai-slack")
public class AiSlackInternalController {

    private final AiMessageService aiMessageService;

    @Operation(
            summary = "AI 분석 요청을 생성",
            description = "주문/배송 정보 생성 시 호출하여 AI 분석을 트리거"
    )
    @PostMapping("/analysis-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UUID> createAiAnalysisRequest(
            @RequestHeader("X-User-Id") String callerUserId,
            @Valid @RequestBody OrderInternalRequest request
    ) {
        // AiMessageService.createAiAnalysisRequest 메서드의 시그니처에 맞춰 request 객체 전체를 전달
        UUID aiMessageId = aiMessageService.createAiAnalysisRequest(request, callerUserId);
        aiMessageService.analyzeAiMessage(aiMessageId); // ADDED: AI 분석 시작
        return ApiResponse.created(aiMessageId);
    }
}
