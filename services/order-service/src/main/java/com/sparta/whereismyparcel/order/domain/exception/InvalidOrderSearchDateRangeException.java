package com.sparta.whereismyparcel.order.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InvalidOrderSearchDateRangeException extends BusinessException {

    public InvalidOrderSearchDateRangeException() {
        super(OrderErrorCode.INVALID_ORDER_SEARCH_DATE_RANGE);
    }
}
