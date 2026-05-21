package com.sparta.whereismyparcel.user.presentation.dto.request;

import com.sparta.whereismyparcel.user.domain.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SignupRequest(

		@NotBlank
		@Pattern(regexp = "^[a-z0-9]{4,10}$", message = "username은 4~10자 소문자와 숫자만 가능합니다.")
		String username,

		@NotBlank
		@Size(max = 50, message = "이름은 50자 이하여야 합니다.")
		String name,

		@NotBlank
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		String email,

		@NotBlank
		@Pattern(
				regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,15}$",
				message = "password는 8~15자, 대소문자·숫자·특수문자를 포함해야 합니다."
		)
		String password,

		@Pattern(regexp = "^01\\d-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
		String phone,

		@NotBlank
		String slackId,

		@NotNull
		UserRole role,

		@Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자등록번호 형식이 올바르지 않습니다. (예: 123-45-67890)")
		String businessNumber,

		UUID hubId,

		UUID companyId
) {
	public SignupRequest {
		name = name != null ? name.trim() : null;
		slackId = slackId != null ? slackId.trim() : null;
		phone = (phone != null && phone.isBlank()) ? null : phone;
		businessNumber = (businessNumber != null && businessNumber.isBlank()) ? null : businessNumber;
	}
}
