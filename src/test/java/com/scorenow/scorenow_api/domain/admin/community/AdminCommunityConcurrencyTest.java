package com.scorenow.scorenow_api.domain.admin.community;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.community.dto.request.CommunityPostUpdateRequest;
import com.scorenow.scorenow_api.domain.admin.forbiddenword.ForbiddenWordFilter;
import com.scorenow.scorenow_api.domain.community.entity.*;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostRepository;
import com.scorenow.scorenow_api.domain.community.service.*;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import jakarta.persistence.EntityManager;

@DataJpaTest
@Import({AdminCommunityService.class, CommunityCommentService.class, CommunityPostCommandService.class,
        CommunityReferenceService.class, CommunityAuthService.class, UserReferenceService.class, ForbiddenWordFilter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminCommunityConcurrencyTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AdminCommunityService admin;
    @Autowired CommunityCommentService userComments;
    @Autowired CommunityPostCommandService userPosts;
    @Autowired CommunityPostRepository posts;
    @SpyBean CommunityReferenceService reference;
    @MockBean JPAQueryFactory queryFactory;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean FileStorage storage;

    @Test void onlyTheAuthorCanDeleteAnActiveComment() {
        Fixture f = fixture();
        assertThatThrownBy(() -> userComments.deleteComment(f.commentId(), f.userId() + 1000))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.COMMUNITY_FORBIDDEN));
        userComments.deleteComment(f.commentId(), f.userId());
        Long count = new TransactionTemplate(transactionManager).execute(status -> posts.findById(f.postId()).orElseThrow().getCommentCount());
        assertThat(count).isEqualTo(1L);
    }

    @Test void concurrentOwnerAndAdminCommentDeletionDecrementsOnlyOnce() throws Exception {
        Fixture f = fixture();
        CountDownLatch loaded = new CountDownLatch(1), resume = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object comment = invocation.callRealMethod();
            loaded.countDown();
            if (!resume.await(10, TimeUnit.SECONDS)) throw new AssertionError("owner deletion was not resumed");
            return comment;
        }).when(reference).requireActiveComment(f.commentId());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> owner = executor.submit(() -> userComments.deleteComment(f.commentId(), f.userId()));
            assertThat(loaded.await(10, TimeUnit.SECONDS)).isTrue();
            try { admin.deleteComment(f.commentId()); } finally { resume.countDown(); }
            assertThatThrownBy(() -> owner.get(10, TimeUnit.SECONDS))
                .hasCauseInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex.getCause()).getErrorCode()).isEqualTo(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
            Long count = new TransactionTemplate(transactionManager).execute(status -> posts.findById(f.postId()).orElseThrow().getCommentCount());
            assertThat(count).isEqualTo(1L);
        } finally { resume.countDown(); executor.shutdownNow(); }
    }

    @Test void ownerUpdateCannotRestoreAnAdminDeletedPost() throws Exception {
        Fixture f = fixture();
        CountDownLatch beforeLock = new CountDownLatch(1), resume = new CountDownLatch(1);
        doAnswer(invocation -> {
            beforeLock.countDown();
            if (!resume.await(10, TimeUnit.SECONDS)) throw new AssertionError("owner update was not resumed");
            return invocation.callRealMethod();
        }).when(reference).requireActivePostForUpdate(f.postId());
        CommunityPostUpdateRequest update = new CommunityPostUpdateRequest();
        ReflectionTestUtils.setField(update, "category", CommunityCategory.SOCCER);
        ReflectionTestUtils.setField(update, "title", "수정 제목");
        ReflectionTestUtils.setField(update, "content", "수정 내용");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> owner = executor.submit(() -> userPosts.updatePost(f.postId(), f.userId(), update));
            assertThat(beforeLock.await(10, TimeUnit.SECONDS)).isTrue();
            try { admin.deletePost(f.postId()); } finally { resume.countDown(); }
            assertThatThrownBy(() -> owner.get(10, TimeUnit.SECONDS))
                .hasCauseInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex.getCause()).getErrorCode()).isEqualTo(ErrorCode.COMMUNITY_POST_NOT_FOUND));
            CommunityPostStatus status = new TransactionTemplate(transactionManager).execute(tx -> posts.findById(f.postId()).orElseThrow().getStatus());
            assertThat(status).isEqualTo(CommunityPostStatus.DELETED);
        } finally { resume.countDown(); executor.shutdownNow(); }
    }

    private Fixture fixture() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            User user = User.builder().provider("test").socialId("race-" + java.util.UUID.randomUUID()).nickname("팬").status(UserStatus.ACTIVE).build();
            em.persist(user);
            CommunityPost post = CommunityPost.create(user, CommunityCategory.SOCCER, "제목", "본문");
            em.persist(post);
            CommunityComment target = CommunityComment.create(post, user, null, "댓글");
            em.persist(target); em.persist(CommunityComment.create(post, user, null, "다른 댓글"));
            post.increaseCommentCount(); post.increaseCommentCount();
            em.flush();
            return new Fixture(user.getId(), post.getId(), target.getId());
        });
    }
    private record Fixture(Long userId, Long postId, Long commentId) { }
}
