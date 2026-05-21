package com.sparta.whereismyparcel.hub.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class HubRouteNotFoundException extends BusinessException {
    public HubRouteNotFoundException() {
        super(HubErrorCode.HUB_ROUTE_NOT_FOUND);
    }
}
