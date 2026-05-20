package com.sparta.whereismyparcel.user.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class UserNotFoundException extends BusinessException {

	public UserNotFoundException() {
		super(UserErrorCode.USER_NOT_FOUND);
	}
}
