package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class ShipmentRouteNotFoundException extends BusinessException {

    public ShipmentRouteNotFoundException() {
        super(ShipmentErrorCode.SHIPMENT_ROUTE_NOT_FOUND);
    }
}
