package com.scorenow.scorenow_api.domain.push.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.push.dto.AdminPushSendRequest;
import com.scorenow.scorenow_api.domain.push.entity.PushSendHistory;
import com.scorenow.scorenow_api.domain.push.enums.PushLandingType;
import com.scorenow.scorenow_api.domain.push.enums.PushPlatform;
import com.scorenow.scorenow_api.domain.push.enums.PushSendStatus;
import com.scorenow.scorenow_api.domain.push.repository.PushSendHistoryRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class AdminPushServiceTest {

	@Mock
	private FirebaseMessaging firebaseMessaging;

	@Mock
	private MatchRepository matchRepository;

	@Mock
	private PushSendHistoryRepository pushSendHistoryRepository;

	@InjectMocks
	private AdminPushService adminPushService;

	@Test
	void LIVE_푸시_발송에_성공한다() throws Exception {
		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			PushPlatform.AOS,
			"라이브 시작",
			"지금 라이브를 확인하세요.",
			PushLandingType.LIVE,
			null,
			null
		);

		when(firebaseMessaging.send(any(Message.class)))
			.thenReturn("firebase-message-id");

		// when
		String result = adminPushService.send(request);

		// then
		assertThat(result).isEqualTo("firebase-message-id");

		ArgumentCaptor<PushSendHistory> captor =
			ArgumentCaptor.forClass(PushSendHistory.class);

		verify(pushSendHistoryRepository).save(captor.capture());

		PushSendHistory history = captor.getValue();

		assertThat(history.getPlatform()).isEqualTo(PushPlatform.AOS);
		assertThat(history.getLandingType()).isEqualTo(PushLandingType.LIVE);
		assertThat(history.getStatus()).isEqualTo(PushSendStatus.SUCCESS);
		assertThat(history.getFirebaseMessageId())
			.isEqualTo("firebase-message-id");
	}

	@Test
	void LIVE_랜딩에_matchId가_있으면_실패한다() {
		AdminPushSendRequest request = new AdminPushSendRequest(
			PushPlatform.AOS,
			"제목",
			"내용",
			PushLandingType.LIVE,
			1L,
			null
		);

		assertThatThrownBy(() -> adminPushService.send(request))
			.isInstanceOf(BusinessException.class);

		verifyNoInteractions(firebaseMessaging);
		verify(pushSendHistoryRepository, never()).save(any());
	}

	@Test
	void MATCH_랜딩에_matchId가_없으면_실패한다() {
		AdminPushSendRequest request = new AdminPushSendRequest(
			PushPlatform.IOS,
			"경기 알림",
			"경기를 확인하세요.",
			PushLandingType.MATCH,
			null,
			null
		);

		assertThatThrownBy(() -> adminPushService.send(request))
			.isInstanceOf(BusinessException.class);

		verifyNoInteractions(firebaseMessaging);
		verify(pushSendHistoryRepository, never()).save(any());
	}

	@Test
	void MATCH_푸시_발송에_성공한다() throws Exception {
		AdminPushSendRequest request = new AdminPushSendRequest(
			PushPlatform.IOS,
			"경기 시작",
			"경기를 확인하세요.",
			PushLandingType.MATCH,
			100L,
			"https://example.com/image.jpg"
		);

		when(matchRepository.existsById(100L)).thenReturn(true);
		when(firebaseMessaging.send(any(Message.class)))
			.thenReturn("message-id");

		String result = adminPushService.send(request);

		assertThat(result).isEqualTo("message-id");

		verify(matchRepository).existsById(100L);
		verify(firebaseMessaging).send(any(Message.class));

		ArgumentCaptor<PushSendHistory> captor =
			ArgumentCaptor.forClass(PushSendHistory.class);

		verify(pushSendHistoryRepository).save(captor.capture());

		PushSendHistory history = captor.getValue();

		assertThat(history.getPlatform()).isEqualTo(PushPlatform.IOS);
		assertThat(history.getLandingType()).isEqualTo(PushLandingType.MATCH);
		assertThat(history.getMatchId()).isEqualTo(100L);
		assertThat(history.getImageUrl())
			.isEqualTo("https://example.com/image.jpg");
		assertThat(history.getStatus()).isEqualTo(PushSendStatus.SUCCESS);
		assertThat(history.getFirebaseMessageId()).isEqualTo("message-id");
	}

	@Test
	void MATCH_랜딩의_경기가_존재하지_않으면_실패한다() {
		AdminPushSendRequest request = new AdminPushSendRequest(
			PushPlatform.AOS,
			"경기 알림",
			"경기를 확인하세요.",
			PushLandingType.MATCH,
			999L,
			null
		);

		when(matchRepository.existsById(999L)).thenReturn(false);

		assertThatThrownBy(() -> adminPushService.send(request))
			.isInstanceOf(BusinessException.class);

		verify(matchRepository).existsById(999L);
		verifyNoInteractions(firebaseMessaging);
		verify(pushSendHistoryRepository, never()).save(any());
	}

	@Test
	void Firebase_푸시_발송에_실패하면_FAILED_이력을_저장한다() throws Exception {
		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			PushPlatform.IOS,
			"라이브 알림",
			"라이브를 확인하세요.",
			PushLandingType.LIVE,
			null,
			null
		);

		FirebaseMessagingException exception =
			mock(FirebaseMessagingException.class);

		when(exception.getMessage()).thenReturn("Firebase error");

		when(firebaseMessaging.send(any(Message.class)))
			.thenThrow(exception);

		// when & then
		assertThatThrownBy(() -> adminPushService.send(request))
			.isInstanceOf(BusinessException.class);

		ArgumentCaptor<PushSendHistory> captor =
			ArgumentCaptor.forClass(PushSendHistory.class);

		verify(pushSendHistoryRepository).save(captor.capture());

		PushSendHistory history = captor.getValue();

		assertThat(history.getStatus()).isEqualTo(PushSendStatus.FAILED);
		assertThat(history.getFailureReason()).isEqualTo("Firebase error");
		assertThat(history.getFirebaseMessageId()).isNull();
	}

}