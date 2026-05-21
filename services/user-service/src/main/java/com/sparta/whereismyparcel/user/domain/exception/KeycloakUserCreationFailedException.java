package com.sparta.whereismyparcel.user.domain.exception;

import com.sparta.whereismyparcel.common.exception.BusinessException;

public class KeycloakUserCreationFailedException extends BusinessException {

	public KeycloakUserCreationFailedException() {
		super(UserErrorCode.KEYCLOAK_USER_CREATION_FAILED);
	}
}
