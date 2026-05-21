package com.sparta.whereismyparcel.aislack.presentation.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/*
 * Google Gemini API 요청 DTO
 * Gemini의 표준 입력 스키마인 { "contents": [ { "parts": [ { "text": "..." } ] } ] } 구조를 따릅니다.
 */
public record GeminiRequest(
    @NotEmpty(message = "콘텐츠 바디는 비어있을 수 없습니다.")
    List<Content> contents;
) {
    public record Content(
            List<Part> parts
    )
    public record Part(
            String text
    ){}

    /*
     * OrderInternalRequest 데이터를 기반으로 Gemini 전용 프롬프트 조립하여
     * GeminiRequest 객체를 생성하는 팩토리 메서드
     */
    public static GeminiRequest create(OrderInternalRequest orderInternalRequest) {

}
