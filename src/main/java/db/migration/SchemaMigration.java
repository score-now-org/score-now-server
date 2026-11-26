package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared, additive MySQL operations for schemas previously updated by Hibernate. */
public final class SchemaMigration {
    private SchemaMigration() { }

    public static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    public static void addColumn(Connection connection, String table, String column, String definition) throws SQLException {
        if (!columnExists(connection, table, column)) {
            execute(connection, "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
        }
        validateColumn(connection, table, column, definition);
    }

    private static void validateColumn(Connection connection, String table, String column, String definition) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, CHARACTER_MAXIMUM_LENGTH, DATETIME_PRECISION "
                        + "FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("기존 " + table + "." + column + " 컬럼이 없습니다.");
                String expected = definition.toLowerCase(Locale.ROOT);
                String actual = result.getString("DATA_TYPE").toLowerCase(Locale.ROOT);
                String expectedType = expected.split("[ (]", 2)[0];
                boolean compatible = actual.equals(expectedType)
                        || ("text".equals(expectedType) && ("mediumtext".equals(actual) || "longtext".equals(actual)));
                Matcher length = Pattern.compile("^(?:varchar|char)\\((\\d+)\\)").matcher(expected);
                if (length.find()) compatible &= result.getLong("CHARACTER_MAXIMUM_LENGTH") >= Long.parseLong(length.group(1));
                Matcher precision = Pattern.compile("^datetime\\((\\d+)\\)").matcher(expected);
                if (precision.find()) compatible &= result.getInt("DATETIME_PRECISION") >= Integer.parseInt(precision.group(1));
                if (expected.startsWith("enum(")) {
                    Matcher value = Pattern.compile("'([^']*)'").matcher(expected);
                    String actualType = result.getString("COLUMN_TYPE").toLowerCase(Locale.ROOT);
                    while (value.find()) compatible &= actualType.contains("'" + value.group(1) + "'");
                }
                if (expected.endsWith(" null") && !expected.endsWith(" not null")) {
                    compatible &= "YES".equals(result.getString("IS_NULLABLE"));
                }
                if (expected.contains(" not null")) compatible &= "NO".equals(result.getString("IS_NULLABLE"));
                if (!compatible) throw new SQLException("기존 " + table + "." + column + " 컬럼 정의가 예상과 다릅니다.");
            }
        }
    }

    public static void requireColumns(Connection connection, String table, String... columns) throws SQLException {
        for (String column : columns) {
            if (!columnExists(connection, table, column)) {
                throw new SQLException("기존 " + table + " 테이블에 필요한 " + column + " 컬럼이 없습니다.");
            }
        }
    }

    public static void addIndex(Connection connection, String table, String name, String columns, boolean unique) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ','), MIN(non_unique) "
                        + "FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?")) {
            statement.setString(1, table);
            statement.setString(2, name);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                String existing = result.getString(1);
                if (existing != null) {
                    String expected = columns.replace("`", "").replace(" ", "");
                    if (!existing.equalsIgnoreCase(expected) || (result.getInt(2) == 0) != unique) {
                        throw new SQLException("기존 " + table + " 인덱스 " + name + "의 정의가 예상과 다릅니다.");
                    }
                    return;
                }
            }
        }
        execute(connection, "CREATE " + (unique ? "UNIQUE " : "") + "INDEX `" + name + "` ON `" + table + "` (" + columns + ")");
    }

    /** Add the new enum member without removing existing values, defaults or collation. */
    public static void allowPermanentWithdrawal(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet invalid = statement.executeQuery(
                "SELECT 1 FROM users WHERE status IS NOT NULL AND BINARY status NOT IN "
                        + "(BINARY 'ACTIVE', BINARY 'INACTIVE', BINARY 'BANNED', BINARY 'DELETED', BINARY 'PERMANENTLY_WITHDRAWN') LIMIT 1")) {
            if (invalid.next()) throw new SQLException("users.status에 지원하지 않는 값이 있어 마이그레이션을 중단했습니다.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, CHARACTER_SET_NAME, "
                        + "COLLATION_NAME, COLUMN_COMMENT, EXTRA, CHARACTER_MAXIMUM_LENGTH "
                        + "FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'status'")) {
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("기존 users.status 컬럼이 없습니다.");
                String dataType = result.getString("DATA_TYPE");
                String type = result.getString("COLUMN_TYPE");
                if ("varchar".equalsIgnoreCase(dataType) || "char".equalsIgnoreCase(dataType)) {
                    if (result.getLong("CHARACTER_MAXIMUM_LENGTH") < "PERMANENTLY_WITHDRAWN".length()) {
                        modifyStatus(connection, result, dataType + "(" + "PERMANENTLY_WITHDRAWN".length() + ")");
                    }
                    return;
                }
                if (!"enum".equalsIgnoreCase(dataType) || !type.endsWith(")")) {
                    throw new SQLException("지원하지 않는 users.status 타입: " + type);
                }
                if (type.contains("'PERMANENTLY_WITHDRAWN'")) return;
                String newType = type.substring(0, type.length() - 1) + ",'PERMANENTLY_WITHDRAWN')";
                modifyStatus(connection, result, newType);
            }
        }
    }

    private static void modifyStatus(Connection connection, ResultSet result, String newType) throws SQLException {
        if (!result.getString("EXTRA").isBlank()) throw new SQLException("users.status의 추가 속성을 수동 확인해야 합니다.");
        String charset = result.getString("CHARACTER_SET_NAME");
        String collation = result.getString("COLLATION_NAME");
        if (!charset.matches("[A-Za-z0-9_]+") || !collation.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("users.status 문자 집합을 확인할 수 없습니다.");
        }
        String sql = "ALTER TABLE users MODIFY COLUMN status " + newType + " CHARACTER SET " + charset
                + " COLLATE " + collation + ("YES".equals(result.getString("IS_NULLABLE")) ? " NULL" : " NOT NULL");
        String defaultValue = result.getString("COLUMN_DEFAULT");
        if (defaultValue != null) sql += " DEFAULT " + quote(connection, defaultValue);
        String comment = result.getString("COLUMN_COMMENT");
        if (comment != null && !comment.isEmpty()) sql += " COMMENT " + quote(connection, comment);
        execute(connection, sql);
    }

    private static String quote(Connection connection, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT QUOTE(?)")) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getString(1); }
        }
    }
}
