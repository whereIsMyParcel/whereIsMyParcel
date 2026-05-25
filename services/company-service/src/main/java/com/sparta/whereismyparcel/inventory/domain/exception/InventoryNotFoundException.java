package com.sparta.whereismyparcel.inventory.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InventoryNotFoundException extends BusinessException {
    public InventoryNotFoundException() {
        super(InventoryErrorCode.INVENTORY_NOT_FOUND);
    }
}
