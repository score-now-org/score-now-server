package com.scorenow.scorenow_api.domain.admin.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;
import com.scorenow.scorenow_api.domain.chat.service.ChatReportService;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageRepository;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import jakarta.persistence.EntityManager;

@DataJpaTest
@Import({AdminChatService.class, ChatReportService.class, UserReferenceService.class})
class AdminChatSearchIntegrationTest {
    @Autowired EntityManager em;
    @Autowired AdminChatService service;
    @Autowired ChatReportService reports;
    @Autowired ChatMessageRepository messages;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @Test void filtersExistingMessagesBeforePagingAndTreatsWildcardsLiterally() {
        User first = User.builder().provider("test").socialId("social%123").nickname("fan").status(UserStatus.ACTIVE).build();
        User second = User.builder().provider("test").socialId("socialX123").nickname("fan").status(UserStatus.ACTIVE).build();
        Match match = Match.builder().sportId(1L).build();
        em.persist(first); em.persist(second); em.persist(match);
        em.persist(ChatMessage.create(match, first, "first"));
        em.persist(ChatMessage.create(match, first, "second"));
        em.persist(ChatMessage.create(match, second, "third"));
        em.flush(); em.clear();
        var page = service.getMessages(null, match.getId(), 1L, "%12", null, false, false, PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent().get(0).memberId()).isEqualTo(first.getId());
        assertThat(page.getContent().get(0).socialId()).isEqualTo("social%123");
        assertThat(service.getMessages(second.getId(), null, null, null, null, false, false, PageRequest.of(0, 15)).getTotalElements()).isEqualTo(1);
    }

    @Test void reportedImageCanBeModeratedAndDeletedMessagesDisappearFromHistory() {
        User sender = User.builder().provider("test").socialId("sender").nickname("sender").status(UserStatus.ACTIVE).build();
        User reporter = User.builder().provider("test").socialId("reporter").nickname("reporter").status(UserStatus.ACTIVE).build();
        Match match = Match.builder().sportId(1L).build();
        em.persist(sender); em.persist(reporter); em.persist(match);
        ChatMessage chat = ChatMessage.create(match, sender, "", "https://example.test/image.png");
        em.persist(chat); em.flush();
        reports.report(chat.getId(), reporter.getId(), "부적절한 내용");
        assertThatThrownBy(() -> reports.report(chat.getId(), reporter.getId(), "중복"))
            .isInstanceOf(BusinessException.class);
        var reported = service.getMessages(null, null, null, null, null, true, false, PageRequest.of(0, 15));
        assertThat(reported.getContent()).hasSize(1);
        assertThat(reported.getContent().get(0).reportCount()).isEqualTo(1);
        assertThat(service.getReports(chat.getId(), PageRequest.of(0, 20)).getContent())
            .extracting("reason").containsExactly("부적절한 내용");
        assertThat(service.getMessages(null, null, null, null, null, false, true, PageRequest.of(0, 15)).getTotalElements()).isEqualTo(1);
        service.delete(chat.getId(), "관리자 삭제");
        em.flush(); em.clear();
        assertThat(messages.findByMatch_IdOrderByIdDesc(match.getId(), PageRequest.of(0, 10))).isEmpty();
        assertThat(service.getMessages(null, null, null, null, null, false, true, PageRequest.of(0, 15)).getTotalElements()).isZero();
        assertThat(service.getMessages(null, null, null, null, null, true, false, PageRequest.of(0, 15)).getContent().get(0).deleted()).isTrue();
    }

    @Test void rejectsInvalidPageSize() {
        assertThatThrownBy(() -> service.getMessages(null, null, null, null, null, false, false, PageRequest.of(0, 101)))
                .isInstanceOf(BusinessException.class);
    }
}
