package com.scorenow.scorenow_api.domain.notice;

import static org.assertj.core.api.Assertions.assertThat;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(NoticeController.class)
class NoticeConcurrencyTest {
    @Autowired NoticeController controller;
    @Autowired NoticeRepository notices;
    @Autowired NoticeLockRepository locks;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentCreatesKeepAUniqueOrder() throws Exception {
        locks.saveAndFlush(new NoticeLock(1));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Void> add = () -> {
                start.await();
                for (int i = 0; i < 5; i++) controller.create(new NoticeRequest("제목", "내용", false, true, Notice.Platform.ALL, null, null));
                return null;
            };
            Future<Void> first = executor.submit(add), second = executor.submit(add);
            start.countDown();
            first.get(30, TimeUnit.SECONDS); second.get(30, TimeUnit.SECONDS);
            assertThat(notices.findByDeletedFalseOrderBySortOrderAscIdDesc())
                    .extracting(Notice::getSortOrder).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        } finally {
            executor.shutdownNow();
        }
    }
}
