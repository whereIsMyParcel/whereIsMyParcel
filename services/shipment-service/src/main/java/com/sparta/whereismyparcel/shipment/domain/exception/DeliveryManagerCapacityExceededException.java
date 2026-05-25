package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class DeliveryManagerCapacityExceededException extends BusinessException {

	public DeliveryManagerCapacityExceededException() {
		super(ShipmentErrorCode.DELIVERY_MANAGER_CAPACITY_EXCEEDED);
	}
}
