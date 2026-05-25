package com.sparta.whereismyparcel.user.presentation.dto.response;

import com.sparta.whereismyparcel.user.domain.entity.User;
import com.sparta.whereismyparcel.user.domain.UserRole;
import com.sparta.whereismyparcel.user.domain.UserStatus;
import java.util.UUID;

public record SignupResponse(
		UUID userId,
		String username,
		String name,
		String email,
		UserRole role,
		UserStatus status
) {
	public static SignupResponse from(User user) {
		return new SignupResponse(
				user.getUserId(),
				user.getUsername(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				user.getStatus()
		);
	}
}
