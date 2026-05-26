package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class ShipmentNotFoundException extends BusinessException {

    public ShipmentNotFoundException() {
        super(ShipmentErrorCode.SHIPMENT_NOT_FOUND);
    }
}
