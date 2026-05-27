package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class DeliveryManagerReadDeniedException extends BusinessException {

    public DeliveryManagerReadDeniedException() {
        super(ShipmentErrorCode.DELIVERY_MANAGER_READ_DENIED);
    }
}