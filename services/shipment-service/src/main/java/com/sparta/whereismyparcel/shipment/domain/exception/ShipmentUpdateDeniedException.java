package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class ShipmentUpdateDeniedException extends BusinessException {

    public ShipmentUpdateDeniedException() {
        super(ShipmentErrorCode.SHIPMENT_UPDATE_DENIED);
    }
}