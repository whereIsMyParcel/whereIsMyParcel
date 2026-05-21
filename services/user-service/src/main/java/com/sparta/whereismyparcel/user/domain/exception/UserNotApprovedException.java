package com.sparta.whereismyparcel.user.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class UserNotApprovedException extends BusinessException {

	public UserNotApprovedException() {
		super(UserErrorCode.USER_NOT_APPROVED);
	}
}
