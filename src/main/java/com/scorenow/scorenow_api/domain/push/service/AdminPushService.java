package com.scorenow.scorenow_api.domain.push.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.push.dto.AdminPushHistoryResponse;
import com.scorenow.scorenow_api.domain.push.dto.AdminPushSendRequest;
import com.scorenow.scorenow_api.domain.push.entity.PushSendHistory;
import com.scorenow.scorenow_api.domain.push.enums.PushPlatform;
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

	public String send(
		AdminPushSendRequest request,
		PushPlatform platform
	) {
		validateLanding(request);

		SendResult result = sendToPlatform(request, platform);

		if (!result.success()) {
			throw new BusinessException(
				ErrorCode.PUSH_SEND_FAILED,
				"Firebase 푸시 발송에 실패했습니다.",
				"platform=" + platform
					+ ", cause=" + result.failureReason()
			);
		}

		return result.messageId();
	}

	public List<String> sendCommon(AdminPushSendRequest request) {
		validateLanding(request);

		SendResult aosResult =
			sendToPlatform(request, PushPlatform.AOS);

		SendResult iosResult =
			sendToPlatform(request, PushPlatform.IOS);

		if (!aosResult.success() || !iosResult.success()) {
			throw new BusinessException(
				ErrorCode.PUSH_SEND_FAILED,
				"일부 플랫폼의 Firebase 푸시 발송에 실패했습니다.",
				"AOS=" + aosResult.failureReason()
					+ ", IOS=" + iosResult.failureReason()
			);
		}

		return List.of(
			aosResult.messageId(),
			iosResult.messageId()
		);
	}

	private SendResult sendToPlatform(
		AdminPushSendRequest request,
		PushPlatform platform
	) {
		PushSendHistory history = pushSendHistoryRepository.save(
			PushSendHistory.processing(request, platform)
		);

		Message message = createMessage(request, platform);

		try {
			String messageId = firebaseMessaging.send(message);

			history.markSuccess(messageId);
			pushSendHistoryRepository.save(history);

			return SendResult.success(messageId);

		} catch (FirebaseMessagingException e) {
			history.markFailed(e.getMessage());
			pushSendHistoryRepository.save(history);

			return SendResult.failed(e.getMessage());
		}
	}

	private Message createMessage(
		AdminPushSendRequest request,
		PushPlatform platform
	) {
		Notification.Builder notificationBuilder = Notification.builder()
			.setTitle(request.title())
			.setBody(request.content());

		if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
			notificationBuilder.setImage(request.imageUrl());
		}

		Message.Builder messageBuilder = Message.builder()
			.setTopic(platform.getTopic())
			.setNotification(notificationBuilder.build())
			.putData(
				"landingType",
				request.landingType().name()
			);

		if (request.matchId() != null) {
			messageBuilder.putData(
				"matchId",
				request.matchId().toString()
			);
		}

		return messageBuilder.build();
	}

	public Page<AdminPushHistoryResponse> getHistories(
		PushPlatform platform,
		Pageable pageable
	) {
		return pushSendHistoryRepository
			.findByPlatformOrderByIdDesc(platform, pageable)
			.map(AdminPushHistoryResponse::from);
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

	private record SendResult(
		boolean success,
		String messageId,
		String failureReason
	) {

		private static SendResult success(String messageId) {
			return new SendResult(
				true,
				messageId,
				null
			);
		}

		private static SendResult failed(String failureReason) {
			return new SendResult(
				false,
				null,
				failureReason
			);
		}
	}
}