package com.scorenow.scorenow_api.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import com.scorenow.scorenow_api.domain.admin.forbiddenword.ForbiddenWordFilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageRequest;
import com.scorenow.scorenow_api.domain.community.repository.CommunityUploadedImageRepository;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import java.util.List;
import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageResponse;
import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageRepository;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.service.MatchReferenceService;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

	@Mock
	private MatchReferenceService matchReferenceService;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private UserReferenceService userReferenceService;

	@Mock private CommunityUploadedImageRepository uploadedImages;
	@Mock private FileStorage fileStorage;
	@Mock private ForbiddenWordFilter forbiddenWords;

	@InjectMocks
	private ChatMessageService chatMessageService;

	@BeforeEach void preserveTextByDefault() {
		lenient().when(forbiddenWords.apply(any(), anyInt())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void createMessage_saves_chat_message_and_returns_response() {
		Long matchId = 1L;
		Long userId = 10L;
		Match match = Match.builder().id(matchId).build();
		User user = User.builder()
			.id(userId)
			.nickname("score-user")
			.profileImageUrl("https://cdn.score-now.com/profiles/10.png")
			.build();
		LocalDateTime createdAt = LocalDateTime.of(2026, 6, 2, 20, 15);

		given(matchReferenceService.requireMatch(matchId)).willReturn(match);
		given(userReferenceService.requireActiveUser(userId)).willReturn(user);
		given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
			ChatMessage savedMessage = invocation.getArgument(0);
			ReflectionTestUtils.setField(savedMessage, "id", 100L);
			ReflectionTestUtils.setField(savedMessage, "createdAt", createdAt);
			return savedMessage;
		});

		ChatMessageResponse response = chatMessageService.createMessage(matchId, userId, request("  hello score now  "));

		ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
		then(chatMessageRepository).should().save(captor.capture());
		ChatMessage savedMessage = captor.getValue();

		assertThat(savedMessage.getMatchId()).isEqualTo(matchId);
		assertThat(savedMessage.getUserId()).isEqualTo(userId);
		assertThat(savedMessage.getSenderNickname()).isEqualTo("score-user");
		assertThat(savedMessage.getMessage()).isEqualTo("hello score now");
		assertThat(response.getChatId()).isEqualTo(100L);
		assertThat(response.getMatchId()).isEqualTo(matchId);
		assertThat(response.getSenderNickname()).isEqualTo("score-user");
		assertThat(response.getProfileImageUrl()).isEqualTo("https://cdn.score-now.com/profiles/10.png");
		assertThat(response.getMessage()).isEqualTo("hello score now");
		assertThat(response.getCreatedAt()).isEqualTo(createdAt);
	}

	@Test
	void createMessage_saves_and_broadcasts_filtered_text() {
		given(matchReferenceService.requireMatch(1L)).willReturn(Match.builder().id(1L).build());
		given(userReferenceService.requireActiveUser(10L)).willReturn(User.builder().id(10L).nickname("fan").build());
		given(forbiddenWords.apply("금칙어", 1000)).willReturn("???");
		given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));
		assertThat(chatMessageService.createMessage(1L, 10L, request("금칙어")).getMessage()).isEqualTo("???");
	}

	@Test
	void createMessage_does_not_save_blank_message() {
		Long matchId = 1L;
		Long userId = 10L;
		Match match = Match.builder().id(matchId).build();
		User user = User.builder().id(userId).nickname("score-user").build();

		given(matchReferenceService.requireMatch(matchId)).willReturn(match);
		given(userReferenceService.requireActiveUser(userId)).willReturn(user);

		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> chatMessageService.createMessage(matchId, userId, request("  ")))
			.satisfies(exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_MESSAGE_INVALID));
		then(chatMessageRepository).should(never()).save(any(ChatMessage.class));
	}

	@Test
	void imageRequiresOwnedUploadAndCanBeSentWithoutText() {
		Long matchId = 1L, userId = 10L;
		given(matchReferenceService.requireMatch(matchId)).willReturn(Match.builder().id(matchId).build());
		given(userReferenceService.requireActiveUser(userId)).willReturn(User.builder().id(userId).nickname("fan").build());
		ChatMessageRequest request = request(null);
		ReflectionTestUtils.setField(request, "imageKey", "key");
		assertThatThrownBy(() -> chatMessageService.createMessage(matchId, userId, request)).isInstanceOf(BusinessException.class);
		given(uploadedImages.countByImageKeyInAndOwner_Id(List.of("key"), userId)).willReturn(1L);
		given(fileStorage.getFileUrl("key")).willReturn("https://example.test/image.png");
		given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));
		assertThat(chatMessageService.createMessage(matchId, userId, request).getImageUrl()).isEqualTo("https://example.test/image.png");
	}

	private ChatMessageRequest request(String message) {
		ChatMessageRequest request = new ChatMessageRequest();
		ReflectionTestUtils.setField(request, "message", message);
		return request;
	}
}
