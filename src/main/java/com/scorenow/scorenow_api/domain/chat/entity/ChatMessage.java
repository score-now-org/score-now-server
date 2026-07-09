package com.scorenow.scorenow_api.domain.chat.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_messages", indexes = {@Index(name = "idx_chat_messages_match_id_id", columnList = "match_id, id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

	private static final int MAX_MESSAGE_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "match_id", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_messages_match"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Match match;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_messages_user"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	@Column(name = "sender_nickname", nullable = false)
	private String senderNickname;

	@Column(nullable = false, length = MAX_MESSAGE_LENGTH)
	private String message;

	public Long getMatchId() {
		return match.getId();
	}

	public Long getUserId() {
		return user.getId();
	}

	public static ChatMessage create(Match match, User sender, String message) {
		validateMatch(match);
		validateSender(sender);
		validateMessage(message);

		ChatMessage chatMessage = new ChatMessage();
		chatMessage.match = match;
		chatMessage.user = sender;
		chatMessage.senderNickname = sender.getNickname();
		chatMessage.message = message.trim();

		return chatMessage;
	}

	private static void validateMatch(Match match) {
		if (match == null) {
			throw new BusinessException(ErrorCode.MATCH_NOT_FOUND);
		}
	}

	private static void validateSender(User sender) {
		if (sender == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
	}

	private static void validateMessage(String message) {
		if (message == null || message.isBlank()) {
			throw new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID);
		}

		if (message.trim().length() > MAX_MESSAGE_LENGTH) {
			throw new BusinessException(ErrorCode.CHAT_MESSAGE_LENGTH_EXCEEDED);
		}
	}
}