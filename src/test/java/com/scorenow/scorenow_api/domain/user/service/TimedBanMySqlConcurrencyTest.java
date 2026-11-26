package com.scorenow.scorenow_api.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "ADMIN_MIGRATION_DISPOSABLE_MYSQL", matches = "true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TimedBanMySqlConcurrencyTest {
    private static final String ROOT_URL = "jdbc:mysql://127.0.0.1:13308/";
    private static final String SCHEMA = "scorenow_timed_ban_test_" + UUID.randomUUID().toString().replace("-", "");
    private static boolean created;

    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactionManager;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    @MockBean JPAQueryFactory queryFactory;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        try (var connection = DriverManager.getConnection(ROOT_URL, "root", ""); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + SCHEMA + "`");
            created = true;
        }
        properties.add("spring.datasource.url", () -> ROOT_URL + SCHEMA);
        properties.add("spring.datasource.username", () -> "root");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @AfterAll
    static void removeDatabase() throws Exception {
        if (!created || !SCHEMA.matches("scorenow_timed_ban_test_[0-9a-f]{32}")) return;
        try (var connection = DriverManager.getConnection(ROOT_URL, "root", ""); var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE `" + SCHEMA + "`");
        }
    }

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousNewIdentitiesCanInsertAfterBothLookups() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var barrier = new CyclicBarrier(2);
        var tx = new TransactionTemplate(transactionManager);
        try {
            var first = executor.submit(() -> createUser(tx, barrier, "first"));
            var second = executor.submit(() -> createUser(tx, barrier, "second"));
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
            assertThat(users.findIdByProviderAndSocialId("test", "first")).isPresent();
            assertThat(users.findIdByProviderAndSocialId("test", "second")).isPresent();
        } finally {
            executor.shutdownNow();
        }
    }

    private void createUser(TransactionTemplate tx, CyclicBarrier barrier, String id) {
        tx.executeWithoutResult(status -> {
            assertThat(users.findIdByProviderAndSocialId("test", id)).isEmpty();
            try { barrier.await(5, TimeUnit.SECONDS); }
            catch (Exception e) { throw new IllegalStateException(e); }
            users.saveAndFlush(User.builder().provider("test").socialId(id).build());
        });
    }
}
