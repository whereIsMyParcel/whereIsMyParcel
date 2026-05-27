package com.sparta.whereismyparcel.product.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class UnsupportedProductStatus extends BusinessException {
    public UnsupportedProductStatus() {
        super(ProductErrorCode.UNSUPPORTED_PRODUCT_STATUS);
    }
}
