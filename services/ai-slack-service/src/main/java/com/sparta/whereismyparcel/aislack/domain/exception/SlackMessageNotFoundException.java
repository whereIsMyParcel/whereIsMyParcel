package com.sparta.whereismyparcel.aislack.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class SlackMessageNotFoundException extends BusinessException {

    public SlackMessageNotFoundException() {
        super(AiSlackErrorCode.SLACK_MESSAGE_NOT_FOUND);
    }
}
