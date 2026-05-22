package com.sparta.whereismyparcel.order.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class OrderCancelTimeExpiredException extends BusinessException {
    public OrderCancelTimeExpiredException() {
        super(OrderErrorCode.ORDER_CANCEL_TIME_EXPIRED);
    }
}
