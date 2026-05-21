package com.sparta.whereismyparcel.hub.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class HubNotFoundException extends BusinessException {
    public HubNotFoundException() {
        super(HubErrorCode.HUB_NOT_FOUND);
    }
}
