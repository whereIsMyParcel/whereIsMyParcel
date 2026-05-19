package com.sparta.whereismyparcel.order.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InvalidOrderItemsException extends BusinessException {

    public InvalidOrderItemsException() {
        super(OrderErrorCode.INVALID_ORDER_ITEMS);
    }
}
