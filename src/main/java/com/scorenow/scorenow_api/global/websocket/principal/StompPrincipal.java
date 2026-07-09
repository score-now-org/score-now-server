package com.scorenow.scorenow_api.global.websocket.principal;

import java.security.Principal;

public class StompPrincipal implements Principal {

	private final Long userId;

	public StompPrincipal(Long userId) {
		this.userId = userId;
	}

	@Override
	public String getName() {
		return String.valueOf(userId);
	}

	public Long getUserId() {
		return userId;
	}
}