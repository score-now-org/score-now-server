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

	// MatchLineup
	MATCH_LINEUP_NOT_FOUND(HttpStatus.NOT_FOUND, "ML001", "라인업을 찾을 수 없습니다."),
	MATCH_LINEUP_PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "ML002", "라인업에서 선수를 찾을 수 없습니다."),
	MATCH_LINEUP_PLAYER_ALREADY_EXISTS(HttpStatus.CONFLICT, "ML004", "이미 라인업에 존재하는 선수입니다."),
	MATCH_LINEUP_INVALID_MATCH_SPORT(HttpStatus.BAD_REQUEST, "ML003", "matchId/sportId가 일치하지 않습니다."),

	// League
	LEAGUE_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "리그를 찾을 수 없습니다."),
	LEAGUE_ALREADY_EXISTS(HttpStatus.CONFLICT, "L002", "이미 존재하는 리그입니다."),

	// Team
	TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "팀을 찾을 수 없습니다."),

	// Sport
	SPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "스포츠를 찾을 수 없습니다."),

	// Stadium
	STADIUM_NOT_FOUND(HttpStatus.NOT_FOUND, "STA001", "경기장을 찾을 수 없습니다."),

	// User
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
	NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "U002", "이미 사용중인 닉네임입니다."),
	REJOIN_RESTRICTED(HttpStatus.CONFLICT, "U003", "재가입이 제한된 계정입니다."),
	NICKNAME_RESTRICTED(HttpStatus.CONFLICT, "U004", "60일 이내 닉네임 재설정 불가합니다."),

	// Token
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A001", "만료된 토큰입니다."),
	TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
	TOKEN_TYPE_MISMATCH(HttpStatus.UNAUTHORIZED, "A003", "RefreshToken이 아닙니다."),
	ACCESS_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "A004", "AccessToken이 필요합니다."),

	// Chat
	CHAT_SEND_AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "CH001", "채팅 전송에는 로그인이 필요합니다."),
	CHAT_MESSAGE_INVALID(HttpStatus.BAD_REQUEST, "CH002", "채팅 메시지가 올바르지 않습니다."),
	CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CH003", "채팅방을 찾을 수 없습니다."),
	CHAT_MESSAGE_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CH004", "채팅 메시지는 1000자를 초과할 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
