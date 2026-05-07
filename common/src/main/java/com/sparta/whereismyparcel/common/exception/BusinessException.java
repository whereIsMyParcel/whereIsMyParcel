package com.sparta.whereismyparcel.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(resolveMessage(errorCode, message));
		this.errorCode = errorCode;
	}

	private static String resolveMessage(ErrorCode errorCode, String message) {
		if (message == null || message.isBlank()) {
			return errorCode.getMessage();
		}

		return message;
	}
}
