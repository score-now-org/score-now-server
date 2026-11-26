package com.scorenow.scorenow_api.global.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import db.migration.SchemaMigration;

@Configuration(proxyBeanMethods = false)
public class AdminFeatureMigrations {
    @Bean
    JavaMigration existingChatUserMigration() { return new V20261125_01__PreserveChatUserHistory(); }

    @Bean @ConditionalOnClass(name = "com.scorenow.scorenow_api.domain.inquiry.InquiryAttachment")
    JavaMigration inquiryMigration() { return new V20261126_01__InquiryColumns(); }

    @Bean @ConditionalOnClass(name = "com.scorenow.scorenow_api.domain.admin.member.entity.MemberModerationEvent")
    JavaMigration memberMigration() { return new V20261126_02__MemberModeration(); }

    @Bean @ConditionalOnClass(name = "com.scorenow.scorenow_api.domain.chat.entity.ChatMessageReport")
    JavaMigration chatMigration() { return new V20261126_03__ChatModeration(); }

    @Bean @ConditionalOnClass(name = "com.scorenow.scorenow_api.domain.admin.forbiddenword.ForbiddenWord")
    JavaMigration forbiddenWordMigration() { return new V20261126_04__ForbiddenWords(); }

    @Bean @ConditionalOnClass(name = "com.scorenow.scorenow_api.domain.admin.version.AppVersionRelease")
    JavaMigration versionMigration() { return new V20261126_05__AppVersionReleases(); }

    @Bean @ConditionalOnClass(name = "com.scorenow.scorenow_api.domain.help.HelpDocument")
    JavaMigration helpMigration() { return new V20261126_06__HelpDocuments(); }

    @Bean @ConditionalOnClass(name = "com.scorenow.scorenow_api.domain.notice.Notice")
    JavaMigration noticeMigration() { return new V20261126_07__NoticeManagement(); }

    @Bean @ConditionalOnClass(name = "com.scorenow.scorenow_api.domain.notice.NewsTickerRequest")
    JavaMigration newsMigration() { return new V20261126_08__NewsTickerFields(); }

    @Bean @ConditionalOnClass(name = "com.scorenow.scorenow_api.domain.terms.TermsRevision")
    JavaMigration termsMigration() { return new V20261126_09__TermsManagement(); }

    public static class V20261125_01__PreserveChatUserHistory extends BaseJavaMigration {
        @Override public void migrate(Context context) throws Exception {
            SchemaMigration.preserveChatUserHistory(context.getConnection());
        }
    }

    public static class V20261126_01__InquiryColumns extends BaseJavaMigration {
        @Override public void migrate(Context context) throws Exception {
            Connection c = context.getConnection();
            SchemaMigration.addColumn(c, "inquiries", "answer_html", "TEXT NULL");
            SchemaMigration.addColumn(c, "inquiries", "attachment_name", "VARCHAR(180) NULL");
        }
    }

    public static class V20261126_02__MemberModeration extends BaseJavaMigration {
        @Override public void migrate(Context context) throws Exception {
            Connection c = context.getConnection();
            SchemaMigration.allowPermanentWithdrawal(c);
            SchemaMigration.addColumn(c, "users", "warning_reason", "VARCHAR(500) NULL");
            SchemaMigration.addColumn(c, "users", "warned_at", "DATETIME(6) NULL");
            SchemaMigration.addColumn(c, "users", "permanent_withdrawal_reason", "VARCHAR(500) NULL");
            SchemaMigration.addColumn(c, "users", "permanently_withdrawn_at", "DATETIME(6) NULL");
            SchemaMigration.addColumn(c, "users", "status_before_permanent_withdrawal",
                    "ENUM('ACTIVE','INACTIVE','BANNED','DELETED','PERMANENTLY_WITHDRAWN') NULL");
            SchemaMigration.addColumn(c, "users", "token_version", "BIGINT NULL");
            SchemaMigration.execute(c, """
                    CREATE TABLE IF NOT EXISTS member_moderation_events (
                      id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
                      action VARCHAR(32) NOT NULL, reason VARCHAR(500) NULL, created_at DATETIME(6) NOT NULL,
                      INDEX idx_member_moderation_user (user_id, id),
                      CONSTRAINT fk_member_moderation_user FOREIGN KEY (user_id) REFERENCES users(id))
                    """);
            SchemaMigration.requireColumns(c, "member_moderation_events", "id", "user_id", "action", "reason", "created_at");
            SchemaMigration.addIndex(c, "member_moderation_events", "idx_member_moderation_user", "`user_id`, `id`", false);
            SchemaMigration.ensureForeignKey(c, "member_moderation_events", "user_id", "fk_member_moderation_user", "users", "RESTRICT");
        }
    }

    public static class V20261126_03__ChatModeration extends BaseJavaMigration {
        @Override public void migrate(Context context) throws Exception {
            Connection c = context.getConnection();
            SchemaMigration.addColumn(c, "chat_messages", "image_url", "VARCHAR(1000) NULL");
            SchemaMigration.addColumn(c, "chat_messages", "deleted_at", "DATETIME(6) NULL");
            SchemaMigration.addColumn(c, "chat_messages", "deletion_reason", "VARCHAR(500) NULL");
            SchemaMigration.addColumn(c, "chat_messages", "image_removed_at", "DATETIME(6) NULL");
            SchemaMigration.addColumn(c, "chat_messages", "image_deletion_reason", "VARCHAR(500) NULL");
            SchemaMigration.addColumn(c, "chat_messages", "report_count", "BIGINT NULL");
            SchemaMigration.addIndex(c, "chat_messages", "idx_chat_messages_report_count_id", "`report_count`, `id`", false);
            SchemaMigration.execute(c, """
                    CREATE TABLE IF NOT EXISTS chat_message_reports (
                      id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, message_id BIGINT NOT NULL,
                      reporter_id BIGINT NULL, reason VARCHAR(500) NOT NULL, created_at DATETIME(6) NOT NULL,
                      CONSTRAINT uk_chat_report_user UNIQUE (message_id, reporter_id),
                      INDEX idx_chat_report_message (message_id),
                      CONSTRAINT fk_chat_report_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
                      CONSTRAINT fk_chat_report_user FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE SET NULL)
                    """);
            SchemaMigration.requireColumns(c, "chat_message_reports", "id", "message_id", "reporter_id", "reason", "created_at");
            SchemaMigration.addIndex(c, "chat_message_reports", "uk_chat_report_user", "`message_id`, `reporter_id`", true);
            SchemaMigration.addIndex(c, "chat_message_reports", "idx_chat_report_message", "`message_id`", false);
            SchemaMigration.ensureForeignKey(c, "chat_message_reports", "message_id", "fk_chat_report_message", "chat_messages", "CASCADE");
            SchemaMigration.ensureForeignKey(c, "chat_message_reports", "reporter_id", "fk_chat_report_user", "users", "SET NULL");
        }
    }

    public static class V20261126_04__ForbiddenWords extends BaseJavaMigration {
        @Override public void migrate(Context context) throws Exception {
            Connection c = context.getConnection();
            SchemaMigration.execute(c, """
                    CREATE TABLE IF NOT EXISTS forbidden_words (
                      id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, word VARCHAR(100) NOT NULL,
                      replacement VARCHAR(100) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
                      CONSTRAINT uk_forbidden_word UNIQUE (word))
                    """);
            SchemaMigration.requireColumns(c, "forbidden_words", "id", "word", "replacement", "created_at", "updated_at");
            SchemaMigration.addIndex(c, "forbidden_words", "uk_forbidden_word", "`word`", true);
        }
    }

    public static class V20261126_05__AppVersionReleases extends BaseJavaMigration {
        @Override public void migrate(Context context) throws Exception {
            Connection c = context.getConnection();
            SchemaMigration.execute(c, """
                    CREATE TABLE IF NOT EXISTS app_version_releases (
                      id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, version_name VARCHAR(40) NOT NULL,
                      platform VARCHAR(16) NOT NULL, release_date DATE NOT NULL, content VARCHAR(3000) NOT NULL,
                      INDEX idx_app_version_release_date (release_date, id))
                    """);
            SchemaMigration.requireColumns(c, "app_version_releases", "id", "version_name", "platform", "release_date", "content");
            SchemaMigration.addIndex(c, "app_version_releases", "idx_app_version_release_date", "`release_date`, `id`", false);
        }
    }

    public static class V20261126_06__HelpDocuments extends BaseJavaMigration {
        @Override public void migrate(Context context) throws Exception {
            Connection c = context.getConnection();
            SchemaMigration.execute(c, """
                    CREATE TABLE IF NOT EXISTS help_documents (
                      id BIGINT NOT NULL PRIMARY KEY, version BIGINT NOT NULL,
                      draft_json MEDIUMTEXT NOT NULL, published_json MEDIUMTEXT NOT NULL)
                    """);
            SchemaMigration.requireColumns(c, "help_documents", "id", "version", "draft_json", "published_json");
        }
    }

    public static class V20261126_07__NoticeManagement extends BaseJavaMigration {
        @Override public void migrate(Context context) throws Exception {
            Connection c = context.getConnection();
            SchemaMigration.execute(c, """
                    CREATE TABLE IF NOT EXISTS notices (
                      id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, created_at DATETIME(6), updated_at DATETIME(6),
                      title VARCHAR(200) NOT NULL, content TEXT NOT NULL, ticker BIT NOT NULL, visible BIT NOT NULL,
                      platform VARCHAR(10) NOT NULL, sort_order INT NOT NULL, url VARCHAR(2048),
                      deleted BIT NOT NULL, modified BIT NOT NULL, published BIT NOT NULL,
                      published_title VARCHAR(200), published_content TEXT, published_ticker BIT NOT NULL,
                      published_platform VARCHAR(10), published_sort_order INT NOT NULL, published_url VARCHAR(2048))
                    """);
            SchemaMigration.requireColumns(c, "notices", "id", "created_at", "updated_at", "title", "content", "ticker",
                    "visible", "platform", "sort_order", "url", "deleted", "modified", "published", "published_title",
                    "published_content", "published_ticker", "published_platform", "published_sort_order", "published_url");
            SchemaMigration.execute(c, "CREATE TABLE IF NOT EXISTS notice_edit_lock (id BIGINT NOT NULL PRIMARY KEY)");
            SchemaMigration.requireColumns(c, "notice_edit_lock", "id");
            SchemaMigration.execute(c, "INSERT INTO notice_edit_lock (id) VALUES (1) ON DUPLICATE KEY UPDATE id = id");
        }
    }

    public static class V20261126_08__NewsTickerFields extends BaseJavaMigration {
        @Override public void migrate(Context context) throws Exception {
            Connection c = context.getConnection();
            SchemaMigration.requireColumns(c, "notices", "id");
            addKind(c);
            SchemaMigration.addColumn(c, "notices", "title_color", "VARCHAR(7) NULL");
            SchemaMigration.addColumn(c, "notices", "content_color", "VARCHAR(7) NULL");
            SchemaMigration.addColumn(c, "notices", "published_title_color", "VARCHAR(7) NULL");
            SchemaMigration.addColumn(c, "notices", "published_content_color", "VARCHAR(7) NULL");
        }

        private void addKind(Connection c) throws SQLException {
            if (SchemaMigration.columnExists(c, "notices", "kind")) {
                try (PreparedStatement statement = c.prepareStatement(
                        "SELECT DATA_TYPE, COLUMN_TYPE, IS_NULLABLE FROM information_schema.columns "
                                + "WHERE table_schema = DATABASE() AND table_name = 'notices' AND column_name = 'kind'")) {
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next() && "enum".equalsIgnoreCase(result.getString("DATA_TYPE"))) {
                            String type = result.getString("COLUMN_TYPE");
                            if (!type.contains("'NOTICE'") || !type.contains("'NEWS'") || !"NO".equals(result.getString("IS_NULLABLE"))) {
                                throw new SQLException("기존 notices.kind의 enum 정의가 예상과 다릅니다.");
                            }
                            return;
                        }
                    }
                }
            }
            SchemaMigration.addColumn(c, "notices", "kind", "VARCHAR(10) NOT NULL DEFAULT 'NOTICE'");
        }
    }

    public static class V20261126_09__TermsManagement extends BaseJavaMigration {
        @Override public void migrate(Context context) throws Exception {
            Connection c = context.getConnection();
            SchemaMigration.execute(c, """
                    CREATE TABLE IF NOT EXISTS terms_revisions (
                      id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, content MEDIUMTEXT NOT NULL, published_at DATETIME(6) NOT NULL)
                    """);
            SchemaMigration.requireColumns(c, "terms_revisions", "id", "content", "published_at");
            SchemaMigration.execute(c, "CREATE TABLE IF NOT EXISTS terms_edit_lock (id BIGINT NOT NULL PRIMARY KEY)");
            SchemaMigration.requireColumns(c, "terms_edit_lock", "id");
            SchemaMigration.execute(c, "INSERT INTO terms_edit_lock (id) VALUES (1) ON DUPLICATE KEY UPDATE id = id");
        }
    }
}
