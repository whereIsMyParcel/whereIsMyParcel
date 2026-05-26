package com.sparta.whereismyparcel.user.application.service;

import com.sparta.whereismyparcel.user.domain.entity.User;
import com.sparta.whereismyparcel.user.domain.UserRole;
import com.sparta.whereismyparcel.user.domain.UserStatus;
import com.sparta.whereismyparcel.user.domain.exception.UserAlreadyExistsException;
import com.sparta.whereismyparcel.user.domain.exception.UserNotFoundException;
import com.sparta.whereismyparcel.user.domain.repository.UserRepository;
import com.sparta.whereismyparcel.user.infrastructure.keycloak.KeycloakAdminService;
import com.sparta.whereismyparcel.user.presentation.dto.request.SignupRequest;
import com.sparta.whereismyparcel.user.presentation.dto.request.UpdateUserRequest;
import com.sparta.whereismyparcel.user.presentation.dto.response.ApproveResponse;
import com.sparta.whereismyparcel.user.presentation.dto.response.InternalUserResponse;
import com.sparta.whereismyparcel.user.presentation.dto.response.SignupResponse;
import com.sparta.whereismyparcel.user.presentation.dto.response.UserResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final KeycloakAdminService keycloakAdminService;

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		validateDuplicates(request);

		UUID userId = keycloakAdminService.createUser(
				request.username(), request.email(), request.password());

		User user;
		try {
			// saveAndFlush: 트랜잭션 커밋 전 즉시 SQL을 실행해 DB 예외를 여기서 잡음
			user = userRepository.saveAndFlush(createUser(userId, request));
		} catch (Exception e) {
			log.error("DB 유저 저장 실패. Keycloak 유저 삭제 시도. userId={}", userId, e);
			tryRollbackKeycloakUser(userId);
			throw e;
		}

		log.info("회원가입 완료. userId={}, username={}", userId, request.username());
		return SignupResponse.from(user);
	}

	@Transactional
	public ApproveResponse approve(UUID userId) {
		User user = findUserById(userId);
		user.approve();
		keycloakAdminService.enableUser(userId);
		log.info("승인 처리 완료. userId={}", userId);
		return ApproveResponse.from(user);
	}

	@Transactional
	public ApproveResponse reject(UUID userId) {
		User user = findUserById(userId);
		user.reject();
		log.info("거절 처리 완료. userId={}", userId);
		return ApproveResponse.from(user);
	}

	public UserResponse getUser(UUID userId) {
		return UserResponse.from(findUserById(userId));
	}

	public InternalUserResponse getInternalUser(UUID userId) {
		return InternalUserResponse.from(findUserById(userId));
	}

	public InternalUserResponse getInternalUserBySlackId(String slackId) {
		User user = userRepository.findBySlackId(slackId)
				.orElseThrow(UserNotFoundException::new);
		return InternalUserResponse.from(user);
	}

	public InternalUserResponse getInternalUserByBusinessNumber(String businessNumber) {
		User user = userRepository.findByBusinessNumber(businessNumber)
				.orElseThrow(UserNotFoundException::new);
		return InternalUserResponse.from(user);
	}

	public Page<UserResponse> getUsers(UserRole role, UserStatus status, Pageable pageable) {
		return userRepository.findAllByFilter(role, status, pageable).map(UserResponse::from);
	}

	@Transactional
	public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
		User user = findUserById(userId);
		user.update(request.name(), request.phone(), request.slackId());
		return UserResponse.from(user);
	}

	@Transactional
	public void deleteUser(UUID userId, String requestedBy) {
		User user = findUserById(userId);
		user.softDelete(requestedBy);
		keycloakAdminService.disableUser(userId);
		log.info("회원 삭제 완료. userId={}", userId);
	}

	// 보상 트랜잭션(best-effort): DB 저장 실패 시 Keycloak 유저 삭제를 시도하되
	// Keycloak 삭제마저 실패하면 원래 예외만 rethrow하고 수동 처리에 위임
	private void tryRollbackKeycloakUser(UUID userId) {
		try {
			keycloakAdminService.deleteUser(userId);
		} catch (Exception e) {
			log.error("Keycloak 유저 삭제 실패. 수동 처리 필요. userId={}", userId, e);
		}
	}

	private User findUserById(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(UserNotFoundException::new);
	}

	private void validateDuplicates(SignupRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new UserAlreadyExistsException();
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new UserAlreadyExistsException();
		}
		if (request.businessNumber() != null
				&& userRepository.existsByBusinessNumber(request.businessNumber())) {
			throw new UserAlreadyExistsException();
		}
	}

	private User createUser(UUID userId, SignupRequest request) {
		return User.create(
				userId,
				request.username(),
				request.name(),
				request.email(),
				request.phone(),
				request.role(),
				request.slackId(),
				request.businessNumber(),
				request.hubId(),
				request.companyId()
		);
	}
}
