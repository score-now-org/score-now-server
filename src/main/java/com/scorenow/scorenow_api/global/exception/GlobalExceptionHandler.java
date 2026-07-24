package com.scorenow.scorenow_api.global.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
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
     * 로그에 API 토큰 등 민감 정보가 포함되지 않도록 마스킹
     * (예: RestClientResponseException 메시지에 URL+token이 포함되는 경우)
     */
    private String maskSensitiveMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.replaceAll("token=[^&\\s]+", "token=***");
    }

    /**
     * BusinessException 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
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
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
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
    public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(
            MissingServletRequestParameterException e) {
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
     * 파라미터 타입 불일치 처리 (쿼리 파라미터, Path Variable 등)
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
     * Request Body 역직렬화 실패 처리
     * 예: Request Body 내부의 잘못된 enum 값, LocalDate format 불일치, JSON 문법 오류
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Request body is not readable: {}", sanitizeForLogging(e.getMessage()));

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_PARAMETER.getCode(),
                "요청 본문 형식이 올바르지 않습니다."
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 지원하지 않는 HTTP Method 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
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
     * RestTemplate 4xx/5xx 응답 시 발생.
     * 예외 메시지에 요청 URL(API 토큰 포함)이 들어갈 수 있으므로 로그에는 마스킹된 메시지만 기록.
     */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestClientResponseException(RestClientResponseException e) {
        String safeMessage = maskSensitiveMessage(e.getMessage());
        log.warn("External API error: status={}, message={}", e.getStatusCode(), safeMessage);

        ApiResponse<Void> response = ApiResponse.error(
                "EXTERNAL_API_ERROR",
                "외부 API 호출 중 오류가 발생했습니다."
        );

        return ResponseEntity
                .status(e.getStatusCode())
                .body(response);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        log.warn("Async request timeout");
        return ResponseEntity.noContent().build();
    }

    /**
     * 그 외 모든 예외 처리
     * 로그 시 예외 메시지에 URL/토큰이 포함될 수 있으므로 마스킹 후 기록.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error occurred: {}", maskSensitiveMessage(e.getMessage()), e);

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
