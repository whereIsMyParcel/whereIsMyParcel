package com.sparta.whereismyparcel.aislack.infrastructure.client.dto;

import java.util.List;

/**
 * Gemini API와 직접적인 통신
 * @param contents
 * @param generationConfig
 */
public record GeminiRequest(
        List<Content> contents,
        GenerationConfig generationConfig
) {
    public record Content(
            List<Part> parts
    ) {}

    public record Part(
            String text
    ) {}

    public record GenerationConfig(
            double temperature,
            int topK,
            int topP,
            int maxOutputTokens,
            List<String> stopSequences
    ) {}
}
