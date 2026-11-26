package com.scorenow.migrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import com.scorenow.scorenow_api.global.config.DatabaseMigrationConfiguration;
import com.scorenow.scorenow_api.global.config.DatabaseMigrationConfiguration.MigrationBootstrap;
import com.scorenow.scorenow_api.global.config.AdminFeatureMigrations;
import com.zaxxer.hikari.HikariDataSource;

import db.migration.SchemaMigration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@EnabledIfEnvironmentVariable(named = "ADMIN_MIGRATION_DISPOSABLE_MYSQL", matches = "true")
class DatabaseMigrationMySqlTest {
    private static final String ROOT_URL = "jdbc:mysql://127.0.0.1:13308/";
    private String schema;
    private DataSource dataSource;

    @BeforeEach
    void createDisposableSchema() throws Exception {
        schema = "scorenow_migration_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(ROOT_URL, "root", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "`");
        }
        dataSource = new DriverManagerDataSource(ROOT_URL + schema, "root", "");
    }

    @AfterEach
    void dropDisposableSchema() throws Exception {
        if (schema == null || !schema.matches("scorenow_migration_test_[0-9a-f]{32}")) return;
        try (Connection connection = DriverManager.getConnection(ROOT_URL, "root", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE `" + schema + "`");
        }
    }

    @Test
    void existingDatabaseIsBaselinedOnceAndKeepsData() throws Exception {
        createCoreTables();
        execute("INSERT INTO users (id) VALUES (42)");
        Flyway flyway = flyway();
        try (MigrationBootstrap bootstrap = new MigrationBootstrap()) {
            bootstrap.start(flyway);
            bootstrap.finish(flyway);
        }
        try (MigrationBootstrap bootstrap = new MigrationBootstrap()) {
            bootstrap.start(flyway);
            bootstrap.finish(flyway);
        }
        assertThat(scalar("SELECT COUNT(*) FROM users WHERE id = 42")).isEqualTo(1);
        assertThat(scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'SQL'")).isEqualTo(1);
    }

    @Test
    void emptyDatabaseMigratesAfterHibernateCreatesTheCoreTables() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                        HibernateJpaAutoConfiguration.class, FlywayAutoConfiguration.class,
                        TransactionAutoConfiguration.class))
                .withUserConfiguration(FixtureJpaConfiguration.class, DatabaseMigrationConfiguration.class, AdminFeatureMigrations.class)
                .withPropertyValues("spring.datasource.url=" + ROOT_URL + schema,
                        "spring.datasource.username=root", "spring.datasource.password=",
                        "spring.jpa.hibernate.ddl-auto=update", "spring.flyway.enabled=true",
                        "spring.flyway.baseline-on-migrate=true", "spring.flyway.baseline-version=0")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'SQL'")).isEqualTo(1);
                    assertThat(scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                            + "AND table_name IN ('users', 'chat_messages', 'inquiries')")).isEqualTo(3);
                    long enabledFeatures = Arrays.stream(FEATURE_MARKERS)
                            .filter(name -> org.springframework.util.ClassUtils.isPresent(name, getClass().getClassLoader())).count();
                    assertThat(context.getBeansOfType(JavaMigration.class)).hasSize((int) enabledFeatures);
                    assertThat(scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'JDBC'"))
                            .isEqualTo(enabledFeatures);
                });
    }

    @Test
    void incompleteExistingSchemaFailsAndReleasesTheLock() throws Exception {
        execute("CREATE TABLE users (id BIGINT PRIMARY KEY, status ENUM('ACTIVE','DELETED') NULL)");
        try (MigrationBootstrap bootstrap = new MigrationBootstrap()) {
            assertThrows(IllegalStateException.class, () -> bootstrap.start(flyway()));
        }
        execute("CREATE TABLE chat_messages (id BIGINT PRIMARY KEY)");
        execute("CREATE TABLE inquiries (id BIGINT PRIMARY KEY)");
        try (MigrationBootstrap retry = new MigrationBootstrap()) {
            retry.start(flyway());
            retry.finish(flyway());
        }
        assertThat(scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1")).isGreaterThan(0);
    }

    @Test
    void unrelatedNonemptyDatabaseIsNotTreatedAsANewInstallation() throws Exception {
        execute("CREATE TABLE batch_unrelated_data (id BIGINT PRIMARY KEY)");
        execute("INSERT INTO batch_unrelated_data (id) VALUES (7)");
        try (MigrationBootstrap bootstrap = new MigrationBootstrap()) {
            assertThrows(IllegalStateException.class, () -> bootstrap.start(flyway()));
        }
        assertThat(scalar("SELECT COUNT(*) FROM batch_unrelated_data WHERE id = 7")).isEqualTo(1);
    }

    @Test
    void additiveOperationsKeepExistingRowsAndStatusColumnAttributes() throws Exception {
        execute("CREATE TABLE users (id BIGINT PRIMARY KEY, status ENUM('ACTIVE','DELETED') "
                + "CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'ACTIVE' COMMENT 'original')");
        execute("INSERT INTO users (id, status) VALUES (42, 'DELETED')");
        try (Connection connection = dataSource.getConnection()) {
            SchemaMigration.allowPermanentWithdrawal(connection);
            SchemaMigration.allowPermanentWithdrawal(connection);
            SchemaMigration.addColumn(connection, "users", "warning_reason", "VARCHAR(500) NULL");
            SchemaMigration.addColumn(connection, "users", "warning_reason", "VARCHAR(500) NULL");
            SchemaMigration.addIndex(connection, "users", "idx_migration_test", "`warning_reason`", false);
            SchemaMigration.addIndex(connection, "users", "idx_migration_test", "`warning_reason`", false);
        }
        assertThat(scalar("SELECT COUNT(*) FROM users WHERE id = 42 AND status = 'DELETED'")).isEqualTo(1);
        assertThat(scalar("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'users' AND column_name = 'status' "
                + "AND column_type LIKE '%PERMANENTLY_WITHDRAWN%' AND is_nullable = 'NO' "
                + "AND column_default = 'ACTIVE' AND collation_name = 'utf8mb4_bin' AND column_comment = 'original'"))
                .isEqualTo(1);
    }

    @Test
    void incompatibleExistingColumnStopsInsteadOfSilentlyAcceptingIt() throws Exception {
        execute("CREATE TABLE users (id BIGINT PRIMARY KEY, warning_reason VARCHAR(5) NULL)");
        execute("INSERT INTO users (id, warning_reason) VALUES (42, 'keep')");
        try (Connection connection = dataSource.getConnection()) {
            assertThrows(java.sql.SQLException.class,
                    () -> SchemaMigration.addColumn(connection, "users", "warning_reason", "VARCHAR(500) NULL"));
        }
        assertThat(scalar("SELECT COUNT(*) FROM users WHERE id = 42 AND warning_reason = 'keep'")).isEqualTo(1);
    }

    @Test
    void shortLegacyStatusColumnIsExpandedWithoutLosingDataOrDefault() throws Exception {
        execute("CREATE TABLE users (id BIGINT PRIMARY KEY, status VARCHAR(6) NOT NULL DEFAULT 'ACTIVE')");
        execute("INSERT INTO users (id, status) VALUES (42, 'ACTIVE')");
        try (Connection connection = dataSource.getConnection()) {
            SchemaMigration.allowPermanentWithdrawal(connection);
        }
        assertThat(scalar("SELECT COUNT(*) FROM users WHERE id = 42 AND status = 'ACTIVE'")).isEqualTo(1);
        assertThat(scalar("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'users' AND column_name = 'status' AND character_maximum_length >= 21 "
                + "AND is_nullable = 'NO' AND column_default = 'ACTIVE'")).isEqualTo(1);
    }

    @Test
    void nonCanonicalExistingStatusStopsWithoutChangingTheData() throws Exception {
        execute("CREATE TABLE users (id BIGINT PRIMARY KEY, status VARCHAR(30) NULL)");
        execute("INSERT INTO users (id, status) VALUES (42, 'active')");
        try (Connection connection = dataSource.getConnection()) {
            assertThrows(java.sql.SQLException.class, () -> SchemaMigration.allowPermanentWithdrawal(connection));
        }
        assertThat(scalar("SELECT COUNT(*) FROM users WHERE id = 42 AND status = 'active'")).isEqualTo(1);
    }

    @Test
    void conflictingExistingForeignKeyStopsWithoutReplacingIt() throws Exception {
        execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
        execute("CREATE TABLE member_moderation_events (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, "
                + "CONSTRAINT existing_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
        try (Connection connection = dataSource.getConnection()) {
            assertThrows(java.sql.SQLException.class, () -> SchemaMigration.ensureForeignKey(connection,
                    "member_moderation_events", "user_id", "fk_member_moderation_user", "users", "RESTRICT"));
        }
        assertThat(scalar("SELECT COUNT(*) FROM information_schema.referential_constraints "
                + "WHERE constraint_schema = DATABASE() AND constraint_name = 'existing_fk' AND delete_rule = 'CASCADE'"))
                .isEqualTo(1);
    }

    @Test
    void singleConnectionPoolFailsImmediatelyInsteadOfWaitingForItself() {
        try (HikariDataSource pool = new HikariDataSource(); MigrationBootstrap bootstrap = new MigrationBootstrap()) {
            pool.setJdbcUrl(ROOT_URL + schema);
            pool.setUsername("root");
            pool.setPassword("");
            pool.setMaximumPoolSize(1);
            Flyway flyway = Flyway.configure().dataSource(pool).baselineOnMigrate(true).baselineVersion("0").load();
            IllegalStateException error = assertThrows(IllegalStateException.class, () -> bootstrap.start(flyway));
            assertThat(error.getCause()).hasMessageContaining("최소 2개");
        }
    }

    @Test
    void proxiedSingleConnectionPoolAlsoFailsImmediately() {
        try (HikariDataSource pool = new HikariDataSource(); MigrationBootstrap bootstrap = new MigrationBootstrap()) {
            pool.setJdbcUrl(ROOT_URL + schema);
            pool.setUsername("root");
            pool.setPassword("");
            pool.setMaximumPoolSize(1);
            DataSource proxy = new LazyConnectionDataSourceProxy(pool);
            Flyway flyway = Flyway.configure().dataSource(proxy).baselineOnMigrate(true).baselineVersion("0").load();
            IllegalStateException error = assertThrows(IllegalStateException.class, () -> bootstrap.start(flyway));
            assertThat(error.getCause()).hasMessageContaining("최소 2개");
        }
    }

    @Test
    void twoConnectionPoolCanRunMySqlMigrationWhileHoldingBootstrapLock() throws Exception {
        createCoreTables();
        try (HikariDataSource pool = new HikariDataSource(); MigrationBootstrap bootstrap = new MigrationBootstrap()) {
            pool.setJdbcUrl(ROOT_URL + schema);
            pool.setUsername("root");
            pool.setPassword("");
            pool.setMaximumPoolSize(2);
            pool.setConnectionTimeout(3000);
            Flyway flyway = Flyway.configure().dataSource(pool).baselineOnMigrate(true).baselineVersion("0")
                    .javaMigrations(allFeatureMigrations()).load();
            bootstrap.start(flyway);
            bootstrap.finish(flyway);
        }
        assertThat(scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'JDBC'")).isEqualTo(9);
    }

    @Test
    void featureMigrationsPreserveExistingDataAndWorkOutOfOrderAndWithoutHistory() throws Exception {
        createCoreTables();
        execute("INSERT INTO users (id, status) VALUES (42, 'ACTIVE')");
        execute("INSERT INTO inquiries (id) VALUES (11)");
        execute("INSERT INTO chat_messages (id) VALUES (12)");
        JavaMigration inquiry = new AdminFeatureMigrations.V20261126_01__InquiryColumns();
        JavaMigration notice = new AdminFeatureMigrations.V20261126_07__NoticeManagement();
        run(flyway(inquiry, notice));
        execute("UPDATE inquiries SET answer_html = '<p>existing</p>' WHERE id = 11");
        execute("INSERT INTO notices (id, title, content, ticker, visible, platform, sort_order, deleted, modified, "
                + "published, published_ticker, published_sort_order) VALUES (8, 'existing', 'body', b'0', b'1', 'ALL', 1, b'0', b'0', b'1', b'0', 1)");
        execute("ALTER TABLE users ADD COLUMN warning_reason VARCHAR(500) NULL");
        execute("UPDATE users SET warning_reason = 'keep' WHERE id = 42");
        execute("CREATE TABLE member_moderation_events (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "user_id BIGINT NOT NULL, action VARCHAR(32) NOT NULL, reason VARCHAR(500) NULL, created_at DATETIME(6) NOT NULL)");
        execute("INSERT INTO member_moderation_events (user_id, action, created_at) VALUES (42, 'WARN', NOW())");

        JavaMigration[] migrations = allFeatureMigrations();
        run(flyway(migrations));
        assertFeatureDataAndSchema();
        assertThat(scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'JDBC'")).isEqualTo(9);

        // An earlier manual deployment may have the schema but no Flyway history.
        execute("DROP TABLE flyway_schema_history");
        run(flyway(allFeatureMigrations()));
        assertFeatureDataAndSchema();
        assertThat(scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'JDBC'")).isEqualTo(9);
    }

    @Test
    void concurrentInstancesWaitUntilTheFirstBootstrapFinishes() throws Exception {
        createCoreTables();
        Flyway flyway = flyway();
        var executor = Executors.newSingleThreadExecutor();
        try (MigrationBootstrap first = new MigrationBootstrap()) {
            first.start(flyway);
            var second = executor.submit(() -> {
                try (MigrationBootstrap bootstrap = new MigrationBootstrap()) {
                    bootstrap.start(flyway());
                    bootstrap.finish(flyway());
                }
            });
            assertThrows(TimeoutException.class, () -> second.get(300, TimeUnit.MILLISECONDS));
            first.finish(flyway);
            second.get(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertThat(scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'SQL'")).isEqualTo(1);
    }

    private static final String[] FEATURE_MARKERS = {
            "com.scorenow.scorenow_api.domain.inquiry.InquiryAttachment",
            "com.scorenow.scorenow_api.domain.admin.member.entity.MemberModerationEvent",
            "com.scorenow.scorenow_api.domain.chat.entity.ChatMessageReport",
            "com.scorenow.scorenow_api.domain.admin.forbiddenword.ForbiddenWord",
            "com.scorenow.scorenow_api.domain.admin.version.AppVersionRelease",
            "com.scorenow.scorenow_api.domain.help.HelpDocument",
            "com.scorenow.scorenow_api.domain.notice.Notice",
            "com.scorenow.scorenow_api.domain.notice.NewsTickerRequest",
            "com.scorenow.scorenow_api.domain.terms.TermsRevision"
    };

    private Flyway flyway(JavaMigration... migrations) {
        return Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).baselineVersion("0")
                .outOfOrder(true).javaMigrations(migrations).load();
    }

    private void run(Flyway flyway) {
        try (MigrationBootstrap bootstrap = new MigrationBootstrap()) {
            bootstrap.start(flyway);
            bootstrap.finish(flyway);
        }
    }

    private JavaMigration[] allFeatureMigrations() {
        return new JavaMigration[] {
                new AdminFeatureMigrations.V20261126_01__InquiryColumns(),
                new AdminFeatureMigrations.V20261126_02__MemberModeration(),
                new AdminFeatureMigrations.V20261126_03__ChatModeration(),
                new AdminFeatureMigrations.V20261126_04__ForbiddenWords(),
                new AdminFeatureMigrations.V20261126_05__AppVersionReleases(),
                new AdminFeatureMigrations.V20261126_06__HelpDocuments(),
                new AdminFeatureMigrations.V20261126_07__NoticeManagement(),
                new AdminFeatureMigrations.V20261126_08__NewsTickerFields(),
                new AdminFeatureMigrations.V20261126_09__TermsManagement()
        };
    }

    private void assertFeatureDataAndSchema() throws Exception {
        assertThat(scalar("SELECT COUNT(*) FROM users WHERE id = 42 AND status = 'ACTIVE' AND warning_reason = 'keep'")).isEqualTo(1);
        assertThat(scalar("SELECT COUNT(*) FROM member_moderation_events WHERE user_id = 42 AND action = 'WARN'")).isEqualTo(1);
        assertThat(scalar("SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema = DATABASE() "
                + "AND table_name = 'member_moderation_events' AND column_name = 'user_id' AND referenced_table_name = 'users'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT COUNT(*) FROM inquiries WHERE id = 11 AND answer_html = '<p>existing</p>'")).isEqualTo(1);
        assertThat(scalar("SELECT COUNT(*) FROM notices WHERE id = 8 AND title = 'existing' AND kind = 'NOTICE'")).isEqualTo(1);
        assertThat(scalar("SELECT COUNT(*) FROM notice_edit_lock WHERE id = 1")).isEqualTo(1);
        assertThat(scalar("SELECT COUNT(*) FROM terms_edit_lock WHERE id = 1")).isEqualTo(1);
        assertThat(scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                + "AND table_name IN ('member_moderation_events', 'chat_message_reports', 'forbidden_words', "
                + "'app_version_releases', 'help_documents', 'terms_revisions')")).isEqualTo(6);
    }

    private void createCoreTables() throws Exception {
        execute("CREATE TABLE users (id BIGINT PRIMARY KEY, status ENUM('ACTIVE','INACTIVE','BANNED','DELETED') NULL)");
        execute("CREATE TABLE chat_messages (id BIGINT PRIMARY KEY)");
        execute("CREATE TABLE inquiries (id BIGINT PRIMARY KEY)");
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long scalar(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = FixtureUser.class)
    static class FixtureJpaConfiguration { }

    @Entity @Table(name = "users") static class FixtureUser {
        @Id Long id;
        @Column(columnDefinition = "ENUM('ACTIVE','INACTIVE','BANNED','DELETED')") String status;
    }
    @Entity @Table(name = "chat_messages") static class FixtureChat { @Id Long id; }
    @Entity @Table(name = "inquiries") static class FixtureInquiry { @Id Long id; }
}
