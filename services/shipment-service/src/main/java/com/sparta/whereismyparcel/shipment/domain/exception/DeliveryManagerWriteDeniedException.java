package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class DeliveryManagerWriteDeniedException extends BusinessException {

    public DeliveryManagerWriteDeniedException() {
        super(ShipmentErrorCode.DELIVERY_MANAGER_WRITE_DENIED);
    }
}