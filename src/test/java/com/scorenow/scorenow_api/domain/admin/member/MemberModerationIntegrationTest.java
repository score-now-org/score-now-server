package com.scorenow.scorenow_api.domain.admin.member;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.admin.member.dto.MemberActionRequest;
import com.scorenow.scorenow_api.domain.admin.member.dto.SuspensionDuration;
import com.scorenow.scorenow_api.domain.admin.member.service.MemberModerationService;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import jakarta.persistence.EntityManager;

@DataJpaTest
@Import(MemberModerationService.class)
class MemberModerationIntegrationTest {
    @Autowired EntityManager em;
    @Autowired MemberModerationService moderation;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @Test void actionsRecordReasonsPreservePreviousStatusAndRevokeOldTokens() {
        User user = User.builder().provider("test").socialId("id").nickname("fan").status(UserStatus.ACTIVE).warningCnt(0).profileImageUrl("https://example.test/p.png").build();
        em.persist(user); em.flush();
        var jwt = new JwtProvider("12345678901234567890123456789012");
        String original = jwt.createAccessToken(user.getId(), user.currentTokenVersion());
        moderation.act(user.getId(), new MemberActionRequest("WARN", "사유"));
        moderation.act(user.getId(), new MemberActionRequest("BAN", "정지 사유"));
        moderation.act(user.getId(), new MemberActionRequest("PERMANENT_WITHDRAWAL", "영구탈퇴 사유"));
        moderation.act(user.getId(), new MemberActionRequest("RESTORE", null));
        assertThat(user.getStatus()).isEqualTo(UserStatus.BANNED);
        moderation.act(user.getId(), new MemberActionRequest("RESTORE", null));
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getWarningCnt()).isEqualTo(1);
        assertThat(user.getWarningReason()).isEqualTo("사유");
        assertThat(user.getPermanentWithdrawalReason()).isEqualTo("영구탈퇴 사유");
        assertThat(user.currentTokenVersion()).isGreaterThan(jwt.getTokenVersion(original));
        assertThat(moderation.history(user.getId())).extracting("action")
            .containsExactly("RESTORE", "RESTORE", "PERMANENT_WITHDRAWAL", "BAN", "WARN");
        moderation.act(user.getId(), new MemberActionRequest("DELETE_PROFILE_IMAGE", "부적절한 이미지"));
        assertThat(user.getProfileImageUrl()).isNull();
        assertThatThrownBy(() -> moderation.act(user.getId(), new MemberActionRequest("BAN", " ")))
            .isInstanceOf(BusinessException.class);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test void timedBansExpireWithoutReactivatingIndefiniteOrPermanentlyWithdrawnMembers() {
        User timed = member("timed");
        User indefinite = member("indefinite");
        User withdrawn = member("withdrawn");
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);
        moderation.act(timed.getId(), new MemberActionRequest("BAN_WEEK", "사유"));
        assertThat(timed.getBanExpiresAt()).isBetween(before.plusWeeks(1), LocalDateTime.now(ZoneOffset.UTC).plusWeeks(1));
        assertThat(moderation.history(timed.getId()).get(0).duration()).isEqualTo("WEEK");
        moderation.act(indefinite.getId(), new MemberActionRequest("BAN", "사유"));
        moderation.act(withdrawn.getId(), new MemberActionRequest("BAN", "사유", SuspensionDuration.MONTH));
        moderation.act(withdrawn.getId(), new MemberActionRequest("PERMANENT_WITHDRAWAL", "사유"));
        timed.setBanExpiresAt(before.minusSeconds(1));
        withdrawn.setBanExpiresAt(before.minusSeconds(1));
        em.flush();
        moderation.expireTimedBans();
        em.clear();
        assertThat(em.find(User.class, timed.getId()).getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(em.find(User.class, indefinite.getId()).getStatus()).isEqualTo(UserStatus.BANNED);
        assertThat(em.find(User.class, withdrawn.getId()).getStatus()).isEqualTo(UserStatus.PERMANENTLY_WITHDRAWN);
        moderation.act(withdrawn.getId(), new MemberActionRequest("RESTORE", null));
        assertThat(em.find(User.class, withdrawn.getId()).getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(em.find(User.class, withdrawn.getId()).getBanExpiresAt()).isNull();
    }

    @Test void manualRestoreClearsExpiryAndInvalidDurationIsRejected() {
        User user = member("manual");
        moderation.act(user.getId(), new MemberActionRequest("BAN", "사유", SuspensionDuration.MONTH));
        assertThat(user.getBanExpiresAt()).isNotNull();
        moderation.act(user.getId(), new MemberActionRequest("RESTORE", null));
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getBanExpiresAt()).isNull();
        assertThatThrownBy(() -> moderation.act(user.getId(), new MemberActionRequest("WARN", "사유", SuspensionDuration.WEEK)))
                .isInstanceOf(BusinessException.class);
    }

    private User member(String socialId) {
        User user = User.builder().provider("test").socialId(socialId).nickname(socialId)
                .status(UserStatus.ACTIVE).warningCnt(0).build();
        em.persist(user);
        em.flush();
        return user;
    }
}
