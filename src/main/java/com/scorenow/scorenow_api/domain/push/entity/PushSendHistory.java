package com.scorenow.scorenow_api.domain.push.entity;

import java.time.LocalDateTime;

import com.scorenow.scorenow_api.domain.push.dto.AdminPushSendRequest;
import com.scorenow.scorenow_api.domain.push.enums.PushLandingType;
import com.scorenow.scorenow_api.domain.push.enums.PushPlatform;
import com.scorenow.scorenow_api.domain.push.enums.PushSendStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSendHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private PushPlatform platform;

	private String title;

	private String content;

	@Enumerated(EnumType.STRING)
	private PushLandingType landingType;

	private Long matchId;

	private String imageUrl;

	@Enumerated(EnumType.STRING)
	private PushSendStatus status;

	private String firebaseMessageId;

	private String failureReason;

	private LocalDateTime sentAt;

	public static PushSendHistory success(
		AdminPushSendRequest request,
		String firebaseMessageId
	) {
		PushSendHistory history = new PushSendHistory();

		history.platform = request.platform();
		history.title = request.title();
		history.content = request.content();
		history.landingType = request.landingType();
		history.matchId = request.matchId();
		history.imageUrl = request.imageUrl();
		history.status = PushSendStatus.SUCCESS;
		history.firebaseMessageId = firebaseMessageId;
		history.sentAt = LocalDateTime.now();

		return history;
	}

	public static PushSendHistory failed(
		AdminPushSendRequest request,
		String failureReason
	) {
		PushSendHistory history = new PushSendHistory();

		history.platform = request.platform();
		history.title = request.title();
		history.content = request.content();
		history.landingType = request.landingType();
		history.matchId = request.matchId();
		history.imageUrl = request.imageUrl();
		history.status = PushSendStatus.FAILED;
		history.failureReason = failureReason;
		history.sentAt = LocalDateTime.now();

		return history;
	}

}
