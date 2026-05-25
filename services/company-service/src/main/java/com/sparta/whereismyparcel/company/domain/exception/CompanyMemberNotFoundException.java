package com.sparta.whereismyparcel.company.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class CompanyMemberNotFoundException extends BusinessException {
    public CompanyMemberNotFoundException() {super(CompanyErrorCode.COMPANY_MEMBER_NOT_FOUND);
    }
}
