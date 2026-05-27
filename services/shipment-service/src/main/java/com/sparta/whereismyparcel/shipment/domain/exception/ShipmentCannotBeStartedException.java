package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class ShipmentCannotBeStartedException extends BusinessException {

    public ShipmentCannotBeStartedException() {
        super(ShipmentErrorCode.SHIPMENT_CAN_NOT_STARTED);
    }
}
