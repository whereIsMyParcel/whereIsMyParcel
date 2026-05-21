package com.sparta.whereismyparcel.user.presentation.dto.response;

import com.sparta.whereismyparcel.user.domain.entity.User;
import com.sparta.whereismyparcel.user.domain.entity.UserStatus;
import java.util.UUID;

public record ApproveResponse(
		UUID userId,
		String username,
		UserStatus status
) {
	public static ApproveResponse from(User user) {
		return new ApproveResponse(
				user.getUserId(),
				user.getUsername(),
				user.getStatus()
		);
	}
}
