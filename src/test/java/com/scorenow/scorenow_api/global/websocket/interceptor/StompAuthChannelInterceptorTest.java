package com.scorenow.scorenow_api.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.global.websocket.principal.StompPrincipal;

class StompAuthChannelInterceptorTest {

	private final JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);
	private final StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(
		new StompJwtAuthenticator(jwtProvider)
	);

	@Test
	void connect_stores_authenticated_user_on_original_message_accessor() {
		String token = "access-token";
		Message<byte[]> message = stompMessage(StompCommand.CONNECT, null, "Bearer " + token);
		Mockito.when(jwtProvider.validateToken(token)).thenReturn(true);
		Mockito.when(jwtProvider.isAccessToken(token)).thenReturn(true);
		Mockito.when(jwtProvider.getUserId(token)).thenReturn(10L);

		Message<?> result = interceptor.preSend(message, null);

		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
		assertThat(accessor.getUser()).isInstanceOf(StompPrincipal.class);
		assertThat(accessor.getUser().getName()).isEqualTo("10");
	}

	@Test
	void subscribe_allows_personal_error_queue() {
		Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/user/queue/errors", null);

		assertThatCode(() -> interceptor.preSend(message, null)).doesNotThrowAnyException();
	}

	private Message<byte[]> stompMessage(StompCommand command, String destination, String authorization) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
		if (destination != null) {
			accessor.setDestination(destination);
		}
		if (authorization != null) {
			accessor.setNativeHeader("Authorization", authorization);
		}
		accessor.setLeaveMutable(true);

		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}
}
