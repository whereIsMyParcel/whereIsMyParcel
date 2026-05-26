package com.sparta.whereismyparcel.product.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class ProductOptionValueNotFoundException extends BusinessException {
    public ProductOptionValueNotFoundException() {
        super(ProductErrorCode.PRODUCT_OPTION_VALUE_NOT_FOUND);
    }
}
