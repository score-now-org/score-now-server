package com.scorenow.scorenow_api.domain.terms;

import static org.assertj.core.api.Assertions.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@DataJpaTest
@Import(TermsService.class)
class TermsServiceTest {
    @Autowired TermsService service;
    @Autowired TermsLockRepository locks;
    @Autowired TermsRevisionRepository revisions;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @Test void publishesExactTextAndRejectsStaleEditors() {
        locks.saveAndFlush(new TermsLock(1));
        assertThat(service.current().published()).isFalse();
        var first = service.publish(new TermsService.PublishRequest(0L, "  첫 약관\n두 번째 줄  "));
        assertThat(first.content()).isEqualTo("  첫 약관\n두 번째 줄  ");
        assertThat(service.current()).isEqualTo(first);
        assertThat(service.publish(new TermsService.PublishRequest(first.version(), first.content()))).isEqualTo(first);
        var second = service.publish(new TermsService.PublishRequest(first.version(), "수정된 약관"));
        assertThat(second.version()).isGreaterThan(first.version());
        assertThat(revisions.count()).isEqualTo(2);
        assertThatThrownBy(() -> service.publish(new TermsService.PublishRequest(first.version(), "오래된 수정")))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TERMS_EDIT_CONFLICT));
        assertThat(service.current()).isEqualTo(second);
    }
}
