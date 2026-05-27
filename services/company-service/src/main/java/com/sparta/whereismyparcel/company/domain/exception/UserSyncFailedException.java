package com.sparta.whereismyparcel.company.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class UserSyncFailedException extends BusinessException {
    public UserSyncFailedException() {
        super(CompanyErrorCode.USER_SYNC_FAILED);
    }
}
