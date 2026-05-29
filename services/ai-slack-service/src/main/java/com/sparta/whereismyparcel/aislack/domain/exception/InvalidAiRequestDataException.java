package com.sparta.whereismyparcel.aislack.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InvalidAiRequestDataException extends BusinessException {

    public InvalidAiRequestDataException() {
        super(AiSlackErrorCode.INVALID_AI_REQUEST_DATA);
    }
}
