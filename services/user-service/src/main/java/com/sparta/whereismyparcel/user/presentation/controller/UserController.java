package com.sparta.whereismyparcel.user.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.common.util.PageableUtils;
import com.sparta.whereismyparcel.user.application.service.UserService;
import com.sparta.whereismyparcel.user.domain.UserRole;
import com.sparta.whereismyparcel.user.domain.UserStatus;
import com.sparta.whereismyparcel.user.presentation.dto.request.SignupRequest;
import com.sparta.whereismyparcel.user.presentation.dto.request.UpdateUserRequest;
import com.sparta.whereismyparcel.user.presentation.dto.response.ApproveResponse;
import com.sparta.whereismyparcel.user.presentation.dto.response.SignupResponse;
import com.sparta.whereismyparcel.user.presentation.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "회원 관리 API")
@RestController
@RequiredArgsConstructor
public class UserController {

	private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt");
	private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

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

	@Operation(summary = "회원 단건 조회", description = "MASTER는 모든 회원 조회 가능. 본인은 자신의 정보만 조회 가능")
	@PreAuthorize("hasRole('MASTER') or authentication.name == #userId.toString()")
	@GetMapping("/api/v1/users/{userId}")
	public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID userId) {
		return ResponseEntity.ok(ApiResponse.success(userService.getUser(userId)));
	}

	@Operation(summary = "회원 정보 수정", description = "MASTER 또는 본인만 가능. name·phone·slackId 변경 지원. 전달한 필드만 수정됨")
	@PreAuthorize("hasRole('MASTER') or authentication.name == #userId.toString()")
	@PatchMapping("/api/v1/users/{userId}")
	public ResponseEntity<ApiResponse<UserResponse>> updateUser(
			@PathVariable UUID userId,
			@RequestBody @Valid UpdateUserRequest request) {
		return ResponseEntity.ok(ApiResponse.success(userService.updateUser(userId, request)));
	}

	@Operation(summary = "회원 삭제", description = "MASTER 권한 필요. Soft delete 처리 및 Keycloak 계정 비활성화")
	@PreAuthorize("hasRole('MASTER')")
	@DeleteMapping("/api/v1/users/{userId}")
	public ResponseEntity<ApiResponse<Void>> deleteUser(
			@PathVariable UUID userId,
			@RequestHeader("X-Username") String requestedBy) {
		userService.deleteUser(userId, requestedBy);
		return ResponseEntity.ok(ApiResponse.ok());
	}

	@Operation(summary = "회원 목록 조회", description = "MASTER 권한 필요. role·status 필터, createdAt·updatedAt 정렬 지원. 페이지 크기는 10·30·50만 허용 (이외 값은 10으로 고정)")
	@PreAuthorize("hasRole('MASTER')")
	@GetMapping("/api/v1/users")
	public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
			@RequestParam(required = false) UserRole role,
			@RequestParam(required = false) UserStatus status,
			@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success(
				userService.getUsers(role, status, PageableUtils.normalize(pageable, ALLOWED_SORT_FIELDS, DEFAULT_SORT))));
	}
}
