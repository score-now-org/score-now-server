package com.scorenow.scorenow_api.global.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.scorenow.scorenow_api.global.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 로그에 기록하기 전 사용자 입력 sanitize
	 * CRLF 및 제어 문자 제거로 Log Injection 방지
	 */
	private String sanitizeForLogging(Object value) {
		if (value == null) {
			return "null";
		}
		String str = value.toString();
		// CRLF 및 제어 문자 제거
		return str.replaceAll("[\r\n\t]", "_")
			.replaceAll("\\p{Cntrl}", "_");
	}

	/**
	 * BusinessException 처리
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e){
		log.warn("BusinessException: {}", e.getErrorCode().getCode());

		ErrorCode errorCode = e.getErrorCode();
		ApiResponse<Void> response = ApiResponse.error(
			errorCode.getCode(),
			e.getMessage(),
			e.getDetails()
		);

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(response);
	}

	/**
	 * Validation 예외 처리(@Valid)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e){
		log.warn("Validation failed for {} fields", e.getBindingResult().getErrorCount());

		Map<String, String> errors = new HashMap<>();
		e.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
			errors.put(fieldName, error.getDefaultMessage());
		});

		ApiResponse<Void> response = ApiResponse.error(
			ErrorCode.INVALID_PARAMETER.getCode(),
			ErrorCode.INVALID_PARAMETER.getMessage(),
			errors
		);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(response);
	}

	/**
	 * 필수 파라미터 누락 처리
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(MissingServletRequestParameterException e){
		log.warn("Missing parameter: {}", e.getParameterName());

		Map<String, String> details = Map.of(e.getParameterName(), "필수 파라미터가 누락되었습니다.");
		ApiResponse<Void> response = ApiResponse.error(
			ErrorCode.INVALID_PARAMETER.getCode(),
			ErrorCode.INVALID_PARAMETER.getMessage(),
			details
		);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(response);
	}

	/**
	 * 파라미터 타입 불일치 처리
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
		String expectedType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
		log.warn("Type mismatch for parameter {} (expected type: {})", e.getName(), expectedType);

		String sanitizedValue = sanitizeForLogging(e.getValue());
		String message = String.format("'%s' 파라미터의 값 '%s'이(가) 올바르지 않습니다",
			e.getName(), sanitizedValue);
		ApiResponse<Void> response = ApiResponse.error(
			ErrorCode.INVALID_PARAMETER.getCode(),
			message
		);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(response);
	}

	/**
	 * 지원하지 않는 HTTP Method 처리
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
		log.warn("Method not supported: {}", e.getMethod());

		ApiResponse<Void> response = ApiResponse.error(
			ErrorCode.METHOD_NOT_ALLOWED.getCode(),
			ErrorCode.METHOD_NOT_ALLOWED.getMessage()
		);

		return ResponseEntity
			.status(HttpStatus.METHOD_NOT_ALLOWED)
			.body(response);
	}

	/**
	 * 그 외 모든 예외 처리
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		log.error("Unexpected error occurred", e);

		ApiResponse<Void> response = ApiResponse.error(
			ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
			ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
		);

		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(response);
	}
}
