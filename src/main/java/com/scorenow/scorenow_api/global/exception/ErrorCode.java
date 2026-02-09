package com.scorenow.scorenow_api.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// Common
	INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 리소스를 찾을 수 없습니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "지원하지 않는 HTTP 메서드입니다"),

	// Match
	MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "경기를 찾을 수 없습니다."),
	MATCH_ALREADY_EXIST(HttpStatus.CONFLICT, "M002", "이미 존재하는 경기입니다."),
	MATCH_INVALID_STATUS(HttpStatus.BAD_REQUEST, "M003", "잘못된 경기 상태입니다."),
	MATCH_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "M004", "삭제할 수 없는 경기입니다."),

	// League
	LEAGUE_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "리그를 찾을 수 없습니다."),

	// Team
	TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "팀을 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
