package com.sparta.whereismyparcel.aislack.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class SlackIdNotFoundException extends BusinessException {

    public SlackIdNotFoundException() {
        super(AiSlackErrorCode.SLACK_ID_NOT_FOUND);
    }
}
