package com.sparta.whereismyparcel.user.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.user.application.service.UserService;
import com.sparta.whereismyparcel.user.presentation.dto.response.InternalUserResponse;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @Hidden: Swagger 문서에서 제외
// /internal/**은 API Gateway가 외부 트래픽을 라우팅하지 않으므로 인증 없이 허용 (SecurityConfig 참고)
@Hidden
@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class InternalUserController {

	private final UserService userService;

	@GetMapping("/{userId}")
	public ResponseEntity<ApiResponse<InternalUserResponse>> getUser(@PathVariable UUID userId) {
		return ResponseEntity.ok(ApiResponse.success(userService.getInternalUser(userId)));
	}

	@GetMapping("/by-business-number/{businessNumber}")
	public ResponseEntity<ApiResponse<InternalUserResponse>> getUserByBusinessNumber(
			@PathVariable String businessNumber) {
		return ResponseEntity.ok(ApiResponse.success(userService.getInternalUserByBusinessNumber(businessNumber)));
	}

	@GetMapping("/by-slack/{slackId}")
	public ResponseEntity<ApiResponse<InternalUserResponse>> getUserBySlackId(@PathVariable String slackId) {
		return ResponseEntity.ok(ApiResponse.success(userService.getInternalUserBySlackId(slackId)));
	}

	@PatchMapping("/{userId}/companies/{companyId}")
	public ResponseEntity<ApiResponse<Void>> updateUserCompanyId(
			@PathVariable UUID userId, @PathVariable UUID companyId) {
		userService.updateUserCompanyId(userId, companyId);
		return ResponseEntity.ok(ApiResponse.ok());
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<ApiResponse<Void>> disconnectCompany(@PathVariable UUID userId) {
		userService.disconnectCompany(userId);
		return ResponseEntity.ok(ApiResponse.ok());
	}

	@DeleteMapping("/companies/{companyId}")
	public ResponseEntity<ApiResponse<Void>> disconnectCompanyFromAllUsers(@PathVariable UUID companyId) {
		userService.disconnectCompanyFromAllUsers(companyId);
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
