package com.sparta.whereismyparcel.user.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class SlackIdAlreadyExistsException extends BusinessException {
    public SlackIdAlreadyExistsException() {
        super(UserErrorCode.SLACK_ID_ALREADY_EXISTS);
    }
}
