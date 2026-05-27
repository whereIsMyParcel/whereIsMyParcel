package com.sparta.whereismyparcel.company.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class BusinessNumberIsExistsException extends BusinessException {
    public BusinessNumberIsExistsException() {
        super(CompanyErrorCode.BUSINESS_NUMBER_IS_EXISTS);
    }
}
