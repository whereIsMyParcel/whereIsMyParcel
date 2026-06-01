package com.sparta.whereismyparcel.aislack.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class OrderServiceCommunicationFailedException extends BusinessException {

    public OrderServiceCommunicationFailedException() {
        super(AiSlackErrorCode.ORDER_SERVICE_COMMUNICATION_FAILED);
    }
}
