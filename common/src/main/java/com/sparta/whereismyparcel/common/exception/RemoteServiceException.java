package com.sparta.whereismyparcel.common.exception;

import org.springframework.http.HttpStatus;

public class RemoteServiceException extends BusinessException {

	public RemoteServiceException(int httpStatus, String code, String message) {
		super(new RemoteErrorCode(
				HttpStatus.resolve(httpStatus) != null ? HttpStatus.resolve(httpStatus) : HttpStatus.INTERNAL_SERVER_ERROR,
				code, message));
	}

	// GlobalExceptionHandler가 BusinessException.getErrorCode()를 호출하므로
	// 원격 서비스의 동적 에러 코드를 ErrorCode 구현체로 감싸서 전달
	private record RemoteErrorCode(
			HttpStatus status, String code, String message
	) implements ErrorCode {
		@Override public HttpStatus getStatus() { return status; }
		@Override public String getCode() { return code; }
		@Override public String getMessage() { return message; }
	}
}
