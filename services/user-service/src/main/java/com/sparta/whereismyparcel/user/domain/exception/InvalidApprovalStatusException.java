package com.sparta.whereismyparcel.user.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class InvalidApprovalStatusException extends BusinessException {

	public InvalidApprovalStatusException() {
		super(UserErrorCode.INVALID_APPROVAL_STATUS);
	}
}
