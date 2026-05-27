package com.sparta.whereismyparcel.inventory.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class HubNotFoundException extends BusinessException {
    public HubNotFoundException() {
        super(InventoryErrorCode.HUB_NOT_FOUND);
    }
}
