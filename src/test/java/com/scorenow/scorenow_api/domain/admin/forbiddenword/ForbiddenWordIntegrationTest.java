package com.scorenow.scorenow_api.domain.admin.forbiddenword;

import static org.assertj.core.api.Assertions.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.util.ReflectionTestUtils;
import com.scorenow.scorenow_api.domain.community.service.*;
import com.scorenow.scorenow_api.domain.community.dto.request.*;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCategory;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostRepository;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;

@DataJpaTest
@Import({ForbiddenWordController.class, ForbiddenWordFilter.class, CommunityPostCommandService.class,
        CommunityCommentService.class, CommunityAuthService.class, CommunityReferenceService.class, UserReferenceService.class})
class ForbiddenWordIntegrationTest {
    @Autowired ForbiddenWordController controller;
    @Autowired ForbiddenWordRepository repository;
    @Autowired ForbiddenWordFilter filter;
    @Autowired EntityManager em;
    @Autowired CommunityPostCommandService posts;
    @Autowired CommunityCommentService comments;
    @Autowired CommunityPostRepository postRepository;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @Test void registersSearchesAndDeletesWords() {
        controller.create(new ForbiddenWordController.Request(" 단어 ", ""));
        controller.create(new ForbiddenWordController.Request("다른말", "***"));
        em.flush(); em.clear();
        var word = repository.findAll().stream().filter(w -> w.getWord().equals("단어")).findFirst().orElseThrow();
        assertThat(word.getReplacement()).isEqualTo("???");
        assertThat(controller.list("단어", 0, 20).getData().getContent()).extracting("word").containsExactly("단어");
        assertThatThrownBy(() -> controller.create(new ForbiddenWordController.Request("단어", "x")))
                .isInstanceOf(BusinessException.class);
        controller.delete(word.getId());
        em.flush();
        assertThat(repository.findById(word.getId())).isEmpty();
    }

    @Test void replacesLongestMatchesOnceAndUpdatesAfterDeletion() {
        controller.create(new ForbiddenWordController.Request("나쁜", "치환"));
        controller.create(new ForbiddenWordController.Request("나쁜말", "나쁜"));
        controller.create(new ForbiddenWordController.Request("X", "123456"));
        em.flush();
        assertThat(filter.apply(" 나쁜말과 나쁜 ", 100)).isEqualTo("나쁜과 치환");
        assertThatThrownBy(() -> filter.apply("XX", 3)).isInstanceOf(BusinessException.class);
        ForbiddenWord longer = repository.findAll().stream().filter(w -> w.getWord().equals("나쁜말")).findFirst().orElseThrow();
        controller.delete(longer.getId());
        em.flush();
        assertThat(filter.apply("나쁜말", 100)).isEqualTo("치환말");
    }

    @Test void filtersNewAndEditedPostsAndNewComments() {
        controller.create(new ForbiddenWordController.Request("나쁜말", "???"));
        User user = User.builder().provider("test").socialId("filter-user").nickname("fan").status(UserStatus.ACTIVE).build();
        em.persist(user); em.flush();
        CommunityPostCreateRequest request = new CommunityPostCreateRequest();
        ReflectionTestUtils.setField(request, "category", CommunityCategory.SOCCER);
        ReflectionTestUtils.setField(request, "title", "나쁜말 제목");
        ReflectionTestUtils.setField(request, "content", "나쁜말 내용");
        Long postId = posts.createPost(user.getId(), request);
        assertThat(postRepository.findById(postId).orElseThrow().getTitle()).isEqualTo("??? 제목");
        CommunityPostUpdateRequest update = new CommunityPostUpdateRequest();
        ReflectionTestUtils.setField(update, "category", CommunityCategory.SOCCER);
        ReflectionTestUtils.setField(update, "title", "수정 나쁜말");
        ReflectionTestUtils.setField(update, "content", "수정 나쁜말");
        posts.updatePost(postId, user.getId(), update);
        assertThat(postRepository.findById(postId).orElseThrow().getContent()).isEqualTo("수정 ???");
        CommunityCommentCreateRequest comment = new CommunityCommentCreateRequest();
        ReflectionTestUtils.setField(comment, "content", "댓글 나쁜말");
        assertThat(comments.createComment(postId, user.getId(), comment).getContent()).isEqualTo("댓글 ???");
    }
}
