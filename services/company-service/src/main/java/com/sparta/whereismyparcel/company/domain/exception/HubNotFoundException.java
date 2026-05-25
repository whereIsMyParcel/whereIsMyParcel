package com.sparta.whereismyparcel.company.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class HubNotFoundException extends BusinessException {
    public HubNotFoundException() {
        super(CompanyErrorCode.HUB_NOT_FOUND);
    }
}
