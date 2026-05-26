package com.sparta.whereismyparcel.aislack.infrastructure.client.dto;

import java.util.List;

/**
 * Gemini API와 직접적인 통신
 * @param candidates
 * @param promptFeedback
 */
public record GeminiResponse(
        List<Candidate> candidates,
        PromptFeedback promptFeedback
) {
    public record Candidate(
            Content content,
            String finishReason,
            int index,
            List<SafetyRating> safetyRatings
    ) {}

    public record Content(
            List<Part> parts,
            String role
    ) {}

    public record Part(
            String text
    ) {}

    public record SafetyRating(
            String category,
            String probability
    ) {}

    public record PromptFeedback(
            List<SafetyRating> safetyRatings
    ) {}
}
