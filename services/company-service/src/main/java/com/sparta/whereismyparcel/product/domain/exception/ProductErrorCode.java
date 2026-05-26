package com.sparta.whereismyparcel.product.domain.exception;

import com.sparta.whereismyparcel.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-001", "해당 상품을 찾을 수 없습니다."),

    PRODUCT_OPTION_VALUE_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-002", "해당 옵션값을 찾을 수 없습니다."),

    UNSUPPORTED_PRODUCT_STATUS(HttpStatus.BAD_REQUEST, "PRODUCT-003", "지원하지 않는 상품 상태 변경 요청입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
