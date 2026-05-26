package com.sparta.whereismyparcel.aislack.infrastructure.client;

import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.GeminiRequest; // infrastructure.client.dto 임포트
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.GeminiResponse; // infrastructure.client.dto 임포트

public interface GeminiClient {

    /**
     * Gemini AI API를 호출하여 텍스트를 생성합니다.
     * @param request Gemini AI 요청 DTO
     * @return Gemini AI 응답 DTO
     */
    GeminiResponse generateText(GeminiRequest request);
}
