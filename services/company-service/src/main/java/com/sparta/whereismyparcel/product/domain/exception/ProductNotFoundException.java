package com.sparta.whereismyparcel.product.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException() {
        super(ProductErrorCode.PRODUCT_NOT_FOUND);
    }
}
