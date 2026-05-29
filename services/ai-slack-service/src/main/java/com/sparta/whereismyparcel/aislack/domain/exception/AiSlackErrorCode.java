package com.sparta.whereismyparcel.aislack.domain.exception;

import com.sparta.whereismyparcel.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiSlackErrorCode implements ErrorCode {

    INVALID_AI_REQUEST_DATA(HttpStatus.BAD_REQUEST, "AISLACK-001", "AI 분석 요청 데이터가 유효하지 않습니다."),
    AI_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AISLACK-002", "Gemini AI 처리 중 오류가 발생했습니다."),
    AI_RESPONSE_PARSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AISLACK-003", "Gemini AI 응답 파싱에 실패했습니다."),
    SLACK_ID_NOT_FOUND(HttpStatus.NOT_FOUND, "AISLACK-004", "배송 담당자의 Slack ID를 찾을 수 없습니다."),
    SLACK_MESSAGE_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AISLACK-005", "Slack 메시지 전송에 실패했습니다."),
    SLACK_MESSAGE_PERMANENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AISLACK-006", "Slack 메시지 전송이 영구적으로 실패했습니다."),
    SLACK_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "AISLACK-007", "Slack 메시지를 찾을 수 없습니다."), // 수정됨
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "AISLACK-008", "유효하지 않은 입력 값입니다."),
    ORDER_SERVICE_COMMUNICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"AISLACK-009" , "Order 서비스에 delivery_deadline 수정 실패했습니다." ), // 수정됨
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "AISLACK-010", "주문 정보를 찾을 수 없습니다."), // 추가됨
    ORDER_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AISLACK-011", "주문 정보를 가져오는 데 실패했습니다."); // 추가됨

    private final HttpStatus status;
    private final String code;
    private final String message;
}
