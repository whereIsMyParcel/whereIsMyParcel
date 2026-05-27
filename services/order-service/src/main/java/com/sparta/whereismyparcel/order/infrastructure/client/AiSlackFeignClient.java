package com.sparta.whereismyparcel.order.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.AiAnalysisRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "ai-slack-service")
public interface AiSlackFeignClient {

    @PostMapping("/internal/v1/ai-slack/analysis-requests")
    ApiResponse<UUID> createAiAnalysisRequest(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody AiAnalysisRequest request
    );
}
