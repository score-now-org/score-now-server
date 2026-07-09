package com.scorenow.scorenow_api.global.websocket.interceptor;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.websocket.principal.StompPrincipal;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StompJwtAuthenticator {

	private final JwtProvider jwtProvider;

	public void authenticateIfTokenExists(StompHeaderAccessor accessor) {
		String authorization = accessor.getFirstNativeHeader("Authorization");

		if (authorization == null || authorization.isBlank()) {
			return;
		}

		if (!authorization.startsWith("Bearer ")) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER);
		}

		String token = authorization.substring(7);

		if (!jwtProvider.validateToken(token)) {
			throw new BusinessException(ErrorCode.TOKEN_INVALID);
		}

		if (!jwtProvider.isAccessToken(token)) {
			throw new BusinessException(ErrorCode.ACCESS_TOKEN_REQUIRED);
		}

		Long userId = jwtProvider.getUserId(token);

		accessor.setUser(new StompPrincipal(userId));
	}
}
