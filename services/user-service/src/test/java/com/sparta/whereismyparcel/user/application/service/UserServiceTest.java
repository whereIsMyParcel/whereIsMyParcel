package com.sparta.whereismyparcel.user.application.service;

import com.sparta.whereismyparcel.user.domain.UserRole;
import com.sparta.whereismyparcel.user.domain.UserStatus;
import com.sparta.whereismyparcel.user.domain.entity.User;
import com.sparta.whereismyparcel.user.domain.exception.InvalidApprovalStatusException;
import com.sparta.whereismyparcel.user.domain.exception.UserAlreadyExistsException;
import com.sparta.whereismyparcel.user.domain.exception.UserNotFoundException;
import com.sparta.whereismyparcel.user.domain.repository.UserRepository;
import com.sparta.whereismyparcel.user.infrastructure.keycloak.KeycloakAdminService;
import com.sparta.whereismyparcel.user.presentation.dto.request.SignupRequest;
import com.sparta.whereismyparcel.user.presentation.dto.request.UpdateUserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
			given(userRepository.saveAndFlush(any(User.class))).willAnswer(inv -> inv.getArgument(0));

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
			given(userRepository.saveAndFlush(any(User.class))).willAnswer(inv -> inv.getArgument(0));

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

	@Nested
	@DisplayName("회원 단건 조회")
	class GetUser {

		@Test
		@DisplayName("존재하는 사용자를 조회하면 UserResponse를 반환한다")
		void success() {
			// given
			UUID userId = UUID.randomUUID();
			User user = createUser(userId, UserStatus.APPROVED);
			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			// when
			var response = userService.getUser(userId);

			// then
			assertThat(response.userId()).isEqualTo(userId);
			assertThat(response.username()).isEqualTo("user01");
			assertThat(response.status()).isEqualTo(UserStatus.APPROVED);
		}

		@Test
		@DisplayName("존재하지 않는 사용자를 조회하면 UserNotFoundException이 발생한다")
		void userNotFound() {
			// given
			UUID userId = UUID.randomUUID();
			given(userRepository.findById(userId)).willReturn(Optional.empty());

			// when // then
			assertThatThrownBy(() -> userService.getUser(userId))
					.isInstanceOf(UserNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("회원 목록 조회")
	class GetUsers {

		private final Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

		@Test
		@DisplayName("필터 없이 전체 목록을 조회한다")
		void successWithNoFilter() {
			// given
			List<User> users = List.of(
					createUser(UUID.randomUUID(), UserStatus.PENDING),
					createUser(UUID.randomUUID(), UserStatus.APPROVED)
			);
			given(userRepository.findAllByFilter(null, null, pageable))
					.willReturn(new PageImpl<>(users, pageable, users.size()));

			// when
			var response = userService.getUsers(null, null, pageable);

			// then
			assertThat(response.getTotalElements()).isEqualTo(2);
		}

		@Test
		@DisplayName("role로 필터링하면 해당 role의 사용자만 반환된다")
		void filteredByRole() {
			// given
			User user = createUser(UUID.randomUUID(), UserStatus.APPROVED);
			given(userRepository.findAllByFilter(UserRole.COMPANY_MANAGER, null, pageable))
					.willReturn(new PageImpl<>(List.of(user), pageable, 1));

			// when
			var response = userService.getUsers(UserRole.COMPANY_MANAGER, null, pageable);

			// then
			assertThat(response.getTotalElements()).isEqualTo(1);
			assertThat(response.getContent().get(0).role()).isEqualTo(UserRole.COMPANY_MANAGER);
		}

		@Test
		@DisplayName("status로 필터링하면 해당 status의 사용자만 반환된다")
		void filteredByStatus() {
			// given
			User user = createUser(UUID.randomUUID(), UserStatus.PENDING);
			given(userRepository.findAllByFilter(null, UserStatus.PENDING, pageable))
					.willReturn(new PageImpl<>(List.of(user), pageable, 1));

			// when
			var response = userService.getUsers(null, UserStatus.PENDING, pageable);

			// then
			assertThat(response.getTotalElements()).isEqualTo(1);
			assertThat(response.getContent().get(0).status()).isEqualTo(UserStatus.PENDING);
		}

		@Test
		@DisplayName("role과 status를 모두 지정하면 두 조건을 모두 만족하는 사용자만 반환된다")
		void filteredByRoleAndStatus() {
			// given
			User user = createUser(UUID.randomUUID(), UserStatus.PENDING);
			given(userRepository.findAllByFilter(UserRole.COMPANY_MANAGER, UserStatus.PENDING, pageable))
					.willReturn(new PageImpl<>(List.of(user), pageable, 1));

			// when
			var response = userService.getUsers(UserRole.COMPANY_MANAGER, UserStatus.PENDING, pageable);

			// then
			assertThat(response.getTotalElements()).isEqualTo(1);
		}

		@Test
		@DisplayName("조건에 맞는 사용자가 없으면 빈 페이지를 반환한다")
		void emptyResult() {
			// given
			given(userRepository.findAllByFilter(UserRole.MASTER, UserStatus.REJECTED, pageable))
					.willReturn(new PageImpl<>(List.of(), pageable, 0));

			// when
			var response = userService.getUsers(UserRole.MASTER, UserStatus.REJECTED, pageable);

			// then
			assertThat(response.getTotalElements()).isEqualTo(0);
			assertThat(response.getContent()).isEmpty();
		}
	}

	@Nested
	@DisplayName("Slack ID로 내부 유저 조회")
	class GetInternalUserBySlackId {

		@Test
		@DisplayName("존재하는 slackId로 조회하면 InternalUserResponse를 반환한다")
		void success() {
			// given
			UUID userId = UUID.randomUUID();
			User user = createUser(userId, UserStatus.APPROVED);
			given(userRepository.findBySlackId("SLACK_ID")).willReturn(Optional.of(user));

			// when
			var response = userService.getInternalUserBySlackId("SLACK_ID");

			// then
			assertThat(response.userId()).isEqualTo(userId);
			assertThat(response.slackId()).isEqualTo("SLACK_ID");
			assertThat(response.status()).isEqualTo(UserStatus.APPROVED);
		}

		@Test
		@DisplayName("존재하지 않는 slackId로 조회하면 UserNotFoundException이 발생한다")
		void slackIdNotFound() {
			// given
			given(userRepository.findBySlackId("NOT_EXIST")).willReturn(Optional.empty());

			// when // then
			assertThatThrownBy(() -> userService.getInternalUserBySlackId("NOT_EXIST"))
					.isInstanceOf(UserNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("회원 수정")
	class UpdateUser {

		@Test
		@DisplayName("전달한 필드로 수정된다")
		void success() {
			// given
			UUID userId = UUID.randomUUID();
			User user = createUser(userId, UserStatus.APPROVED);
			UpdateUserRequest request = new UpdateUserRequest("변경된이름", "010-1111-2222", "NEW_SLACK");
			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			// when
			var response = userService.updateUser(userId, request);

			// then
			assertThat(response.name()).isEqualTo("변경된이름");
			assertThat(response.phone()).isEqualTo("010-1111-2222");
			assertThat(response.slackId()).isEqualTo("NEW_SLACK");
		}

		@Test
		@DisplayName("null인 필드는 기존 값을 유지한다")
		void nullFieldsKeepExistingValues() {
			// given
			UUID userId = UUID.randomUUID();
			User user = createUser(userId, UserStatus.APPROVED);
			UpdateUserRequest request = new UpdateUserRequest("변경된이름", null, null);
			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			// when
			var response = userService.updateUser(userId, request);

			// then
			assertThat(response.name()).isEqualTo("변경된이름");
			assertThat(response.phone()).isEqualTo("010-0000-0000");
			assertThat(response.slackId()).isEqualTo("SLACK_ID");
		}

		@Test
		@DisplayName("존재하지 않는 사용자는 수정할 수 없다")
		void userNotFound() {
			// given
			UUID userId = UUID.randomUUID();
			UpdateUserRequest request = new UpdateUserRequest("변경된이름", null, null);
			given(userRepository.findById(userId)).willReturn(Optional.empty());

			// when // then
			assertThatThrownBy(() -> userService.updateUser(userId, request))
					.isInstanceOf(UserNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("회원 삭제")
	class DeleteUser {

		@Test
		@DisplayName("회원을 삭제하면 soft delete 처리되고 Keycloak 계정이 비활성화된다")
		void success() {
			// given
			UUID userId = UUID.randomUUID();
			User user = createUser(userId, UserStatus.APPROVED);
			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			// when
			userService.deleteUser(userId, "master-id");

			// then
			assertThat(user.isDeleted()).isTrue();
			then(keycloakAdminService).should().disableUser(userId);
		}

		@Test
		@DisplayName("존재하지 않는 사용자는 삭제할 수 없다")
		void userNotFound() {
			// given
			UUID userId = UUID.randomUUID();
			given(userRepository.findById(userId)).willReturn(Optional.empty());

			// when // then
			assertThatThrownBy(() -> userService.deleteUser(userId, "master-id"))
					.isInstanceOf(UserNotFoundException.class);
			then(keycloakAdminService).should(never()).disableUser(any());
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
