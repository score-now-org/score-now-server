package com.scorenow.scorenow_api.domain.admin.member;

import static org.assertj.core.api.Assertions.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.admin.member.dto.MemberSearchCondition;
import com.scorenow.scorenow_api.domain.admin.member.service.AdminMemberService;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.entity.UserLoginHistory;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import jakarta.persistence.EntityManager;

@DataJpaTest
@Import(AdminMemberService.class)
class AdminMemberSearchIntegrationTest {
    @Autowired EntityManager em;
    @Autowired AdminMemberService service;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @Test void filtersInDatabaseBeforePagingAndDoesNotDuplicateUsersWithMultipleIpHits() {
        User first = user("one", "팬%일", 2, UserStatus.ACTIVE);
        User second = user("two", "팬둘", 1, UserStatus.ACTIVE);
        User third = user("three", "팬셋", 0, UserStatus.DELETED);
        em.persist(new UserLoginHistory(first, "KR", "192.0.2.10"));
        em.persist(new UserLoginHistory(first, "KR", "192.0.2.10"));
        em.persist(new UserLoginHistory(second, "KR", "192.0.2.10"));
        em.flush(); em.clear();
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setStatus("active");
        condition.setMinWarningCnt(1);
        condition.setIp("192.0.2.10");
        var page = service.getMembers(condition, PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        condition.setNickname("%");
        var matched = service.getMembers(condition, PageRequest.of(0, 10));
        assertThat(matched.getContent()).extracting("memberId").containsExactly(first.getId());
        assertThat(matched.getContent().get(0).getProfileImageUrl()).isEqualTo("https://example.test/profile.png");
        condition.setNickname(null);
        condition.setStatus("DELETED"); condition.setMinWarningCnt(null); condition.setIp(null);
        assertThat(service.getMembers(condition, PageRequest.of(0, 10)).getContent())
                .extracting("memberId").containsExactly(third.getId());
    }
    @Test void distinguishesPermanentWithdrawalFromBanAndSearchesSocialIdLiterally() {
        User permanent = user("social%123", "nickname", 0, UserStatus.PERMANENTLY_WITHDRAWN);
        user("socialX123", "nickname", 0, UserStatus.PERMANENTLY_WITHDRAWN);
        user("social%123", "nickname", 0, UserStatus.BANNED);
        em.flush(); em.clear();
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setStatus("permanently_withdrawn");
        condition.setSocialId("%12");
        var page = service.getMembers(condition, PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting("memberId").containsExactly(permanent.getId());
        assertThat(page.getContent().get(0).getSocialId()).isEqualTo("social%123");
    }
    @Test void rejectsInvalidFilters() {
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setStatus("unknown");
        assertThatThrownBy(() -> service.getMembers(condition, PageRequest.of(0, 15)))
                .isInstanceOf(BusinessException.class);
        condition.setStatus(null);
        condition.setCreatedFrom(LocalDate.of(2026, 2, 2));
        condition.setCreatedTo(LocalDate.of(2026, 2, 1));
        assertThatThrownBy(() -> service.getMembers(condition, PageRequest.of(0, 15)))
                .isInstanceOf(BusinessException.class);
    }
    private User user(String socialId, String nickname, int warnings, UserStatus status) {
        User user = User.builder().provider("test").socialId(socialId).nickname(nickname)
                .profileImageUrl("https://example.test/profile.png").warningCnt(warnings).status(status).build();
        em.persist(user);
        return user;
    }
}
