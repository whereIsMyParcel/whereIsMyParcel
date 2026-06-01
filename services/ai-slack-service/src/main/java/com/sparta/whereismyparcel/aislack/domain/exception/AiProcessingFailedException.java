package com.sparta.whereismyparcel.aislack.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class AiProcessingFailedException extends BusinessException {

    public AiProcessingFailedException() {
        super(AiSlackErrorCode.AI_PROCESSING_FAILED);
    }
}
