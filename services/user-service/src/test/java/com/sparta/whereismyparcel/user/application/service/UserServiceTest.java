package com.sparta.whereismyparcel.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sparta.whereismyparcel.user.domain.entity.User;
import com.sparta.whereismyparcel.user.domain.entity.UserRole;
import com.sparta.whereismyparcel.user.domain.entity.UserStatus;
import com.sparta.whereismyparcel.user.domain.exception.InvalidApprovalStatusException;
import com.sparta.whereismyparcel.user.domain.exception.UserAlreadyExistsException;
import com.sparta.whereismyparcel.user.domain.exception.UserNotFoundException;
import com.sparta.whereismyparcel.user.domain.repository.UserRepository;
import com.sparta.whereismyparcel.user.infrastructure.keycloak.KeycloakAdminService;
import com.sparta.whereismyparcel.user.presentation.dto.request.SignupRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private KeycloakAdminService keycloakAdminService;

	@InjectMocks
	private UserService userService;

	@Nested
	@DisplayName("회원가입")
	class Signup {

		@Test
		@DisplayName("유효한 정보로 회원가입을 할 수 있다")
		void success() {
			// given
			UUID keycloakId = UUID.randomUUID();
			SignupRequest request = createSignupRequest("user01", "홍길동", "hong@test.com",
					UserRole.COMPANY_MANAGER, "123-45-67890", null, null);

			given(userRepository.existsByUsername("user01")).willReturn(false);
			given(userRepository.existsByEmail("hong@test.com")).willReturn(false);
			given(userRepository.existsByBusinessNumber("123-45-67890")).willReturn(false);
			given(keycloakAdminService.createUser(anyString(), anyString(), anyString()))
					.willReturn(keycloakId);
			given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

			// when
			var response = userService.signup(request);

			// then
			assertThat(response.userId()).isEqualTo(keycloakId);
			assertThat(response.username()).isEqualTo("user01");
			assertThat(response.name()).isEqualTo("홍길동");
			assertThat(response.email()).isEqualTo("hong@test.com");
			assertThat(response.role()).isEqualTo(UserRole.COMPANY_MANAGER);
			assertThat(response.status()).isEqualTo(UserStatus.PENDING);
		}

		@Test
		@DisplayName("이미 존재하는 username이면 회원가입을 할 수 없다")
		void duplicateUsername() {
			// given
			SignupRequest request = createSignupRequest("user01", "홍길동", "hong@test.com",
					UserRole.HUB_MANAGER, null, null, null);

			given(userRepository.existsByUsername("user01")).willReturn(true);

			// when // then
			assertThatThrownBy(() -> userService.signup(request))
					.isInstanceOf(UserAlreadyExistsException.class);
			then(keycloakAdminService).should(never()).createUser(anyString(), anyString(), anyString());
			then(userRepository).should(never()).save(any(User.class));
		}

		@Test
		@DisplayName("이미 존재하는 email이면 회원가입을 할 수 없다")
		void duplicateEmail() {
			// given
			SignupRequest request = createSignupRequest("user01", "홍길동", "hong@test.com",
					UserRole.HUB_MANAGER, null, null, null);

			given(userRepository.existsByUsername("user01")).willReturn(false);
			given(userRepository.existsByEmail("hong@test.com")).willReturn(true);

			// when // then
			assertThatThrownBy(() -> userService.signup(request))
					.isInstanceOf(UserAlreadyExistsException.class);
			then(keycloakAdminService).should(never()).createUser(anyString(), anyString(), anyString());
			then(userRepository).should(never()).save(any(User.class));
		}

		@Test
		@DisplayName("이미 존재하는 사업자등록번호면 회원가입을 할 수 없다")
		void duplicateBusinessNumber() {
			// given
			SignupRequest request = createSignupRequest("user01", "홍길동", "hong@test.com",
					UserRole.COMPANY_MANAGER, "123-45-67890", null, null);

			given(userRepository.existsByUsername("user01")).willReturn(false);
			given(userRepository.existsByEmail("hong@test.com")).willReturn(false);
			given(userRepository.existsByBusinessNumber("123-45-67890")).willReturn(true);

			// when // then
			assertThatThrownBy(() -> userService.signup(request))
					.isInstanceOf(UserAlreadyExistsException.class);
			then(keycloakAdminService).should(never()).createUser(anyString(), anyString(), anyString());
			then(userRepository).should(never()).save(any(User.class));
		}

		@Test
		@DisplayName("사업자등록번호가 없으면 중복 검사를 건너뛴다")
		void nullBusinessNumberSkipsDuplicateCheck() {
			// given
			UUID keycloakId = UUID.randomUUID();
			SignupRequest request = createSignupRequest("user01", "홍길동", "hong@test.com",
					UserRole.HUB_MANAGER, null, UUID.randomUUID(), null);

			given(userRepository.existsByUsername("user01")).willReturn(false);
			given(userRepository.existsByEmail("hong@test.com")).willReturn(false);
			given(keycloakAdminService.createUser(anyString(), anyString(), anyString()))
					.willReturn(keycloakId);
			given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

			// when
			var response = userService.signup(request);

			// then
			assertThat(response.status()).isEqualTo(UserStatus.PENDING);
			then(userRepository).should(never()).existsByBusinessNumber(any());
		}
	}

	@Nested
	@DisplayName("회원 승인")
	class Approve {

		@Test
		@DisplayName("PENDING 상태의 사용자를 승인하면 APPROVED 상태가 된다")
		void success() {
			// given
			UUID userId = UUID.randomUUID();
			User user = createUser(userId, UserStatus.PENDING);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			// when
			var response = userService.approve(userId);

			// then
			assertThat(response.userId()).isEqualTo(userId);
			assertThat(response.status()).isEqualTo(UserStatus.APPROVED);
			then(keycloakAdminService).should().enableUser(userId);
		}

		@Test
		@DisplayName("존재하지 않는 사용자는 승인할 수 없다")
		void userNotFound() {
			// given
			UUID userId = UUID.randomUUID();
			given(userRepository.findById(userId)).willReturn(Optional.empty());

			// when // then
			assertThatThrownBy(() -> userService.approve(userId))
					.isInstanceOf(UserNotFoundException.class);
			then(keycloakAdminService).should(never()).enableUser(any());
		}

		@Test
		@DisplayName("이미 승인된 사용자는 다시 승인할 수 없다")
		void alreadyApproved() {
			// given
			UUID userId = UUID.randomUUID();
			User user = createUser(userId, UserStatus.APPROVED);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			// when // then
			assertThatThrownBy(() -> userService.approve(userId))
					.isInstanceOf(InvalidApprovalStatusException.class);
		}

		@Test
		@DisplayName("거절된 사용자는 승인할 수 없다")
		void rejectedUserCannotBeApproved() {
			// given
			UUID userId = UUID.randomUUID();
			User user = createUser(userId, UserStatus.REJECTED);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			// when // then
			assertThatThrownBy(() -> userService.approve(userId))
					.isInstanceOf(InvalidApprovalStatusException.class);
		}
	}

	@Nested
	@DisplayName("회원 거절")
	class Reject {

		@Test
		@DisplayName("PENDING 상태의 사용자를 거절하면 REJECTED 상태가 된다")
		void success() {
			// given
			UUID userId = UUID.randomUUID();
			User user = createUser(userId, UserStatus.PENDING);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			// when
			var response = userService.reject(userId);

			// then
			assertThat(response.userId()).isEqualTo(userId);
			assertThat(response.status()).isEqualTo(UserStatus.REJECTED);
			then(keycloakAdminService).should(never()).enableUser(any());
		}

		@Test
		@DisplayName("존재하지 않는 사용자는 거절할 수 없다")
		void userNotFound() {
			// given
			UUID userId = UUID.randomUUID();
			given(userRepository.findById(userId)).willReturn(Optional.empty());

			// when // then
			assertThatThrownBy(() -> userService.reject(userId))
					.isInstanceOf(UserNotFoundException.class);
		}

		@Test
		@DisplayName("이미 승인된 사용자는 거절할 수 없다")
		void alreadyApproved() {
			// given
			UUID userId = UUID.randomUUID();
			User user = createUser(userId, UserStatus.APPROVED);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			// when // then
			assertThatThrownBy(() -> userService.reject(userId))
					.isInstanceOf(InvalidApprovalStatusException.class);
		}
	}

	private SignupRequest createSignupRequest(String username, String name, String email,
			UserRole role, String businessNumber, UUID hubId, UUID companyId) {
		return new SignupRequest(username, name, email, "Test1234!", "010-0000-0000",
				"SLACK_ID", role, businessNumber, hubId, companyId);
	}

	private User createUser(UUID userId, UserStatus status) {
		User user = User.create(userId, "user01", "홍길동", "hong@test.com",
				"010-0000-0000", UserRole.COMPANY_MANAGER, "SLACK_ID",
				"123-45-67890", null, null);
		ReflectionTestUtils.setField(user, "status", status);
		return user;
	}
}
