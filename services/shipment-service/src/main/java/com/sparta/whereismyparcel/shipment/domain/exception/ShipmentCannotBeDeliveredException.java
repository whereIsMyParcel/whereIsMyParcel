package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class ShipmentCannotBeDeliveredException extends BusinessException {

    public ShipmentCannotBeDeliveredException() {
        super(ShipmentErrorCode.SHIPMENT_CAN_NOT_DELIVERED);
    }
}
