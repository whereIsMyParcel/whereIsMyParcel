package com.sparta.whereismyparcel.aislack.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class SlackMessagePermanentFailedException extends BusinessException {

    public SlackMessagePermanentFailedException() {
        super(AiSlackErrorCode.SLACK_MESSAGE_PERMANENT_FAILED);
    }
}
