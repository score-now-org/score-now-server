package com.scorenow.migrationtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.scorenow.scorenow_api.ScorenowApiApplication;

@EnabledIfEnvironmentVariable(named = "ADMIN_MIGRATION_DISPOSABLE_MYSQL", matches = "true")
@SpringBootTest(classes = ScorenowApiApplication.class, properties = {
        "spring.flyway.enabled=true", "spring.flyway.baseline-on-migrate=true", "spring.flyway.baseline-version=0",
        "spring.flyway.out-of-order=true", "spring.jpa.hibernate.ddl-auto=update", "spring.batch.jdbc.initialize-schema=always"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApplicationMigrationMySqlTest {
    private static final String ROOT_URL = "jdbc:mysql://127.0.0.1:13308/";
    private static final String SCHEMA = "scorenow_migration_test_" + UUID.randomUUID().toString().replace("-", "");
    private static boolean created;

    @DynamicPropertySource
    static void disposableDatabase(DynamicPropertyRegistry properties) throws Exception {
        try (Connection connection = DriverManager.getConnection(ROOT_URL, "root", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + SCHEMA + "`");
            created = true;
        }
        properties.add("spring.datasource.url", () -> ROOT_URL + SCHEMA);
        properties.add("spring.datasource.username", () -> "root");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @AfterAll
    static void removeDisposableDatabase() throws Exception {
        if (!created || !SCHEMA.matches("scorenow_migration_test_[0-9a-f]{32}")) return;
        try (Connection connection = DriverManager.getConnection(ROOT_URL, "root", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE `" + SCHEMA + "`");
        }
    }

    @Autowired DataSource dataSource;

    @Test
    void realApplicationStartsOnFreshMySqlWithMigrationHistory() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'SQL'")) {
            result.next();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables "
                     + "WHERE table_schema = DATABASE() AND table_name = 'BATCH_JOB_INSTANCE'")) {
            result.next();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
    }
}
