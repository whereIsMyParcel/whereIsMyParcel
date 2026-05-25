package com.sparta.whereismyparcel.inventory.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class NotEnoughAvailableStockException extends BusinessException {
    public NotEnoughAvailableStockException() {
        super(InventoryErrorCode.NOT_ENOUGH_AVAILABLE_STOCK);
    }
}
