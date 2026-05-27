package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class DeliveryManagerNotFoundException extends BusinessException {

    public DeliveryManagerNotFoundException() {
        super(ShipmentErrorCode.DELIVERY_MANAGER_NOT_FOUND);
    }
}
