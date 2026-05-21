package com.sparta.whereismyparcel.user.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;
import com.sparta.whereismyparcel.common.exception.CommonErrorCode;

public class ForbiddenException extends BusinessException {

	public ForbiddenException() {
		super(CommonErrorCode.FORBIDDEN);
	}
}
