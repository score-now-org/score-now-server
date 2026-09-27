package com.scorenow.scorenow_api.domain.push.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.push.dto.AdminPushHistoryResponse;
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
	void LIVE_AOS_푸시_발송에_성공한다() throws Exception {
		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"라이브 시작",
			"지금 라이브를 확인하세요.",
			PushLandingType.LIVE,
			null,
			null
		);

		mockHistorySave();

		when(firebaseMessaging.send(any(Message.class)))
			.thenReturn("firebase-message-id");

		// when
		String result = adminPushService.send(
			request,
			PushPlatform.AOS
		);

		// then
		assertThat(result).isEqualTo("firebase-message-id");

		verify(firebaseMessaging).send(any(Message.class));
		verify(pushSendHistoryRepository, times(2))
			.save(any(PushSendHistory.class));
	}

	@Test
	void 푸시_발송시_PROCESSING에서_SUCCESS로_변경된다() throws Exception {
		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"라이브 시작",
			"지금 라이브를 확인하세요.",
			PushLandingType.LIVE,
			null,
			null
		);

		List<PushSendStatus> statuses = new ArrayList<>();
		List<PushPlatform> platforms = new ArrayList<>();

		when(pushSendHistoryRepository.save(any(PushSendHistory.class)))
			.thenAnswer(invocation -> {
				PushSendHistory history = invocation.getArgument(0);

				statuses.add(history.getStatus());
				platforms.add(history.getPlatform());

				return history;
			});

		when(firebaseMessaging.send(any(Message.class)))
			.thenReturn("firebase-message-id");

		// when
		adminPushService.send(
			request,
			PushPlatform.AOS
		);

		// then
		assertThat(statuses).containsExactly(
			PushSendStatus.PROCESSING,
			PushSendStatus.SUCCESS
		);

		assertThat(platforms).containsExactly(
			PushPlatform.AOS,
			PushPlatform.AOS
		);
	}

	@Test
	void LIVE_랜딩에_matchId가_있으면_실패한다() {
		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"제목",
			"내용",
			PushLandingType.LIVE,
			1L,
			null
		);

		// when & then
		assertThatThrownBy(() ->
			adminPushService.send(
				request,
				PushPlatform.AOS
			)
		).isInstanceOf(BusinessException.class);

		verifyNoInteractions(firebaseMessaging);
		verify(pushSendHistoryRepository, never())
			.save(any());
	}

	@Test
	void MATCH_랜딩에_matchId가_없으면_실패한다() {
		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"경기 알림",
			"경기를 확인하세요.",
			PushLandingType.MATCH,
			null,
			null
		);

		// when & then
		assertThatThrownBy(() ->
			adminPushService.send(
				request,
				PushPlatform.IOS
			)
		).isInstanceOf(BusinessException.class);

		verifyNoInteractions(firebaseMessaging);
		verify(pushSendHistoryRepository, never())
			.save(any());
	}

	@Test
	void MATCH_IOS_푸시_발송에_성공한다() throws Exception {
		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"경기 시작",
			"경기를 확인하세요.",
			PushLandingType.MATCH,
			100L,
			"https://example.com/image.jpg"
		);

		mockHistorySave();

		when(matchRepository.existsById(100L))
			.thenReturn(true);

		when(firebaseMessaging.send(any(Message.class)))
			.thenReturn("message-id");

		// when
		String result = adminPushService.send(
			request,
			PushPlatform.IOS
		);

		// then
		assertThat(result).isEqualTo("message-id");

		verify(matchRepository).existsById(100L);
		verify(firebaseMessaging).send(any(Message.class));

		verify(pushSendHistoryRepository, times(2))
			.save(any(PushSendHistory.class));
	}

	@Test
	void MATCH_랜딩의_경기가_존재하지_않으면_실패한다() {
		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"경기 알림",
			"경기를 확인하세요.",
			PushLandingType.MATCH,
			999L,
			null
		);

		when(matchRepository.existsById(999L))
			.thenReturn(false);

		// when & then
		assertThatThrownBy(() ->
			adminPushService.send(
				request,
				PushPlatform.AOS
			)
		).isInstanceOf(BusinessException.class);

		verify(matchRepository).existsById(999L);
		verifyNoInteractions(firebaseMessaging);

		verify(pushSendHistoryRepository, never())
			.save(any());
	}

	@Test
	void Firebase_푸시_발송에_실패하면_FAILED_이력을_저장한다()
		throws Exception {

		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"라이브 알림",
			"라이브를 확인하세요.",
			PushLandingType.LIVE,
			null,
			null
		);

		List<PushSendStatus> statuses = new ArrayList<>();

		when(pushSendHistoryRepository.save(any(PushSendHistory.class)))
			.thenAnswer(invocation -> {
				PushSendHistory history = invocation.getArgument(0);

				statuses.add(history.getStatus());

				return history;
			});

		FirebaseMessagingException exception =
			mock(FirebaseMessagingException.class);

		when(exception.getMessage())
			.thenReturn("Firebase error");

		when(firebaseMessaging.send(any(Message.class)))
			.thenThrow(exception);

		// when & then
		assertThatThrownBy(() ->
			adminPushService.send(
				request,
				PushPlatform.IOS
			)
		).isInstanceOf(BusinessException.class);

		assertThat(statuses).containsExactly(
			PushSendStatus.PROCESSING,
			PushSendStatus.FAILED
		);

		verify(pushSendHistoryRepository, times(2))
			.save(any(PushSendHistory.class));
	}

	@Test
	void 공통_푸시는_AOS와_IOS에_모두_발송한다()
		throws Exception {

		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"공통 알림",
			"모든 기기에 보내는 알림입니다.",
			PushLandingType.LIVE,
			null,
			null
		);

		List<PushPlatform> platforms = new ArrayList<>();
		List<PushSendStatus> statuses = new ArrayList<>();

		when(pushSendHistoryRepository.save(any(PushSendHistory.class)))
			.thenAnswer(invocation -> {
				PushSendHistory history = invocation.getArgument(0);

				platforms.add(history.getPlatform());
				statuses.add(history.getStatus());

				return history;
			});

		when(firebaseMessaging.send(any(Message.class)))
			.thenReturn("aos-message-id")
			.thenReturn("ios-message-id");

		// when
		List<String> result =
			adminPushService.sendCommon(request);

		// then
		assertThat(result).containsExactly(
			"aos-message-id",
			"ios-message-id"
		);

		verify(firebaseMessaging, times(2))
			.send(any(Message.class));

		assertThat(platforms).containsExactly(
			PushPlatform.AOS,
			PushPlatform.AOS,
			PushPlatform.IOS,
			PushPlatform.IOS
		);

		assertThat(statuses).containsExactly(
			PushSendStatus.PROCESSING,
			PushSendStatus.SUCCESS,
			PushSendStatus.PROCESSING,
			PushSendStatus.SUCCESS
		);
	}

	@Test
	void 공통_발송에서_AOS가_실패해도_IOS_발송을_시도한다()
		throws Exception {

		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"공통 알림",
			"모든 기기에 보내는 알림입니다.",
			PushLandingType.LIVE,
			null,
			null
		);

		mockHistorySave();

		FirebaseMessagingException exception =
			mock(FirebaseMessagingException.class);

		when(exception.getMessage())
			.thenReturn("AOS Firebase error");

		when(firebaseMessaging.send(any(Message.class)))
			.thenThrow(exception)
			.thenReturn("ios-message-id");

		// when & then
		assertThatThrownBy(() ->
			adminPushService.sendCommon(request)
		).isInstanceOf(BusinessException.class);

		// AOS 실패 후에도 IOS까지 시도해야 함
		verify(firebaseMessaging, times(2))
			.send(any(Message.class));

		// AOS PROCESSING + FAILED
		// IOS PROCESSING + SUCCESS
		verify(pushSendHistoryRepository, times(4))
			.save(any(PushSendHistory.class));
	}

	@Test
	void AOS_발송_이력을_조회한다() {
		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"AOS 알림",
			"AOS 알림 내용",
			PushLandingType.LIVE,
			null,
			null
		);

		PushSendHistory history =
			PushSendHistory.processing(
				request,
				PushPlatform.AOS
			);

		history.markSuccess("message-id");

		Pageable pageable =
			PageRequest.of(0, 20);

		Page<PushSendHistory> page =
			new PageImpl<>(
				List.of(history),
				pageable,
				1
			);

		when(
			pushSendHistoryRepository
				.findByPlatformOrderByIdDesc(
					PushPlatform.AOS,
					pageable
				)
		).thenReturn(page);

		// when
		Page<AdminPushHistoryResponse> result =
			adminPushService.getHistories(
				PushPlatform.AOS,
				pageable
			);

		// then
		assertThat(result.getContent())
			.hasSize(1);

		assertThat(result.getContent().get(0).content())
			.isEqualTo("AOS 알림 내용");

		assertThat(result.getContent().get(0).status())
			.isEqualTo(PushSendStatus.SUCCESS);

		verify(pushSendHistoryRepository)
			.findByPlatformOrderByIdDesc(
				PushPlatform.AOS,
				pageable
			);
	}

	@Test
	void IOS_발송_이력을_조회한다() {
		// given
		AdminPushSendRequest request = new AdminPushSendRequest(
			"IOS 알림",
			"IOS 알림 내용",
			PushLandingType.LIVE,
			null,
			null
		);

		PushSendHistory history =
			PushSendHistory.processing(
				request,
				PushPlatform.IOS
			);

		history.markSuccess("message-id");

		Pageable pageable =
			PageRequest.of(0, 20);

		Page<PushSendHistory> page =
			new PageImpl<>(
				List.of(history),
				pageable,
				1
			);

		when(
			pushSendHistoryRepository
				.findByPlatformOrderByIdDesc(
					PushPlatform.IOS,
					pageable
				)
		).thenReturn(page);

		// when
		Page<AdminPushHistoryResponse> result =
			adminPushService.getHistories(
				PushPlatform.IOS,
				pageable
			);

		// then
		assertThat(result.getContent())
			.hasSize(1);

		assertThat(result.getContent().get(0).content())
			.isEqualTo("IOS 알림 내용");

		verify(pushSendHistoryRepository)
			.findByPlatformOrderByIdDesc(
				PushPlatform.IOS,
				pageable
			);
	}

	private void mockHistorySave() {
		when(
			pushSendHistoryRepository.save(
				any(PushSendHistory.class)
			)
		).thenAnswer(invocation ->
			invocation.getArgument(0)
		);
	}
}