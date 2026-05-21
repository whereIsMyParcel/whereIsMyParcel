package com.sparta.whereismyparcel.hub.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class NoPathBetweenHubsException extends BusinessException {
    public NoPathBetweenHubsException() {
        super(HubErrorCode.NO_PATH_BETWEEN_HUBS);
    }
}
