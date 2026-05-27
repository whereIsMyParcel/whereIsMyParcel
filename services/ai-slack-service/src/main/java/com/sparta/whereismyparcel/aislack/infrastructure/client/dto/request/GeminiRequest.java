/*
package com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

*/
/*
 * Google Gemini API 요청 DTO
 * Gemini의 표준 입력 스키마인 { "contents": [ { "parts": [ { "text": "..." } ] } ] } 구조를 따릅니다.
 *//*

public record GeminiRequest(
        @NotEmpty(message = "콘텐츠 바디는 비어있을 수 없습니다.")
        List<Content> contents,
        GenerationConfig config
) {
    public record Content(
            List<Part> parts
    ){}

    public record Part(
            String text
    ){}

    // 여기에 GenerationConfig 정의
    public record GenerationConfig(
            Double temperature,
            Integer topK,
            Integer topP,
            Integer maxOutputTokens,
            List<String> stopSequences
    ) {}

}*/
