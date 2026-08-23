package com.scorenow.scorenow_api.domain.community.service;

import org.springframework.stereotype.Service;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

@Service
public class CommunityAuthService {

	public Long requireLogin(Long userId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.COMMUNITY_AUTH_REQUIRED);
		}

		return userId;
	}
}
