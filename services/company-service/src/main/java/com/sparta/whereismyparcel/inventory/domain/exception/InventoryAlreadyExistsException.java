package com.sparta.whereismyparcel.inventory.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InventoryAlreadyExistsException extends BusinessException {
    public InventoryAlreadyExistsException() {
        super(InventoryErrorCode.INVENTORY_ALREADY_EXISTS);
    }
}
