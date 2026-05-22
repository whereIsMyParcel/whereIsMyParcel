package com.sparta.whereismyparcel.user.presentation.dto.response;

import com.sparta.whereismyparcel.user.domain.entity.User;
import com.sparta.whereismyparcel.user.domain.UserRole;
import com.sparta.whereismyparcel.user.domain.UserStatus;
import java.util.UUID;

public record UserResponse(
		UUID userId,
		String username,
		String name,
		String email,
		String phone,
		UserRole role,
		UserStatus status,
		String slackId,
		String businessNumber,
		UUID hubId,
		UUID companyId
) {
	public static UserResponse from(User user) {
		return new UserResponse(
				user.getUserId(),
				user.getUsername(),
				user.getName(),
				user.getEmail(),
				user.getPhone(),
				user.getRole(),
				user.getStatus(),
				user.getSlackId(),
				user.getBusinessNumber(),
				user.getHubId(),
				user.getCompanyId()
		);
	}
}
