package com.scorenow.scorenow_api.domain.inquiry;

import static org.assertj.core.api.Assertions.*;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import(InquiryController.class)
class InquiryConcurrencyTest {
    @Autowired InquiryController controller;
    @Autowired InquiryRepository inquiries;
    @Autowired InquiryImageRepository images;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactionManager;
    @MockBean UserReferenceService userReferences;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void answeringWaitsForAttachmentUpdateAndPreservesIt() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        long[] ids = tx.execute(status -> {
            User user = users.saveAndFlush(User.builder().provider("test").socialId("concurrent-inquiry").build());
            Inquiry inquiry = inquiries.saveAndFlush(new Inquiry(user, "문의", "내용"));
            return new long[]{user.getId(), inquiry.getId()};
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        Future<?>[] answer = new Future<?>[1];
        try {
            tx.executeWithoutResult(status -> {
                Inquiry inquiry = inquiries.findOwnedForUpdate(ids[1], ids[0]).orElseThrow();
                inquiry.attachFile("file.pdf", false);
                images.save(new InquiryImage(ids[1], "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)));
                answer[0] = executor.submit(() -> { started.countDown(); return controller.answer(ids[1], new InquiryController.Answer("답변", null)); });
                try {
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                    assertThatThrownBy(() -> answer[0].get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
            });
            answer[0].get(10, TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                Inquiry saved = inquiries.findById(ids[1]).orElseThrow();
                assertThat(saved.getAnswer()).isEqualTo("답변");
                assertThat(saved.getAttachmentName()).isEqualTo("file.pdf");
                assertThat(saved.hasAttachment()).isTrue();
            });
        } finally {
            executor.shutdownNow();
            tx.executeWithoutResult(status -> { images.deleteById(ids[1]); inquiries.deleteById(ids[1]); users.deleteById(ids[0]); });
        }
    }
}
