package com.sparta.whereismyparcel.order.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InvalidOrderStatusException extends BusinessException {

    public InvalidOrderStatusException() {
        super(OrderErrorCode.INVALID_ORDER_STATUS);
    }
}
