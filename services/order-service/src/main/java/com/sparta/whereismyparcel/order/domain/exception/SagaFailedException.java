package com.sparta.whereismyparcel.order.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class SagaFailedException extends BusinessException {
    public SagaFailedException() {
        super(OrderErrorCode.SAGA_FAILED);
    }
}
