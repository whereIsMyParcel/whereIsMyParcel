package com.sparta.whereismyparcel.common.response;

import com.sparta.whereismyparcel.common.exception.ErrorCode;

public record ApiResponse<T>(
	boolean success,
	int status,
	String errorCode,
	String message,
	T data
) {
	// ========== 성공 ==========

	/**
	 * 데이터 없는 200 OK 응답입니다.
	 * record의 success() 접근자와 이름이 충돌하지 않도록 ok()로 명명합니다.
	 */
	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, 200, null, "OK", null);
	}

	/**
	 * 기본 메시지를 사용하는 200 OK 응답입니다.
	 */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, 200, null, "OK", data);
	}

	/**
	 * 커스텀 메시지를 사용하는 200 OK 응답입니다.
	 */
	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(true, 200, null, message, data);
	}

	/**
	 * 리소스 생성 성공을 나타내는 201 Created 응답입니다.
	 */
	public static <T> ApiResponse<T> created(T data) {
		return new ApiResponse<>(true, 201, null, "Created", data);
	}

	// ========== 실패 ==========

	/**
	 * ErrorCode 기반의 표준 실패 응답입니다.
	 */
	public static ApiResponse<Void> error(ErrorCode errorCode) {
		return new ApiResponse<>(
			false,
			errorCode.getStatus().value(),
			errorCode.getCode(),
			errorCode.getMessage(),
			null
		);
	}

	/**
	 * ErrorCode의 상태 코드와 에러 코드는 유지하되 메시지만 재정의하는 실패 응답입니다.
	 */
	public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
		return new ApiResponse<>(
			false,
			errorCode.getStatus().value(),
			errorCode.getCode(),
			resolveMessage(errorCode, message),
			null
		);
	}

	/**
	 * Validation 필드 오류처럼 추가 데이터를 data에 담는 실패 응답입니다.
	 */
	public static <T> ApiResponse<T> error(ErrorCode errorCode, T data) {
		return new ApiResponse<>(
			false,
			errorCode.getStatus().value(),
			errorCode.getCode(),
			errorCode.getMessage(),
			data
		);
	}

	/**
	 * 외부 API 오류 변환 등 ErrorCode enum 없이 직접 구성해야 할 때 사용하는 실패 응답입니다.
	 */
	public static ApiResponse<Void> error(int status, String errorCode, String message) {
		return new ApiResponse<>(false, status, errorCode, message, null);
	}

	private static String resolveMessage(ErrorCode errorCode, String message) {
		if (message == null || message.isBlank()) {
			return errorCode.getMessage();
		}

		return message;
	}

	public record FieldError(
		String field,
		String value,
		String reason
	) {
		public static FieldError of(String field, String reason) {
			return new FieldError(field, null, reason);
		}

		public static FieldError of(String field, Object value, String reason) {
			return new FieldError(
				field,
				value == null ? null : value.toString(),
				reason
			);
		}
	}
}
