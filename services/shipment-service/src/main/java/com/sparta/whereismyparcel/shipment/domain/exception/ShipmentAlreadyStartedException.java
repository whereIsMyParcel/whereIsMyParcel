package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class ShipmentAlreadyStartedException extends BusinessException {

	public ShipmentAlreadyStartedException() {
		super(ShipmentErrorCode.SHIPMENT_ALREADY_STARTED);
	}
}
