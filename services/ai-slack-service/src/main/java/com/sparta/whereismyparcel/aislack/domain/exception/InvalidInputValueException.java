package com.sparta.whereismyparcel.aislack.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InvalidInputValueException extends BusinessException {

    public InvalidInputValueException() {
        super(AiSlackErrorCode.INVALID_INPUT_VALUE);
    }
}
