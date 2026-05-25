package com.sparta.whereismyparcel.company.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class CompanyNotFoundException extends BusinessException {
    public CompanyNotFoundException() {
        super(CompanyErrorCode.COMPANY_NOT_FOUND);
    }
}
