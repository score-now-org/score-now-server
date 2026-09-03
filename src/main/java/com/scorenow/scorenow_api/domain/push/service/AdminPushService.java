package com.scorenow.scorenow_api.domain.push.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.push.dto.AdminPushSendRequest;
import com.scorenow.scorenow_api.domain.push.entity.PushSendHistory;
import com.scorenow.scorenow_api.domain.push.repository.PushSendHistoryRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
	name = "firebase.enabled",
	havingValue = "true"
)
public class AdminPushService {

	private final FirebaseMessaging firebaseMessaging;
	private final MatchRepository matchRepository;
	private final PushSendHistoryRepository pushSendHistoryRepository;

	public String send(AdminPushSendRequest request) {
		validateLanding(request);

		Message message = createMessage(request);

		try {
			String messageId = firebaseMessaging.send(message);

			pushSendHistoryRepository.save(
				PushSendHistory.success(request, messageId)
			);

			return messageId;

		} catch (FirebaseMessagingException e) {
			pushSendHistoryRepository.save(
				PushSendHistory.failed(request, e.getMessage())
			);

			throw new BusinessException(
				ErrorCode.PUSH_SEND_FAILED,
				"Firebase 푸시 발송에 실패했습니다.",
				"platform=" + request.platform() + ", cause=" + e.getMessage()
			);
		}
	}

	private Message createMessage(AdminPushSendRequest request) {
		Notification.Builder notificationBuilder = Notification.builder()
			.setTitle(request.title())
			.setBody(request.content());

		if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
			notificationBuilder.setImage(request.imageUrl());
		}

		Message.Builder messageBuilder = Message.builder()
			.setTopic(request.platform().getTopic())
			.setNotification(notificationBuilder.build())
			.putData("landingType", request.landingType().name());

		if (request.matchId() != null) {
			messageBuilder.putData("matchId", request.matchId().toString());
		}

		return messageBuilder.build();
	}

	private void validateLanding(AdminPushSendRequest request) {
		switch (request.landingType()) {
			case LIVE -> validateLiveLanding(request.matchId());
			case MATCH -> validateMatchLanding(request.matchId());
		}
	}

	private void validateLiveLanding(Long matchId) {
		if (matchId != null) {
			throw new BusinessException(
				ErrorCode.PUSH_INVALID_LANDING,
				"LIVE 랜딩에는 경기 ID를 입력할 수 없습니다.",
				"matchId=" + matchId
			);
		}
	}

	private void validateMatchLanding(Long matchId) {
		if (matchId == null) {
			throw new BusinessException(
				ErrorCode.PUSH_INVALID_LANDING,
				"경기 랜딩에는 경기 ID가 필요합니다."
			);
		}

		if (!matchRepository.existsById(matchId)) {
			throw new BusinessException(
				ErrorCode.MATCH_NOT_FOUND,
				"경기가 존재하지 않습니다.",
				"matchId=" + matchId
			);
		}
	}
}