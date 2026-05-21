package com.sparta.whereismyparcel.user.domain.exception;

import com.sparta.whereismyparcel.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
	USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER-002", "이미 존재하는 사용자입니다."),
	USER_NOT_APPROVED(HttpStatus.FORBIDDEN, "USER-003", "승인되지 않은 사용자입니다."),
	INVALID_APPROVAL_STATUS(HttpStatus.BAD_REQUEST, "USER-004", "승인할 수 없는 상태입니다."),
	KEYCLOAK_USER_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER-900", "Keycloak 계정 생성에 실패했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
