package com.scorenow.scorenow_api.domain.chat.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

class ChatMessageTest {

	@Test
	void create_trims_message_and_snapshots_sender_nickname() {
		Match match = Match.builder().id(1L).build();
		User sender = User.builder().id(10L).nickname("score-user").build();

		ChatMessage chatMessage = ChatMessage.create(match, sender, "  hello score now  ");

		assertThat(chatMessage.getMatchId()).isEqualTo(1L);
		assertThat(chatMessage.getUserId()).isEqualTo(10L);
		assertThat(chatMessage.getSenderNickname()).isEqualTo("score-user");
		assertThat(chatMessage.getMessage()).isEqualTo("hello score now");
	}

	@Test
	void create_fails_when_match_is_null() {
		User sender = User.builder().id(10L).nickname("score-user").build();

		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> ChatMessage.create(null, sender, "message"))
			.satisfies(exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MATCH_NOT_FOUND));
	}

	@Test
	void create_fails_when_sender_is_null() {
		Match match = Match.builder().id(1L).build();

		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> ChatMessage.create(match, null, "message"))
			.satisfies(exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
	}

	@Test
	void create_fails_when_message_is_blank() {
		Match match = Match.builder().id(1L).build();
		User sender = User.builder().id(10L).nickname("score-user").build();

		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> ChatMessage.create(match, sender, "   "))
			.satisfies(exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_MESSAGE_INVALID));
	}

	@Test
	void create_fails_when_message_length_exceeds_limit_after_trim() {
		Match match = Match.builder().id(1L).build();
		User sender = User.builder().id(10L).nickname("score-user").build();
		String overLimitMessage = " " + "a".repeat(1001) + " ";

		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> ChatMessage.create(match, sender, overLimitMessage))
			.satisfies(exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_MESSAGE_LENGTH_EXCEEDED));
	}
}
