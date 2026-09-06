package com.example.chookjibupadmin.support.seed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-shot RDS light seed runner. Uses JDBC only (no psql/python on EC2).
 * Invoked by Gradle task {@code applyRdsSeed}.
 */
public final class RdsLightSeedRunner {

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "(?m)^\\s*(url|username|password):\\s*(.+?)\\s*$"
    );

    private RdsLightSeedRunner() {
    }

    public static void main(String[] args) throws Exception {
        Path secretFile = Path.of(required(
                firstNonBlank(
                        System.getProperty("seed.secretFile"),
                        System.getenv("SECRET_FILE")
                ),
                "seed.secretFile / SECRET_FILE"
        ));
        Path sqlFile = Path.of(required(
                firstNonBlank(
                        System.getProperty("seed.sqlFile"),
                        System.getenv("SEED_SQL")
                ),
                "seed.sqlFile / SEED_SQL"
        ));

        if (!Files.isRegularFile(secretFile)) {
            throw new IllegalStateException("Secret file not found: " + secretFile);
        }
        if (!Files.isRegularFile(sqlFile)) {
            throw new IllegalStateException("Seed SQL not found: " + sqlFile);
        }

        Map<String, String> datasource = readDatasource(secretFile);
        String jdbcUrl = datasource.get("url");
        String username = datasource.get("username");
        String password = datasource.get("password");

        System.out.println("Applying seed SQL via JDBC (password redacted)");
        System.out.println("jdbcUrl=" + jdbcUrl + " user=" + username);

        String script = Files.readString(sqlFile, StandardCharsets.UTF_8);
        List<String> statements = splitSql(script);
        System.out.println("Parsed SQL statements: " + statements.size());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            // One JDBC transaction: TEMP ... ON COMMIT DROP tables must survive until COMMIT.
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                int index = 0;
                for (String sql : statements) {
                    index++;
                    if (isTransactionBoundary(sql)) {
                        continue;
                    }
                    try {
                        statement.execute(sql);
                    } catch (SQLException ex) {
                        throw new SQLException(
                                "Seed statement #" + index + " failed: " + preview(sql),
                                ex
                        );
                    }
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }

            System.out.println("----- Seed verification -----");
            try (ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT id, email, account_kind, status "
                            + "FROM admin_accounts "
                            + "WHERE email = 'admin01@seed.mapo.go.kr'"
            )) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf(
                            "id=%s email=%s account_kind=%s status=%s%n",
                            rs.getObject("id"),
                            rs.getString("email"),
                            rs.getString("account_kind"),
                            rs.getString("status")
                    );
                }
                if (!found) {
                    throw new IllegalStateException(
                            "Seed verification failed: admin01@seed.mapo.go.kr not found"
                    );
                }
            }
        }

        System.out.println("Seed completed.");
    }

    private static boolean isTransactionBoundary(String sql) {
        String normalized = sql.trim();
        return normalized.equalsIgnoreCase("BEGIN")
                || normalized.equalsIgnoreCase("COMMIT")
                || normalized.equalsIgnoreCase("ROLLBACK");
    }

    private static String preview(String sql) {
        String compact = sql.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 180) {
            return compact;
        }
        return compact.substring(0, 180) + "...";
    }

    private static Map<String, String> readDatasource(Path secretFile) throws IOException {
        String text = Files.readString(secretFile, StandardCharsets.UTF_8);
        Matcher matcher = FIELD_PATTERN.matcher(text);
        Map<String, String> values = new LinkedHashMap<>();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (values.containsKey(key)) {
                continue;
            }
            values.put(key, stripQuotes(matcher.group(2).trim()));
        }
        for (String required : List.of("url", "username", "password")) {
            if (!values.containsKey(required) || values.get(required).isBlank()) {
                throw new IllegalStateException(
                        "missing spring.datasource." + required + " in secret file"
                );
            }
        }
        String url = values.get("url");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException(
                    "unsupported jdbc url; expected jdbc:postgresql://host[:port]/database"
            );
        }
        return values;
    }

    static List<String> splitSql(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        String dollarTag = null;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : '\0';

            if (inLineComment) {
                current.append(c);
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                current.append(c);
                if (c == '*' && next == '/') {
                    current.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            if (dollarTag != null) {
                if (script.startsWith(dollarTag, i)) {
                    current.append(dollarTag);
                    i += dollarTag.length() - 1;
                    dollarTag = null;
                } else {
                    current.append(c);
                }
                continue;
            }

            if (inSingleQuote) {
                current.append(c);
                if (c == '\'' && next == '\'') {
                    current.append(next);
                    i++;
                } else if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            if (c == '-' && next == '-') {
                current.append(c).append(next);
                i++;
                inLineComment = true;
                continue;
            }

            if (c == '/' && next == '*') {
                current.append(c).append(next);
                i++;
                inBlockComment = true;
                continue;
            }

            if (c == '\'') {
                current.append(c);
                inSingleQuote = true;
                continue;
            }

            if (c == '$') {
                String tag = readDollarTag(script, i);
                if (tag != null) {
                    current.append(tag);
                    i += tag.length() - 1;
                    dollarTag = tag;
                    continue;
                }
            }

            if (c == ';') {
                String statement = current.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        String trailing = current.toString().trim();
        if (!trailing.isEmpty()) {
            statements.add(trailing);
        }
        return statements;
    }

    private static String readDollarTag(String script, int start) {
        if (script.charAt(start) != '$') {
            return null;
        }
        int i = start + 1;
        while (i < script.length()) {
            char c = script.charAt(i);
            if (c == '$') {
                return script.substring(start, i + 1);
            }
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return null;
            }
            i++;
        }
        return null;
    }

    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
