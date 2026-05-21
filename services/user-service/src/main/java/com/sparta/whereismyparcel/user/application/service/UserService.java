package com.sparta.whereismyparcel.user.application.service;

import com.sparta.whereismyparcel.user.domain.entity.User;
import com.sparta.whereismyparcel.user.domain.exception.UserAlreadyExistsException;
import com.sparta.whereismyparcel.user.domain.exception.UserNotFoundException;
import com.sparta.whereismyparcel.user.domain.repository.UserRepository;
import com.sparta.whereismyparcel.user.infrastructure.keycloak.KeycloakAdminService;
import com.sparta.whereismyparcel.user.presentation.dto.request.SignupRequest;
import com.sparta.whereismyparcel.user.presentation.dto.response.ApproveResponse;
import com.sparta.whereismyparcel.user.presentation.dto.response.SignupResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

		User user = createUser(userId, request);
		userRepository.save(user);
		log.info("회원가입 완료. userId={}, username={}", userId, request.username());
		return SignupResponse.from(user);
	}

	@Transactional
	public ApproveResponse approve(UUID userId) {
		User user = findUserById(userId);
		keycloakAdminService.enableUser(userId);
		user.approve();
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
