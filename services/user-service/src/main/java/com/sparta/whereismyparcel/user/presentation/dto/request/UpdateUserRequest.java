package com.sparta.whereismyparcel.user.presentation.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

		@Size(max = 50, message = "이름은 50자 이하여야 합니다.")
		String name,

		@Pattern(regexp = "^01\\d-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
		String phone,

		String slackId
) {
	public UpdateUserRequest {
		// 공백 문자열을 null로 정규화 — isValidRequest()가 빈 문자열을 "값 있음"으로 오인하지 않도록
		name = (name != null && !name.isBlank()) ? name.trim() : null;
		phone = (phone != null && !phone.isBlank()) ? phone.trim() : null;
		slackId = (slackId != null && !slackId.isBlank()) ? slackId.trim() : null;
	}

	@AssertTrue(message = "수정할 필드가 최소 하나 이상 필요합니다.")
	public boolean isValidRequest() {
		return name != null || phone != null || slackId != null;
	}
}
