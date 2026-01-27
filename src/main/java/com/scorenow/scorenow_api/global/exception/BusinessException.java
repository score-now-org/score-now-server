package com.scorenow.scorenow_api.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{

	private final ErrorCode errorCode;
	private final Object details;

	public BusinessException(ErrorCode errorCode){
		this(errorCode, errorCode.getMessage(), null);
	}

	public BusinessException(ErrorCode errorCode, String message){
		this(errorCode, message, null);
	}

	public BusinessException(ErrorCode errorCode, Object details){
		this(errorCode, errorCode.getMessage(), details);
	}

	public BusinessException(ErrorCode errorCode, String message, Object details){
		super(message);
		this.errorCode = errorCode;
		this.details = details;
	}
}
