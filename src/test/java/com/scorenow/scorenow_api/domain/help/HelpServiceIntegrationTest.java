package com.scorenow.scorenow_api.domain.help;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@DataJpaTest
@Import({HelpService.class, HelpServiceIntegrationTest.Config.class})
class HelpServiceIntegrationTest {
    @Autowired HelpService service;
    @MockBean FileStorage storage;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @TestConfiguration
    static class Config {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    }

    @Test
    void draftPublicationOrderingAndConflict() {
        when(storage.getFileUrl("help/example.png")).thenReturn("https://example.test/example.png");
        var first = new HelpService.ItemInput("one", "첫 도움말", null);
        var second = new HelpService.ItemInput("two", "", "help/example.png");
        var draft = service.saveDraft(new HelpService.PublishRequest(-1L, List.of(first, second)));
        assertThat(draft.unpublished()).isTrue();
        assertThat(service.get(true).items()).isEmpty();
        var createdAt = draft.items().get(0).createdAt();

        var published = service.publish(new HelpService.VersionRequest(draft.version()));
        assertThat(published.unpublished()).isFalse();
        assertThat(service.get(true).items()).extracting(HelpService.Item::id).containsExactly("one", "two");
        assertThat(service.get(true).items().get(1).imageUrl()).isEqualTo("https://example.test/example.png");

        var changed = service.saveDraft(new HelpService.PublishRequest(published.version(), List.of(second, first)));
        assertThat(changed.items().get(1).createdAt()).isEqualTo(createdAt);
        assertThat(service.get(true).items()).extracting(HelpService.Item::id).containsExactly("one", "two");
        assertThatThrownBy(() -> service.publish(new HelpService.VersionRequest(published.version())))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.HELP_EDIT_CONFLICT));
        service.publish(new HelpService.VersionRequest(changed.version()));
        assertThat(service.get(true).items()).extracting(HelpService.Item::id).containsExactly("two", "one");
        var empty = service.saveDraft(new HelpService.PublishRequest(service.get(false).version(), List.of()));
        service.publish(new HelpService.VersionRequest(empty.version()));
        assertThat(service.get(true).items()).isEmpty();
    }

    @Test
    void rejectsDuplicateIdsEmptyItemsAndExternalImageKeys() {
        var item = new HelpService.ItemInput("one", "내용", null);
        assertThatThrownBy(() -> service.saveDraft(new HelpService.PublishRequest(-1L, List.of(item, item))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.saveDraft(new HelpService.PublishRequest(-1L, List.of(new HelpService.ItemInput("one", " ", null)))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.saveDraft(new HelpService.PublishRequest(-1L, List.of(new HelpService.ItemInput("one", "", "other/image.png")))))
                .isInstanceOf(BusinessException.class);
    }
}
