package com.sparta.whereismyparcel.aislack.presentation.controller;

import com.sparta.whereismyparcel.aislack.application.service.AiMessageService;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request.DeliveryDeadlinePatchRequest;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request.OrderInternalRequest;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/ai-slack")
public class AiSlackInternalController {

    private final AiMessageService aiMessageService;

    /**
     * AI 분석 요청을 생성합니다.
     * 다른 서비스에서 주문/배송 정보 생성 시 호출하여 AI 분석을 트리거합니다.
     * @param request AI 분석 요청에 필요한 정보 (orderId 등 OrderInternalRequest의 필드)
     * @param callerUserId 요청을 시작한 사용자 ID (인증/권한 부여용)
     * @return 생성된 AiMessage의 ID
     */
    @PostMapping("/analysis-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UUID> createAiAnalysisRequest(
            @RequestHeader("X-User-Id") String callerUserId,
            @Valid @RequestBody OrderInternalRequest request
    ) {
        // AiMessageService.createAiAnalysisRequest 메서드의 시그니처에 맞춰 request 객체 전체를 전달
        UUID aiMessageId = aiMessageService.createAiAnalysisRequest(request, callerUserId);
        return ApiResponse.created(aiMessageId);
    }
}
