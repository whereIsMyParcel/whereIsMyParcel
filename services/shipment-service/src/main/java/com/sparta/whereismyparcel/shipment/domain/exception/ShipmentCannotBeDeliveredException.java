package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class ShipmentCannotBeDeliveredException extends BusinessException {

    public ShipmentCannotBeDeliveredException() {
        super(ShipmentErrorCode.HUB_NOT_FOUND);
    }
}
