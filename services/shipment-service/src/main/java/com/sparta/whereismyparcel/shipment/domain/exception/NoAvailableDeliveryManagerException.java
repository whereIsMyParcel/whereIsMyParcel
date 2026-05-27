package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class NoAvailableDeliveryManagerException extends BusinessException {

    public NoAvailableDeliveryManagerException() {
        super(ShipmentErrorCode.NO_AVAILABLE_DELIVERY_MANAGER);
    }
}
