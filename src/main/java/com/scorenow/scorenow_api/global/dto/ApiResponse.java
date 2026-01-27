package com.scorenow.scorenow_api.global.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T>{
	private final boolean success;
	private final T data;
	private final ErrorDetail error;
	private final LocalDateTime timestamp;

	@Getter
	@Builder
	public static class ErrorDetail{
		private final String code;
		private final String message;
		private final Object details;
	}

	// 성공 응답(데이터 있음)
	public static <T> ApiResponse<T> success(T data){
		return ApiResponse.<T>builder()
			.success(true)
			.data(data)
			.error(null)
			.timestamp(LocalDateTime.now())
			.build();
	}

	// 성공 응답(데이터 없음)
	public static <T> ApiResponse<T> success(){
		return ApiResponse.<T>builder()
			.success(true)
			.data(null)
			.error(null)
			.timestamp(LocalDateTime.now())
			.build();
	}

	// 에러 응답
	public static <T> ApiResponse<T> error(String code, String message){
		return ApiResponse.<T>builder()
			.success(false)
			.data(null)
			.error(ErrorDetail.builder()
				.code(code)
				.message(message)
				.details(null)
				.build())
			.timestamp(LocalDateTime.now())
			.build();
	}

	// 에러응답(상세 정보 포함)
	public static <T> ApiResponse<T> error(String code, String message, Object details){
		return ApiResponse.<T>builder()
			.success(false)
			.data(null)
			.error(ErrorDetail.builder()
				.code(code)
				.message(message)
				.details(details)
				.build())
			.timestamp(LocalDateTime.now())
			.build();
	}
}
