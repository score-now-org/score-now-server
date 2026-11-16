package com.scorenow.scorenow_api.domain.admin.community;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.community.entity.*;
import com.scorenow.scorenow_api.domain.community.repository.*;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import jakarta.persistence.EntityManager;

@DataJpaTest
@Import(AdminCommunityService.class)
class AdminCommunityIntegrationTest {
    @Autowired EntityManager em;
    @Autowired AdminCommunityService service;
    @Autowired CommunityPostRepository posts;
    @Autowired CommunityCommentRepository comments;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;
    @MockBean FileStorage storage;
    private final PageRequest page = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id"));

    @Test void dailyBoundariesUseSeoulDayInTheServerStorageTimezone() {
        Instant now = Instant.parse("2026-11-17T00:00:00Z");
        assertThat(AdminCommunityService.seoulDayBounds(now, ZoneId.of("UTC")))
                .containsExactly(LocalDateTime.of(2026, 11, 16, 15, 0), LocalDateTime.of(2026, 11, 17, 15, 0));
        assertThat(AdminCommunityService.seoulDayBounds(now, ZoneId.of("Asia/Seoul")))
                .containsExactly(LocalDateTime.of(2026, 11, 17, 0, 0), LocalDateTime.of(2026, 11, 18, 0, 0));
    }

    @Test void filtersPostsAndCommentsBeforePaginationAndUsesPostThumbnail() {
        User author = user("social%one", "작성자");
        CommunityPost hot = post(author, "HOT 제목");
        for (int i = 0; i < 10; i++) hot.increaseLikeCount();
        CommunityPost ordinary = post(author, "일반 제목");
        post(user("socialXone", "다른작성자"), "다른 작성자의 게시글");
        em.persist(CommunityPostImage.create(hot, "image-key", 0));
        em.persist(CommunityComment.create(hot, author, null, "테스트 댓글"));
        em.persist(CommunityComment.create(ordinary, author, null, "다른 댓글"));
        em.flush(); em.clear();
        when(storage.getFileUrl("image-key")).thenReturn("https://example.test/image.png");
        var hotPage = service.posts(null, "%one", null, null, true, page);
        assertThat(hotPage.page().getTotalElements()).isEqualTo(1);
        assertThat(hotPage.page().getContent().get(0).id()).isEqualTo(hot.getId());
        assertThat(hotPage.page().getContent().get(0).imageUrl()).isEqualTo("https://example.test/image.png");
        assertThat(hotPage.todayCount()).isGreaterThanOrEqualTo(2);
        assertThat(service.posts(null, "%one", null, null, false, page).page().getTotalElements()).isEqualTo(2);
        assertThat(service.posts(null, null, null, null, false, page).page().getTotalElements()).isEqualTo(3);
        assertThat(service.posts(null, "%missing", null, null, false, page).page().getTotalElements()).isZero();
        assertThat(service.comments("테스트", null, "작성자", author.getId(), null, page).getContent())
                .extracting("postId").containsExactly(hot.getId());
        assertThat(service.comments("HOT 제목", null, null, null, null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.comments(null, null, null, null, hot.getId(), page).getTotalElements()).isEqualTo(1);
    }

    @Test void deletesACommentAndThenSoftDeletesPostAndRemainingComments() {
        User author = user("sender", "팬");
        CommunityPost post = post(author, "제목");
        CommunityComment first = CommunityComment.create(post, author, null, "첫 댓글");
        CommunityComment second = CommunityComment.create(post, author, null, "둘째 댓글");
        em.persist(first); em.persist(second); post.increaseCommentCount(); post.increaseCommentCount();
        em.flush(); em.clear();
        service.deleteComment(first.getId());
        assertThat(posts.findById(post.getId()).orElseThrow().getCommentCount()).isEqualTo(1);
        assertThat(comments.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(CommunityCommentStatus.DELETED);
        assertThatThrownBy(() -> service.deleteComment(first.getId())).isInstanceOf(BusinessException.class);
        service.deletePost(post.getId());
        assertThat(posts.findById(post.getId()).orElseThrow().getStatus()).isEqualTo(CommunityPostStatus.DELETED);
        assertThat(posts.findById(post.getId()).orElseThrow().getCommentCount()).isZero();
        assertThat(comments.findById(second.getId()).orElseThrow().getStatus()).isEqualTo(CommunityCommentStatus.DELETED);
        assertThat(comments.findByPost_IdAndStatusOrderByIdDesc(post.getId(), CommunityCommentStatus.ACTIVE, page)).isEmpty();
        assertThatThrownBy(() -> service.deletePost(post.getId())).isInstanceOf(BusinessException.class);
    }

    @Test void nicknameSearchMatchesTheDisplayedSnapshotWhenTheCurrentNicknameIsMissing() {
        User author = user("anonymous", null);
        CommunityPost post = post(author, "제목");
        em.persist(CommunityComment.create(post, author, null, "댓글"));
        em.flush(); em.clear();
        assertThat(service.posts(null, null, "익명", null, false, page).page().getContent())
                .extracting("nickname").containsExactly("익명");
        assertThat(service.comments(null, null, "익명", null, null, page).getContent())
                .extracting("nickname").containsExactly("익명");
    }

    private User user(String socialId, String nickname) {
        User user = User.builder().provider("test").socialId(socialId).nickname(nickname).status(UserStatus.ACTIVE).build();
        em.persist(user); return user;
    }
    private CommunityPost post(User author, String title) {
        CommunityPost post = CommunityPost.create(author, CommunityCategory.SOCCER, title, "본문");
        em.persist(post); return post;
    }
}
