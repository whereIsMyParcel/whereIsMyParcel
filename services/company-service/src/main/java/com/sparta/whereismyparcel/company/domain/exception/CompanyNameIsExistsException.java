package com.sparta.whereismyparcel.company.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class CompanyNameIsExistsException extends BusinessException {
    public CompanyNameIsExistsException() {
        super(CompanyErrorCode.COMPANY_NAME_IS_EXISTS);
    }
}
