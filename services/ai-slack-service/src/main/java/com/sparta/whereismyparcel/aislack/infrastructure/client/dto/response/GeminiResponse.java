/*
package com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response;

import java.util.List;
import java.util.Optional;

*/
/*
 * Google Gemini API 응답 DTO
 * Gemini의 표준 응답 스키마인 { "candidates": [ { "content": { "parts": [ { "text": "..." } ] } } ] } 구조를 따릅니다.
 *//*

public record GeminiResponse(
        List<Candidate> candidates
) {
    public record Candidate(
       Content content,
       String finishReason
    ){}

    public record Content(
        List<Part> parts,
        String role
    ){}

    public record Part(
        String text
    ){}

    */
/*
     * Gemini의 복잡한 응답 계층 구조에서
     * '최종 발송 시한 결과 텍스트'만 안전하게 뽑아내는 메서드
     * 계산 실패 시 예외 확인 로직 필요 - 서비스 레이어에서
     *//*

    public Optional<String> getResponseText(){
        if(candidates != null && !candidates.isEmpty()){
            Candidate firstCandidate = candidates.get(0);
            if(firstCandidate.content() != null &&
                firstCandidate.content().parts() != null &&
                !firstCandidate.content().parts().isEmpty()){

                return Optional.ofNullable(firstCandidate.content().parts().get(0).text());
            }
        }
        return Optional.empty();
    }
}
*/
