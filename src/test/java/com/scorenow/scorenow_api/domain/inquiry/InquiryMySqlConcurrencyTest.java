package com.scorenow.scorenow_api.domain.inquiry;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import(InquiryController.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// Opt in only with the disposable local container documented in INQUIRY_API_GUIDE.md.
@EnabledIfEnvironmentVariable(named = "INQUIRY_MYSQL_DISPOSABLE_TEST", matches = "true")
class InquiryMySqlConcurrencyTest {
    @Autowired InquiryController controller;
    @Autowired InquiryRepository inquiries;
    @Autowired InquiryImageRepository images;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactionManager;
    @MockBean UserReferenceService userReferences;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:mysql://127.0.0.1:13307/scorenow_inquiry_test_only?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "localtest");
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void competingFirstUploadsSeeTheCommittedAttachmentBeforeReplacingIt() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        long[] ids = tx.execute(status -> {
            User user = users.saveAndFlush(User.builder().provider("test").socialId("mysql-inquiry").build());
            Inquiry inquiry = inquiries.saveAndFlush(new Inquiry(user, "문의", "내용"));
            return new long[]{user.getId(), inquiry.getId()};
        });
        // This read establishes a repeatable-read snapshot when it occurs inside the upload transaction.
        doAnswer(invocation -> { users.existsById(invocation.getArgument(0)); return null; }).when(userReferences).requireActiveUser(anyLong());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        Future<?>[] second = new Future<?>[1];
        byte[] firstPdf = "%PDF-first".getBytes(StandardCharsets.US_ASCII);
        byte[] secondPdf = "%PDF-second".getBytes(StandardCharsets.US_ASCII);
        try {
            tx.executeWithoutResult(status -> {
                Inquiry locked = inquiries.findOwnedForUpdate(ids[1], ids[0]).orElseThrow();
                images.saveAndFlush(new InquiryImage(ids[1], "application/pdf", firstPdf));
                locked.attachFile("first.pdf", false);
                second[0] = executor.submit(() -> {
                    started.countDown();
                    controller.uploadAttachment(ids[0], ids[1], new MockMultipartFile("file", "second.pdf", "application/pdf", secondPdf));
                    return null;
                });
                try {
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                    assertThatThrownBy(() -> second[0].get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
            });
            second[0].get(15, TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                assertThat(inquiries.findById(ids[1]).orElseThrow().getAttachmentName()).isEqualTo("second.pdf");
                assertThat(images.findById(ids[1]).orElseThrow().getData()).containsExactly(secondPdf);
            });
        } finally { executor.shutdownNow(); }
    }

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void administratorSearchEscapesWildcardsAndFiltersByDateAndStatus() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        long[] ids = tx.execute(status -> {
            User first = users.saveAndFlush(User.builder().provider("test").socialId("mysql-search-1").nickname("축구_팬").build());
            User second = users.saveAndFlush(User.builder().provider("test").socialId("mysql-search-2").nickname("축구팬").build());
            Inquiry one = inquiries.saveAndFlush(new Inquiry(first, "첫 문의", "내용"));
            Inquiry two = inquiries.saveAndFlush(new Inquiry(second, "두 번째 문의", "내용"));
            return new long[]{first.getId(), second.getId(), one.getId(), two.getId()};
        });
        try {
            assertThat(controller.adminList(0, 20, "_", null, LocalDate.now(), LocalDate.now(), "WAITING")
                    .getData().getContent()).extracting(InquiryController.AdminResponse::id).containsExactly(ids[2]);
            controller.answer(ids[2], new InquiryController.Answer("완료", null));
            assertThat(controller.adminList(0, 20, "축구", ids[0], LocalDate.now(), LocalDate.now(), "ANSWERED")
                    .getData().getContent()).extracting(InquiryController.AdminResponse::id).containsExactly(ids[2]);
        } finally {
            tx.executeWithoutResult(status -> {
                inquiries.deleteById(ids[2]); inquiries.deleteById(ids[3]); inquiries.flush();
                users.deleteById(ids[0]); users.deleteById(ids[1]);
            });
        }
    }
}
