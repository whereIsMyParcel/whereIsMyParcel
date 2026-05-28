package com.sparta.whereismyparcel.common.exception;

public class ServiceUnavailableException extends BusinessException {

	public ServiceUnavailableException() {
		super(CommonErrorCode.SERVICE_UNAVAILABLE);
	}
}
