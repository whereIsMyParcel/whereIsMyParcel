package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class SlackIdNotFoundException extends BusinessException {

    public SlackIdNotFoundException() {
        super(ShipmentErrorCode.SLACK_ID_NOT_FOUND);
    }
}
