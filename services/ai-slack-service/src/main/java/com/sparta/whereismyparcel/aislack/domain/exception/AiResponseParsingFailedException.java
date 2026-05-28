package com.sparta.whereismyparcel.aislack.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class AiResponseParsingFailedException extends BusinessException {

    public AiResponseParsingFailedException() {
        super(AiSlackErrorCode.AI_RESPONSE_PARSING_FAILED);
    }
}
