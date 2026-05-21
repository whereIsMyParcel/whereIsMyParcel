package com.sparta.whereismyparcel.user.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.user.application.service.UserService;
import com.sparta.whereismyparcel.user.presentation.dto.request.SignupRequest;
import com.sparta.whereismyparcel.user.presentation.dto.response.ApproveResponse;
import com.sparta.whereismyparcel.user.presentation.dto.response.SignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "회원 관리 API")
@RestController
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@Operation(summary = "회원가입", description = "가입 후 PENDING 상태로 생성되며 관리자 승인 전까지 로그인 불가")
	@PostMapping("/api/v1/auth/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(
			@RequestBody @Valid SignupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(userService.signup(request)));
	}

	@Operation(summary = "회원 승인", description = "MASTER 권한 필요. PENDING 상태의 회원을 APPROVED로 변경하고 Keycloak 계정을 활성화")
	@PreAuthorize("hasRole('MASTER')")
	@PatchMapping("/api/v1/users/{userId}/approve")
	public ResponseEntity<ApiResponse<ApproveResponse>> approve(@PathVariable UUID userId) {
		return ResponseEntity.ok(ApiResponse.success(userService.approve(userId)));
	}

	@Operation(summary = "회원 거절", description = "MASTER 권한 필요. PENDING 상태의 회원을 REJECTED로 변경")
	@PreAuthorize("hasRole('MASTER')")
	@PatchMapping("/api/v1/users/{userId}/reject")
	public ResponseEntity<ApiResponse<ApproveResponse>> reject(@PathVariable UUID userId) {
		return ResponseEntity.ok(ApiResponse.success(userService.reject(userId)));
	}
}
