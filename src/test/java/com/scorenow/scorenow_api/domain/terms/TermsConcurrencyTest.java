package com.scorenow.scorenow_api.domain.terms;

import static org.assertj.core.api.Assertions.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import java.util.List;
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
@Import(TermsService.class)
class TermsConcurrencyTest {
    @Autowired TermsService service;
    @Autowired TermsLockRepository locks;
    @Autowired TermsRevisionRepository revisions;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousPublishDoesNotOverwriteAnotherEditor() throws Exception {
        revisions.deleteAll();
        locks.saveAndFlush(new TermsLock(1));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Object> edit = () -> {
                start.await();
                try { return service.publish(new TermsService.PublishRequest(0L, Thread.currentThread().getName())); }
                catch (BusinessException e) { return e.getErrorCode(); }
            };
            Future<Object> a = pool.submit(edit), b = pool.submit(edit);
            start.countDown();
            List<Object> results = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertThat(results.stream().filter(TermsService.Snapshot.class::isInstance)).hasSize(1);
            assertThat(results).contains(ErrorCode.TERMS_EDIT_CONFLICT);
            assertThat(revisions.count()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
            revisions.deleteAll();
            locks.deleteAll();
        }
    }
}
