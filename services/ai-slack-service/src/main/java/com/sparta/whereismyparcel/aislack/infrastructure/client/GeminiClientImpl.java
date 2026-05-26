package com.sparta.whereismyparcel.aislack.infrastructure.client;

import com.sparta.whereismyparcel.aislack.domain.exception.AiSlackErrorCode;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request.GeminiRequest; // infrastructure.client.dto 임포트
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.GeminiResponse; // infrastructure.client.dto 임포트
import com.sparta.whereismyparcel.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class GeminiClientImpl implements GeminiClient {

    private final RestTemplate restTemplate;
    private final String geminiApiUrl;
    private final String geminiApiKey;

    public GeminiClientImpl(
            RestTemplate restTemplate,
            @Value("${gemini.api.url}") String geminiApiUrl,
            @Value("${gemini.api.key}") String geminiApiKey
    ) {
        this.restTemplate = restTemplate;
        this.geminiApiUrl = geminiApiUrl;
        this.geminiApiKey = geminiApiKey;
    }

    @Override
    public GeminiResponse generateText(GeminiRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey); // Gemini API Key 헤더 설정

        HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

        try {
            String fullUrl = geminiApiUrl + "?key=" + geminiApiKey; // URL에 API Key 포함 (Gemini API 문서 참조)
            GeminiResponse response = restTemplate.postForObject(fullUrl, entity, GeminiResponse.class);
            if (response == null || response.candidates().isEmpty() || response.candidates().get(0).content().parts().isEmpty()) {
                log.warn("Gemini AI 응답이 비어있거나 유효하지 않습니다.");
                throw new BusinessException(AiSlackErrorCode.AI_RESPONSE_PARSING_FAILED);
            }
            return response;
        } catch (Exception e) {
            log.error("Gemini AI API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(AiSlackErrorCode.AI_PROCESSING_FAILED, e.getMessage());
        }
    }
}
