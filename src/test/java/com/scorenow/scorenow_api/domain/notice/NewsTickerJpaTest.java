package com.scorenow.scorenow_api.domain.notice;

import static org.assertj.core.api.Assertions.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@DataJpaTest
class NewsTickerJpaTest {
    @Autowired NoticeRepository notices;
    @Autowired NoticeLockRepository locks;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @Test void newsAndNoticesHaveIndependentDraftsOrderAndPublication() {
        locks.saveAndFlush(new NoticeLock(1));
        NoticeController c = new NoticeController(notices, locks);
        Long notice = c.create(new NoticeRequest("공지", "본문", true, true, Notice.Platform.ALL, null, null)).getData().id();
        Long news = c.createNews(new NewsTickerRequest("뉴스", "뉴스 본문", true, Notice.Platform.IOS, "https://example.com/a", "#112233", "#445566")).getData().id();
        Long otherNews = c.createNews(new NewsTickerRequest("공통 뉴스", "본문", true, Notice.Platform.ALL, null, "#000000", "#000000")).getData().id();
        assertThat(c.status().getData().pending()).isTrue();
        assertThat(c.newsStatus().getData().pending()).isTrue();
        c.publishNews();
        assertThat(c.status().getData().pending()).isTrue();
        assertThat(c.newsStatus().getData().pending()).isFalse();
        assertThat(c.list(false, Notice.Platform.IOS, 0, 20).getData()).isEmpty();
        var visible = c.news(Notice.Platform.IOS, 0, 20).getData().getContent();
        assertThat(visible).extracting(NoticeController.NewsResponse::id).containsExactly(otherNews, news);
        assertThat(visible.get(1).titleColor()).isEqualTo("#112233");
        assertThat(c.news(Notice.Platform.ANDROID, 0, 20).getData().getContent()).extracting(NoticeController.NewsResponse::id).containsExactly(otherNews);
        c.updateNews(news, new NewsTickerRequest("새 뉴스", "본문 변경", true, Notice.Platform.IOS, null, "#ffffff", "#000000"));
        c.moveNews(news, new NoticeController.OrderRequest(1));
        assertThat(c.news(Notice.Platform.IOS, 0, 20).getData().getContent().get(1).title()).isEqualTo("뉴스");
        c.publish();
        assertThat(c.list(false, Notice.Platform.IOS, 0, 20).getData().getContent()).extracting(NoticeController.Response::id).containsExactly(notice);
        assertThat(c.news(Notice.Platform.IOS, 0, 20).getData().getContent().get(1).title()).isEqualTo("뉴스");
        assertThatThrownBy(() -> c.delete(news)).isInstanceOf(BusinessException.class);
        c.publishNews();
        assertThat(c.news(Notice.Platform.IOS, 0, 20).getData().getContent().get(0).titleColor()).isEqualTo("#ffffff");
        c.deleteNews(news); c.publishNews();
        assertThat(c.news(Notice.Platform.IOS, 0, 20).getData().getContent()).extracting(NoticeController.NewsResponse::id).containsExactly(otherNews);
        assertThat(c.list(false, Notice.Platform.IOS, 0, 20).getData().getTotalElements()).isEqualTo(1);
    }
}
