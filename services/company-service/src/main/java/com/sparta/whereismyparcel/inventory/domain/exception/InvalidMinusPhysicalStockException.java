package com.sparta.whereismyparcel.inventory.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InvalidMinusPhysicalStockException extends BusinessException {
    public InvalidMinusPhysicalStockException() {
        super(InventoryErrorCode.INVALID_MINUS_PHYSICAL_STOCK);
    }
}
