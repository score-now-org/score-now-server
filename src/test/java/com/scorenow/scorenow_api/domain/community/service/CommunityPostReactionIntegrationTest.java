package com.scorenow.scorenow_api.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorenow.scorenow_api.domain.community.controller.CommunityPostController;
import com.scorenow.scorenow_api.domain.community.dto.request.CommunityReactionRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityPostListResponse;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityReactionResponse;
import com.scorenow.scorenow_api.domain.community.entity.CommunityBoardType;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCategory;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostReaction;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostSort;
import com.scorenow.scorenow_api.domain.community.entity.CommunityReactionType;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostReactionRepository;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.config.QueryDslConfig;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import({QueryDslConfig.class, CommunityReactionService.class, CommunityPostQueryService.class,
	CommunityReferenceService.class, CommunityAuthService.class, UserReferenceService.class})
class CommunityPostReactionIntegrationTest {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private CommunityReactionService reactionService;

	@Autowired
	private CommunityPostQueryService queryService;

	@Autowired
	private CommunityPostReactionRepository reactionRepository;

	@MockBean(name = "mongoMappingContext")
	private MongoMappingContext mongoMappingContext;

	@MockBean
	private CommunityImageService imageService;

	@MockBean
	private CommunityPostCommandService commandService;

	private Long userId;
	private Long postId;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		User user = persistUser("author");
		CommunityPost post = CommunityPost.create(user, CommunityCategory.SOCCER, "title", "content");
		entityManager.persist(post);
		entityManager.flush();
		userId = user.getId();
		postId = post.getId();
		entityManager.clear();
		mockMvc = MockMvcBuilders.standaloneSetup(new CommunityPostController(commandService, queryService))
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.build();
	}

	@AfterEach
	void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	@ParameterizedTest
	@EnumSource(CommunityReactionType.class)
	void repeatedReactionCancelsAndCanBeSelectedAgain(CommunityReactionType type) {
		assertReaction(type, type);
		assertReaction(type, null);
		assertReaction(type, type);
	}

	@ParameterizedTest
	@EnumSource(CommunityReactionType.class)
	void differentReactionSwitchesThePersistedTypeAndCounts(CommunityReactionType type) {
		assertReaction(type, type);
		CommunityReactionType opposite = type == CommunityReactionType.LIKE
			? CommunityReactionType.DISLIKE : CommunityReactionType.LIKE;
		assertReaction(opposite, opposite);
		assertReaction(opposite, null);
	}

	@ParameterizedTest
	@EnumSource(CommunityReactionType.class)
	void detailEndpointReturnsTheAuthenticatedUsersReaction(CommunityReactionType type) throws Exception {
		assertReaction(type, type);
		authenticate(userId);
		mockMvc.perform(get("/api/v1/community/posts/{postId}", postId).accept(APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.currentReaction").value(type.name()));
	}

	@Test
	void detailEndpointReturnsExplicitNullForAnonymousAndUnreactedUsers() throws Exception {
		assertReaction(CommunityReactionType.LIKE, CommunityReactionType.LIKE);
		String anonymousJson = mockMvc.perform(get("/api/v1/community/posts/{postId}", postId).accept(APPLICATION_JSON))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(new ObjectMapper().readTree(anonymousJson).get("data").get("currentReaction").isNull()).isTrue();

		authenticate(persistUser("viewer").getId());
		String unreactedJson = mockMvc.perform(get("/api/v1/community/posts/{postId}", postId).accept(APPLICATION_JSON))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(new ObjectMapper().readTree(unreactedJson).get("data").get("currentReaction").isNull()).isTrue();
	}

	@Test
	void detailUsesTheViewersReactionInsteadOfTheAuthorsReaction() {
		assertReaction(CommunityReactionType.LIKE, CommunityReactionType.LIKE);
		Long viewerId = persistUser("viewer").getId();
		reactionService.react(postId, viewerId, request(CommunityReactionType.DISLIKE));
		entityManager.flush();
		entityManager.clear();

		assertThat(queryService.getPostDetail(postId, viewerId).getCurrentReaction())
			.isEqualTo(CommunityReactionType.DISLIKE);
		assertThat(queryService.getPostDetail(postId, userId).getCurrentReaction())
			.isEqualTo(CommunityReactionType.LIKE);
	}

	@Test
	void allPostListsExposeLikeCount() throws Exception {
		assertReaction(CommunityReactionType.LIKE, CommunityReactionType.LIKE);
		List<List<CommunityPostListResponse>> lists = List.of(
			queryService.getPosts(CommunityBoardType.ALL, CommunityPostSort.LATEST, null, null, 20).getPosts(),
			queryService.getPosts(CommunityBoardType.SOCCER, CommunityPostSort.RECOMMENDED, null, null, 20).getPosts(),
			queryService.getPosts(CommunityBoardType.POPULAR, CommunityPostSort.RECOMMENDED, null, null, 20).getPosts(),
			queryService.getMyPosts(userId, null, 20).getPosts(),
			queryService.getPopularRollingPosts(5),
			queryService.getPopularTopPosts(3)
		);
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		for (List<CommunityPostListResponse> posts : lists) {
			assertThat(posts).hasSize(1);
			assertThat(posts.get(0).getId()).isEqualTo(postId);
			assertThat(objectMapper.readTree(objectMapper.writeValueAsString(posts)).get(0).get("likeCount").asLong())
				.isEqualTo(1L);
		}
	}

	@Test
	void reactionRequiresAuthentication() {
		assertThatThrownBy(() -> reactionService.react(postId, null, request(CommunityReactionType.LIKE)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMUNITY_AUTH_REQUIRED));
		assertThat(reactionRepository.count()).isZero();
	}

	private void assertReaction(CommunityReactionType requested, CommunityReactionType expected) {
		CommunityReactionResponse response = reactionService.react(postId, userId, request(requested));
		long likes = expected == CommunityReactionType.LIKE ? 1L : 0L;
		long dislikes = expected == CommunityReactionType.DISLIKE ? 1L : 0L;
		assertThat(response.getCurrentReaction()).isEqualTo(expected);
		assertThat(response.getLikeCount()).isEqualTo(likes);
		assertThat(response.getDislikeCount()).isEqualTo(dislikes);
		entityManager.flush();
		entityManager.clear();
		CommunityPost post = entityManager.find(CommunityPost.class, postId);
		assertThat(post.getLikeCount()).isEqualTo(likes);
		assertThat(post.getDislikeCount()).isEqualTo(dislikes);
		assertThat(reactionRepository.findByPost_IdAndUser_Id(postId, userId)
			.map(CommunityPostReaction::getType).orElse(null)).isEqualTo(expected);
	}

	private User persistUser(String socialId) {
		User user = User.builder().provider("test").socialId(socialId).nickname(socialId)
			.status(UserStatus.ACTIVE).build();
		entityManager.persist(user);
		entityManager.flush();
		return user;
	}

	private CommunityReactionRequest request(CommunityReactionType type) {
		CommunityReactionRequest request = new CommunityReactionRequest();
		ReflectionTestUtils.setField(request, "type", type);
		return request;
	}

	private void authenticate(Long id) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(id, null, List.of()));
	}
}
