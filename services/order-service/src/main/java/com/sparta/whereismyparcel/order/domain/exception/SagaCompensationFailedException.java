package com.sparta.whereismyparcel.order.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class SagaCompensationFailedException extends BusinessException {
    public SagaCompensationFailedException() {
        super(OrderErrorCode.SAGA_COMPENSATION_FAILED);
    }
}
