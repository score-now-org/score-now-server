package com.scorenow.scorenow_api.global.websocket.principal;

import java.security.Principal;

public class StompPrincipal implements Principal {

	private final Long userId;
	private final long tokenVersion;

	public StompPrincipal(Long userId) {
		this(userId, 0L);
	}

	public StompPrincipal(Long userId, long tokenVersion) {
		this.userId = userId;
		this.tokenVersion = tokenVersion;
	}

	public long getTokenVersion() { return tokenVersion; }

	@Override
	public String getName() {
		return String.valueOf(userId);
	}

	public Long getUserId() {
		return userId;
	}
}