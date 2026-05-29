package com.sparta.whereismyparcel.aislack.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class SlackMessageSendFailedException extends BusinessException {

    public SlackMessageSendFailedException() {
        super(AiSlackErrorCode.SLACK_MESSAGE_SEND_FAILED);
    }
}
