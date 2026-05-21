package com.sparta.whereismyparcel.hub.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InvalidPageSizeException extends BusinessException {
    public InvalidPageSizeException() {
        super(HubErrorCode.INVALID_PAGE_SIZE);
    }
}
