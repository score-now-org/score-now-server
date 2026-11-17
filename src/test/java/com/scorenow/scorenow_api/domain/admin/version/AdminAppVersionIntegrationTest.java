package com.scorenow.scorenow_api.domain.admin.version;

import static org.assertj.core.api.Assertions.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@DataJpaTest
@Import(AdminAppVersionService.class)
class AdminAppVersionIntegrationTest {
    @Autowired AdminAppVersionService service;
    @Autowired EntityManager em;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    private AdminAppVersionService.Request request(String version, String date, String content) {
        return new AdminAppVersionService.Request(version, AppVersionRelease.Platform.ALL, LocalDate.parse(date), content);
    }

    @Test void persistsEditsDeletesAndListsInStableDateOrder() {
        var older = service.create(request(" 1.0 ", "2025-01-01", " 첫 업데이트 "));
        var newer = service.create(request("1.1", "2025-02-01", "두 번째"));
        var sameDate = service.create(request("1.2", "2025-02-01", "세 번째"));
        em.flush(); em.clear();
        assertThat(service.list(0, 2).getContent()).extracting(AdminAppVersionService.Response::id)
                .containsExactly(sameDate.id(), newer.id());
        assertThat(service.list(1, 2).getContent().get(0).versionName()).isEqualTo("1.0");
        service.update(older.id(), new AdminAppVersionService.Request("2.0", AppVersionRelease.Platform.IOS,
                LocalDate.parse("2025-03-01"), "수정"));
        em.flush(); em.clear();
        assertThat(service.list(0, 20).getContent().get(0).platform()).isEqualTo(AppVersionRelease.Platform.IOS);
        service.delete(older.id());
        em.flush(); em.clear();
        assertThat(service.list(0, 20).getTotalElements()).isEqualTo(2);
        assertThatThrownBy(() -> service.delete(older.id())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.list(0, 101)).isInstanceOf(BusinessException.class);
    }
}
