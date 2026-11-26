package com.scorenow.scorenow_api.global.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.zaxxer.hikari.HikariDataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMigrationConfiguration {
    @Bean(destroyMethod = "close")
    MigrationBootstrap migrationBootstrap() {
        return new MigrationBootstrap();
    }

    @Bean
    FlywayMigrationStrategy migrationStrategy(MigrationBootstrap bootstrap) {
        return bootstrap::start;
    }

    @Bean
    @DependsOn("entityManagerFactory")
    InitializingBean finishDatabaseBootstrap(MigrationBootstrap bootstrap, Flyway flyway) {
        return () -> bootstrap.finish(flyway);
    }

    /** Serializes Hibernate's existing schema bootstrap with Flyway on multiple instances. */
    public static final class MigrationBootstrap implements AutoCloseable {
        private Connection lockConnection;
        private String lockName;
        private boolean locked;
        private boolean deferred;

        public void start(Flyway flyway) {
            try {
                DataSource dataSource = flyway.getConfiguration().getDataSource();
                HikariDataSource hikari = dataSource instanceof HikariDataSource direct ? direct
                        : dataSource.isWrapperFor(HikariDataSource.class) ? dataSource.unwrap(HikariDataSource.class) : null;
                if (hikari != null && hikari.getMaximumPoolSize() == 1) {
                    throw new IllegalStateException("DB 마이그레이션에는 연결 풀 크기가 최소 2개 필요합니다.");
                }
                lockConnection = dataSource.getConnection();
                if (!"MySQL".equalsIgnoreCase(lockConnection.getMetaData().getDatabaseProductName())) {
                    throw new IllegalStateException("관리자 마이그레이션은 MySQL에서만 실행할 수 있습니다.");
                }
                String database = lockConnection.getCatalog();
                if (database == null || database.isBlank()) throw new IllegalStateException("MySQL 데이터베이스가 지정되지 않았습니다.");
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(database.getBytes(StandardCharsets.UTF_8));
                lockName = "scorenow-schema-" + HexFormat.of().formatHex(hash, 0, 16);
                try (PreparedStatement statement = lockConnection.prepareStatement("SELECT GET_LOCK(?, 60)")) {
                    statement.setString(1, lockName);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next() || result.getInt(1) != 1) throw new IllegalStateException("DB 마이그레이션 잠금을 얻지 못했습니다.");
                        locked = true;
                    }
                }
                int coreTables;
                try (PreparedStatement statement = lockConnection.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                                + "AND table_name IN ('users', 'chat_messages', 'inquiries')")) {
                    try (ResultSet result = statement.executeQuery()) {
                        result.next();
                        coreTables = result.getInt(1);
                    }
                }
                if (coreTables != 0 && coreTables != 3) {
                    throw new IllegalStateException("기존 DB의 users, chat_messages, inquiries 테이블을 모두 확인해야 합니다.");
                }
                deferred = coreTables == 0;
                if (deferred) {
                    try (PreparedStatement statement = lockConnection.prepareStatement(
                            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                                    + "AND table_type = 'BASE TABLE' AND UPPER(table_name) NOT IN ("
                                    + "'BATCH_JOB_INSTANCE', 'BATCH_JOB_EXECUTION', 'BATCH_JOB_EXECUTION_PARAMS', "
                                    + "'BATCH_STEP_EXECUTION', 'BATCH_STEP_EXECUTION_CONTEXT', 'BATCH_JOB_EXECUTION_CONTEXT', "
                                    + "'BATCH_STEP_EXECUTION_SEQ', 'BATCH_JOB_EXECUTION_SEQ', 'BATCH_JOB_SEQ')")) {
                        try (ResultSet result = statement.executeQuery()) {
                            result.next();
                            if (result.getInt(1) != 0) throw new IllegalStateException("기존 스키마가 예상과 달라 자동 초기화를 중단했습니다.");
                        }
                    }
                }
                if (!deferred) flyway.migrate();
            } catch (Exception failure) {
                close();
                throw new IllegalStateException("DB 마이그레이션 준비에 실패했습니다.", failure);
            }
        }

        public void finish(Flyway flyway) {
            try {
                if (deferred) flyway.migrate();
            } finally {
                close();
            }
        }

        @Override
        public void close() {
            if (lockConnection == null) return;
            Connection connection = lockConnection;
            try {
                if (locked && !connection.isClosed()) {
                    try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                        statement.setString(1, lockName);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next() || result.getInt(1) != 1) throw new SQLException("DB 마이그레이션 잠금 해제 실패");
                        }
                    }
                }
            } catch (SQLException ignored) {
                // A pooled close may retain the physical connection and its advisory lock.
                try { connection.abort(Runnable::run); } catch (SQLException ignoredAbort) { }
            } finally {
                try { connection.close(); } catch (SQLException ignored) { }
                lockConnection = null;
                lockName = null;
                locked = false;
            }
        }
    }
}
