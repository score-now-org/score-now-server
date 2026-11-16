package com.scorenow.scorenow_api.domain.notice;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import com.querydsl.jpa.impl.JPAQueryFactory;

@DataJpaTest @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class NoticeJpaTest {
 @Autowired NoticeRepository notices;
 @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
 @MockBean JPAQueryFactory queryFactory;
 @Test void publishControlsVisibilityPlatformAndDeletion() {
  NoticeController c = new NoticeController(notices);
  Long id = c.create(new NoticeRequest("제목", "본문", true, true, Notice.Platform.IOS, null, null)).getData().id();
  assertThat(c.list(false, Notice.Platform.IOS, 0, 20).getData()).isEmpty();
  c.publish();
  assertThat(c.list(true, Notice.Platform.IOS, 0, 20).getData().getContent()).extracting(NoticeController.Response::title).containsExactly("제목");
  assertThat(c.list(false, Notice.Platform.ANDROID, 0, 20).getData()).isEmpty();
  c.update(id, new NoticeRequest("변경", "본문", false, false, Notice.Platform.IOS, 1, null));
  assertThat(c.detail(id, Notice.Platform.IOS).getData().title()).isEqualTo("제목");
  c.publish();
  assertThatThrownBy(() -> c.detail(id, Notice.Platform.IOS)).isInstanceOf(BusinessException.class);
 }
}
