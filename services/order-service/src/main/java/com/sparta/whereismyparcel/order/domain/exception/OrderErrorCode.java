package com.sparta.whereismyparcel.order.domain.exception;

import com.sparta.whereismyparcel.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "ORDER-100", "변경할 수 없는 주문 상태입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
