package com.sparta.whereismyparcel.inventory.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class ProductVariantNotFoundException extends BusinessException {
    public ProductVariantNotFoundException() {
        super(InventoryErrorCode.PRODUCT_VARIANT_NOT_FOUND);
    }
}
