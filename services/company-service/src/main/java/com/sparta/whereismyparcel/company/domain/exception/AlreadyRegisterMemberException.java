package com.sparta.whereismyparcel.company.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class AlreadyRegisterMemberException extends BusinessException {
    public AlreadyRegisterMemberException() {
        super(CompanyErrorCode.ALREADY_REGISTERED_MEMBER);
    }
}
