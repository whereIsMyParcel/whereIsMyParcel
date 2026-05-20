package com.sparta.whereismyparcel.user.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class UserAlreadyExistsException extends BusinessException {

	public UserAlreadyExistsException() {
		super(UserErrorCode.USER_ALREADY_EXISTS);
	}
}
