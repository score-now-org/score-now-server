package com.scorenow.scorenow_api.domain.admin.member;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.admin.member.dto.MemberActionRequest;
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
}
