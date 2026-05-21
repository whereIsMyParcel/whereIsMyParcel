package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;
import com.sparta.whereismyparcel.common.exception.CommonErrorCode;

public class HubNotFoundException extends BusinessException {

	public HubNotFoundException() {
		super(ShipmentErrorCode.HUB_NOT_FOUND);
	}
}
