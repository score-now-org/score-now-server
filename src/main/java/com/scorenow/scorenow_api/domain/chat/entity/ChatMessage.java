package com.scorenow.scorenow_api.domain.chat.entity;

import java.time.LocalDateTime;
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

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "user_id", nullable = true, foreignKey = @ForeignKey(name = "fk_chat_messages_user"))
	@OnDelete(action = OnDeleteAction.SET_NULL)
	private User user;

	@Column(name = "sender_nickname", nullable = false)
	private String senderNickname;

	@Column(nullable = false, length = MAX_MESSAGE_LENGTH)
	private String message;

	@Column(length = 1000)
	private String imageUrl;
	private LocalDateTime deletedAt;
	@Column(length = 500)
	private String deletionReason;
	private LocalDateTime imageRemovedAt;
	@Column(length = 500)
	private String imageDeletionReason;
	private Long reportCount;

	public void removeImage(String reason) {
		if (imageUrl == null || imageUrl.isBlank() || deletedAt != null)
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "삭제할 채팅 사진이 없습니다.");
		imageUrl = null;
		imageRemovedAt = LocalDateTime.now();
		imageDeletionReason = reason;
		if (message == null || message.isBlank()) markDeleted(reason);
	}

	public void markReported() { reportCount = (reportCount == null ? 0 : reportCount) + 1; }
	public void markDeleted(String reason) {
		if (deletedAt != null) throw new BusinessException(ErrorCode.INVALID_PARAMETER, "이미 삭제된 채팅입니다.");
		deletedAt = LocalDateTime.now();
		deletionReason = reason;
	}

	public Long getMatchId() {
		return match.getId();
	}

	public Long getUserId() {
		return user != null ? user.getId() : null;
	}

	public static ChatMessage create(Match match, User sender, String message) {
		return create(match, sender, message, null);
	}

	public static ChatMessage create(Match match, User sender, String message, String imageUrl) {
		validateMatch(match);
		validateSender(sender);
		validateMessage(message, imageUrl);

		ChatMessage chatMessage = new ChatMessage();
		chatMessage.match = match;
		chatMessage.user = sender;
		chatMessage.senderNickname = sender.getNickname();
		chatMessage.message = message == null ? "" : message.trim();
		chatMessage.imageUrl = imageUrl;

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

	private static void validateMessage(String message, String imageUrl) {
		if ((message == null || message.isBlank()) && (imageUrl == null || imageUrl.isBlank())) {
			throw new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID);
		}

		if (message != null && message.trim().length() > MAX_MESSAGE_LENGTH) {
			throw new BusinessException(ErrorCode.CHAT_MESSAGE_LENGTH_EXCEEDED);
		}
	}
}
