package com.sparta.whereismyparcel.order.domain.exception;

import com.sparta.whereismyparcel.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-001", "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "ORDER-100", "변경할 수 없는 주문 상태입니다."),
    INVALID_ORDER_ITEMS(HttpStatus.BAD_REQUEST, "ORDER-101", "주문 상품은 최소 1개 이상이어야 합니다."),
    ORDER_CANCEL_TIME_EXPIRED(HttpStatus.BAD_REQUEST, "ORDER-102", "주문 생성 후 5분이 지나 취소할 수 없습니다."),
    SAGA_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER-900", "주문 처리 중 오류가 발생했습니다."),
    SAGA_COMPENSATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER-901", "보상 트랜잭션 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
