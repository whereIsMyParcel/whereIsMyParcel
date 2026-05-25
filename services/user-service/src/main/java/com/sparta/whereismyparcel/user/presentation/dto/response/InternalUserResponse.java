package com.sparta.whereismyparcel.user.presentation.dto.response;

import com.sparta.whereismyparcel.user.domain.UserRole;
import com.sparta.whereismyparcel.user.domain.UserStatus;
import com.sparta.whereismyparcel.user.domain.entity.User;
import java.util.UUID;

public record InternalUserResponse(
		UUID userId,
		String username,
		String name,
		String email,
		UserRole role,
		UserStatus status,
		String slackId,
		String businessNumber,
		UUID hubId,
		UUID companyId
) {
	public static InternalUserResponse from(User user) {
		return new InternalUserResponse(
				user.getUserId(),
				user.getUsername(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				user.getStatus(),
				user.getSlackId(),
				user.getBusinessNumber(),
				user.getHubId(),
				user.getCompanyId()
		);
	}
}
