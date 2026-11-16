package com.scorenow.scorenow_api.domain.community.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.scorenow.scorenow_api.domain.community.dto.response.CommunityCommentSliceResponse;
import com.scorenow.scorenow_api.domain.community.entity.*;
import com.scorenow.scorenow_api.domain.community.repository.CommunityCommentRepository;
import com.scorenow.scorenow_api.domain.user.controller.AuthController;
import com.scorenow.scorenow_api.domain.user.dto.RegisterNicknameRequestDto;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.NicknameGenerator;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.jwt.TokenService;
import com.scorenow.scorenow_api.domain.user.service.*;
import com.scorenow.scorenow_api.external.social.SocialLoginVerifier;
import com.scorenow.scorenow_api.global.config.QueryDslConfig;
import com.scorenow.scorenow_api.global.exception.*;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import({QueryDslConfig.class, AuthService.class, UserReferenceService.class, CommunityPostQueryService.class,
    CommunityCommentService.class, CommunityReferenceService.class, CommunityAuthService.class})
class CommunityNicknameIntegrationTest {
    @Autowired EntityManager em;
    @Autowired AuthService authService;
    @Autowired CommunityPostQueryService posts;
    @Autowired CommunityCommentService comments;
    @Autowired CommunityCommentRepository commentRepository;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongoMappingContext;
    @MockBean GeoService geoService;
    @MockBean TokenService tokenService;
    @MockBean SocialLoginVerifier socialLoginVerifier;
    @MockBean NicknameGenerator nicknameGenerator;
    @MockBean CommunityImageService imageService;

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void changedNicknameAppearsOnExistingPostsAndCommentsAcrossLists() {
        User author = user("author", "이전닉네임");
        User other = user("other", "다른회원");
        CommunityPost post = CommunityPost.create(author, CommunityCategory.SOCCER, "title", "content");
        em.persist(post);
        CommunityComment first = CommunityComment.create(post, author, null, "first");
        CommunityComment second = CommunityComment.create(post, other, null, "second");
        em.persist(first); em.persist(second); em.flush();
        Long authorId = author.getId(), postId = post.getId(), firstId = first.getId();
        em.clear();

        authService.registerNickname(authorId, new RegisterNicknameRequestDto(null, null, "바뀐닉네임"));
        em.flush(); em.clear();
        assertThat(posts.getPostDetail(postId, null).getAuthorNickname()).isEqualTo("바뀐닉네임");
        assertThat(posts.getPosts(CommunityBoardType.ALL, CommunityPostSort.LATEST, null, null, 20).getPosts())
            .extracting("authorNickname").containsExactly("바뀐닉네임");
        assertThat(posts.getMyPosts(authorId, null, 20).getPosts())
            .extracting("authorNickname").containsExactly("바뀐닉네임");
        for (CommunityCommentSort sort : CommunityCommentSort.values()) {
            CommunityCommentSliceResponse page = comments.getComments(postId, null, sort, null, 1);
            assertThat(page.getComments()).extracting("authorNickname").containsExactly("다른회원");
            assertThat(page.isHasNext()).isTrue();
            assertThat(comments.getComments(postId, null, sort, page.getNextCursor(), 1).getComments())
                .extracting("authorNickname").containsExactly("바뀐닉네임");
        }
        assertThat(em.createNativeQuery("select author_nickname from community_posts where id = :id")
            .setParameter("id", postId).getSingleResult()).isEqualTo("이전닉네임");
        assertThat(em.createNativeQuery("select author_nickname from community_comments where id = :id")
            .setParameter("id", firstId).getSingleResult()).isEqualTo("이전닉네임");
    }

    @Test void commentListQueriesLoadAuthorsForBothSortsAndCursors() {
        User author = user("author", "닉네임");
        CommunityPost post = CommunityPost.create(author, CommunityCategory.SOCCER, "title", "content");
        em.persist(post);
        CommunityComment comment = CommunityComment.create(post, author, null, "comment");
        em.persist(comment); em.flush();
        Long postId = post.getId(), commentId = comment.getId();
        var pageable = PageRequest.of(0, 10);
        for (int query = 0; query < 4; query++) {
            em.clear();
            List<CommunityComment> result = switch (query) {
                case 0 -> commentRepository.findByPost_IdAndStatusOrderByIdDesc(postId, CommunityCommentStatus.ACTIVE, pageable);
                case 1 -> commentRepository.findByPost_IdAndStatusAndIdLessThanOrderByIdDesc(postId, CommunityCommentStatus.ACTIVE, commentId + 1, pageable);
                case 2 -> commentRepository.findRecommendedComments(postId, CommunityCommentStatus.ACTIVE, pageable);
                default -> commentRepository.findRecommendedCommentsAfter(postId, CommunityCommentStatus.ACTIVE, 1, commentId, pageable);
            };
            assertThat(result).hasSize(1);
            assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(result.get(0), "author")).isTrue();
        }
    }

    @Test void nicknameEndpointAcceptsEightCharactersAndRejectsNine() throws Exception {
        User author = user("author", "원래이름");
        em.flush();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(author.getId(), null, List.of()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, tokenService))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/v1/auth/register-nickname").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"가나다라마바사아자\"}"))
            .andExpect(status().isBadRequest());
        assertThatThrownBy(() -> authService.registerNickname(author.getId(),
            new RegisterNicknameRequestDto(null, null, "가나다라마바사아자")))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));
        mvc.perform(post("/api/v1/auth/register-nickname").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"가나다라마바사아\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.newNickname").value("가나다라마바사아"));
    }

    @Test void nicknameLengthCountsEmojiAndCombinedCharacters() throws Exception {
        User author = user("emoji", "원래이름");
        em.flush();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(author.getId(), null, List.of()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, tokenService))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        String emoji = "😀";
        String combined = "e\u0301";
        assertThat(combined.repeat(8).matches(RegisterNicknameRequestDto.NICKNAME_PATTERN)).isTrue();
        assertThat(combined.repeat(9).matches(RegisterNicknameRequestDto.NICKNAME_PATTERN)).isFalse();
        mvc.perform(post("/api/v1/auth/register-nickname").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"" + emoji.repeat(9) + "\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/register-nickname").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"" + emoji.repeat(8) + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.newNickname").value(emoji.repeat(8)));
    }

    private User user(String socialId, String nickname) {
        User user = User.builder().provider("test").socialId(socialId).nickname(nickname).status(UserStatus.ACTIVE).build();
        em.persist(user);
        return user;
    }
}
