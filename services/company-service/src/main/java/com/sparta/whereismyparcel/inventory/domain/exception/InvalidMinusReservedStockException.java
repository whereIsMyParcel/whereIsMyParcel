package com.sparta.whereismyparcel.inventory.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InvalidMinusReservedStockException extends BusinessException {
    public InvalidMinusReservedStockException() {
        super(InventoryErrorCode.INVALID_MINUS_RESERVED_STOCK);
    }
}
