package com.sparta.whereismyparcel.common.exception;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * 서비스에서 명시적으로 던지는 비즈니스 예외를 공통 응답 포맷으로 변환합니다.
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(
		BusinessException exception,
		HttpServletRequest request
	) {
		log.warn(
			"[BusinessException] uri={}, code={}, message={}",
			request.getRequestURI(),
			exception.getErrorCode().getCode(),
			exception.getMessage()
		);

		return ResponseEntity
			.status(exception.getErrorCode().getStatus())
			.body(ApiResponse.error(exception.getErrorCode(), exception.getMessage()));
	}

	/**
	 * @Valid 검증 실패 시 필드별 오류 목록을 data에 담아 반환합니다.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<List<ApiResponse.FieldError>>> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException exception,
		HttpServletRequest request
	) {
		List<ApiResponse.FieldError> errors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::toFieldError)
			.toList();

		log.warn("[ValidationFailed] uri={}, fieldErrorCount={}", request.getRequestURI(), errors.size());

		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE, errors));
	}

	/**
	 * @RequestParam, @PathVariable 등에서 발생하는 validation 실패를 처리합니다.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<List<ApiResponse.FieldError>>> handleConstraintViolationException(
		ConstraintViolationException exception,
		HttpServletRequest request
	) {
		List<ApiResponse.FieldError> errors = exception.getConstraintViolations()
			.stream()
			.map(violation -> ApiResponse.FieldError.of(
				violation.getPropertyPath().toString(),
				violation.getMessage()
			))
			.toList();

		log.warn("[ConstraintViolation] uri={}, violationCount={}", request.getRequestURI(), errors.size());

		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE, errors));
	}

	/**
	 * JSON 파싱 실패, enum 변환 실패처럼 요청 본문을 읽을 수 없는 경우를 처리합니다.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
		HttpMessageNotReadableException exception,
		HttpServletRequest request
	) {
		Throwable cause = exception.getMostSpecificCause();
		String causeType = (cause == null ? exception : cause).getClass().getSimpleName();
		log.warn("[InvalidRequestBody] uri={}, causeType={}", request.getRequestURI(), causeType);

		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE));
	}

	/**
	 * 지원하지 않는 HTTP 메서드로 요청한 경우를 처리합니다.
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
		HttpRequestMethodNotSupportedException exception,
		HttpServletRequest request
	) {
		log.warn("[MethodNotAllowed] uri={}, method={}", request.getRequestURI(), exception.getMethod());

		return ResponseEntity
			.status(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus())
			.body(ApiResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED));
	}

	/**
	 * 처리되지 않은 예외의 스택트레이스는 로그에만 남기고, 클라이언트에는 공통 서버 오류 응답을 반환합니다.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(
		Exception exception,
		HttpServletRequest request
	) {
		Sentry.withScope(scope -> {
			scope.setTag("uri", request.getRequestURI());
			scope.setTag("method", request.getMethod());
			Sentry.captureException(exception);
		});
		log.error("[UnhandledException] uri={}, message={}", request.getRequestURI(), exception.getMessage(), exception);

		return ResponseEntity
			.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, exception.getMessage() + " / " + exception.getClass().getName()));
	}

	private ApiResponse.FieldError toFieldError(FieldError fieldError) {
		return ApiResponse.FieldError.of(
			fieldError.getField(),
			fieldError.getRejectedValue(),
			fieldError.getDefaultMessage()
		);
	}
}
