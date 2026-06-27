package com.scorenow.scorenow_api.global.websocket.interceptor;

import java.security.Principal;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private static final Pattern CHAT_SEND_DESTINATION = Pattern.compile("^/pub/matches/\\d+/chat/messages$");
	private final StompJwtAuthenticator stompJwtAuthenticator;

	private static final Pattern CHAT_SUBSCRIBE_DESTINATION = Pattern.compile("^/sub/matches/\\d+/chat/messages$");

	public StompAuthChannelInterceptor(StompJwtAuthenticator stompJwtAuthenticator) {
		this.stompJwtAuthenticator = stompJwtAuthenticator;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		StompCommand command = accessor.getCommand();

		if (StompCommand.CONNECT.equals(command)) {
			stompJwtAuthenticator.authenticateIfTokenExists(accessor);
		} else if (StompCommand.SEND.equals(command)) {
			validateDestination(accessor.getDestination(),CHAT_SEND_DESTINATION);
			validateAuthenticatedUser(accessor.getUser());
		} else if (StompCommand.SUBSCRIBE.equals(command)) {
			validateDestination(accessor.getDestination(),CHAT_SUBSCRIBE_DESTINATION);
		}

		return message;
	}

	private void validateDestination(String destination, Pattern allowedPattern) {
		if (destination == null || !allowedPattern.matcher(destination).matches()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER);
		}
	}

	private void validateAuthenticatedUser(Principal user) {
		if (user == null) {
			throw new BusinessException(ErrorCode.CHAT_SEND_AUTH_REQUIRED);
		}
	}
}