import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Copies every row of the old H2 database into the MySQL one, table by table and column by column.
 *
 * <p>The MySQL schema is whatever Hibernate created from the entities, so this never writes DDL: it
 * reads the target's tables and columns, finds the same names in H2 (which upper-cases unquoted
 * identifiers), and moves the values across by name rather than by position. Anything the target
 * does not have is reported and skipped rather than guessed at.
 *
 * <p>Run it with the drivers Spring Boot uses, from the backend directory:
 *
 * <pre>
 * java --class-path "%USERPROFILE%\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar" ^
 *      tools\H2ToMySql.java [--dry-run]
 * </pre>
 */
public class H2ToMySql {

    private static final String H2_URL = env("H2_URL", "jdbc:h2:file:./data/tideline;AUTO_SERVER=TRUE;IFEXISTS=TRUE");
    private static final String H2_USER = env("H2_USERNAME", "sa");
    private static final String H2_PASSWORD = env("H2_PASSWORD", "");

    private static final String MYSQL_URL = env("MYSQL_URL",
            "jdbc:mysql://localhost:3306/tideline?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Colombo");
    private static final String MYSQL_USER = env("DB_USERNAME", "root");
    private static final String MYSQL_PASSWORD = env("DB_PASSWORD", "");

    public static void main(String[] args) throws Exception {
        boolean dryRun = List.of(args).contains("--dry-run");

        try (Connection h2 = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
             Connection mysql = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)) {

            mysql.setAutoCommit(false);
            List<String> tables = tablesOf(mysql);
            System.out.printf("%d tables in the MySQL schema%n%n", tables.size());
            System.out.printf("%-28s %8s %8s   %s%n", "TABLE", "H2", "COPIED", "NOTES");

            if (!dryRun) {
                run(mysql, "SET FOREIGN_KEY_CHECKS = 0");
            }

            int totalRows = 0;
            List<String> problems = new ArrayList<>();
            for (String table : tables) {
                Set<String> targetColumns = columnsOf(mysql, table);
                Source source = sourceOf(h2, table);
                if (source == null) {
                    System.out.printf("%-28s %8s %8s   not in H2 — nothing to copy%n", table, "-", 0);
                    continue;
                }

                List<String> missing = new ArrayList<>(targetColumns);
                missing.removeIf(column -> source.columns().containsKey(column));
                List<String> dropped = new ArrayList<>(source.columns().keySet());
                dropped.removeIf(targetColumns::contains);

                List<String> columns = new ArrayList<>(targetColumns);
                columns.removeIf(column -> !source.columns().containsKey(column));

                int available = count(h2, source.table());
                int copied = dryRun ? available : copy(h2, mysql, source, table, columns);
                totalRows += copied;

                String notes = "";
                if (!missing.isEmpty()) {
                    notes += "new column(s) left at their default: " + String.join(", ", missing) + "  ";
                }
                if (!dropped.isEmpty()) {
                    notes += "H2-only column(s) skipped: " + String.join(", ", dropped);
                    problems.add(table + " has columns MySQL does not: " + String.join(", ", dropped));
                }
                System.out.printf("%-28s %8d %8d   %s%n", table, available, copied, notes);
            }

            if (!dryRun) {
                resetAutoIncrement(mysql, tables);
                run(mysql, "SET FOREIGN_KEY_CHECKS = 1");
                mysql.commit();
            }

            System.out.printf("%n%s: %d rows across %d tables%n",
                    dryRun ? "Would copy" : "Copied", totalRows, tables.size());
            problems.forEach(problem -> System.out.println("  ! " + problem));
        }
    }

    /** Every base table in the MySQL schema, in a stable order. */
    private static List<String> tablesOf(Connection mysql) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = mysql.getMetaData().getTables(mysql.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        tables.sort(String::compareTo);
        return tables;
    }

    private static Set<String> columnsOf(Connection connection, String table) throws SQLException {
        Set<String> columns = new LinkedHashSet<>();
        try (ResultSet rs = connection.getMetaData().getColumns(connection.getCatalog(), null, table, "%")) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    /** An H2 table and its columns, as a map of lower-case name to the name H2 knows it by. */
    private record Source(String table, Map<String, String> columns) {
    }

    /** The H2 table matching this one, or null when H2 never had it. */
    private static Source sourceOf(Connection h2, String table) throws SQLException {
        DatabaseMetaData meta = h2.getMetaData();
        String found = null;
        try (ResultSet rs = meta.getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                if (name.equalsIgnoreCase(table)) {
                    found = name;
                    break;
                }
            }
        }
        if (found == null) {
            return null;
        }

        Map<String, String> columns = new LinkedHashMap<>();
        try (ResultSet rs = meta.getColumns(null, "PUBLIC", found, "%")) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                columns.put(name.toLowerCase(Locale.ROOT), name);
            }
        }
        return new Source(found, columns);
    }

    private static int count(Connection h2, String table) throws SQLException {
        try (Statement statement = h2.createStatement();
             ResultSet rs = statement.executeQuery("select count(*) from \"PUBLIC\".\"" + table + "\"")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static int copy(Connection h2, Connection mysql, Source source, String targetTable,
                            List<String> columns) throws SQLException {
        run(mysql, "DELETE FROM `" + targetTable + "`");
        if (columns.isEmpty()) {
            return 0;
        }

        String select = columns.stream()
                .map(column -> "\"" + source.columns().get(column) + "\"")
                .reduce((a, b) -> a + ", " + b)
                .orElseThrow();
        String insert = "INSERT INTO `" + targetTable + "` ("
                + columns.stream().map(c -> "`" + c + "`").reduce((a, b) -> a + ", " + b).orElseThrow()
                + ") VALUES (" + "?,".repeat(columns.size() - 1) + "?)";

        int rows = 0;
        try (Statement statement = h2.createStatement();
             ResultSet rs = statement.executeQuery("select " + select + " from \"PUBLIC\".\"" + source.table() + "\"");
             PreparedStatement ps = mysql.prepareStatement(insert)) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                for (int i = 1; i <= columns.size(); i++) {
                    Object value = rs.getObject(i);
                    if (value == null) {
                        ps.setNull(i, meta.getColumnType(i));
                    } else {
                        ps.setObject(i, converted(value));
                    }
                }
                ps.addBatch();
                if (++rows % 500 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }
        return rows;
    }

    /**
     * H2 keeps an {@code Instant} in a {@code timestamp with time zone} column and hands it back as
     * an {@code OffsetDateTime} at UTC. MySQL has no such type: Hibernate writes the same instant
     * into a plain {@code datetime(6)} as its UTC wall time, so that is what is written here. Handing
     * the driver the OffsetDateTime instead would shift every row by the server's time zone.
     */
    private static Object converted(Object value) {
        if (value instanceof OffsetDateTime time) {
            return LocalDateTime.ofInstant(time.toInstant(), ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return LocalDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
        }
        if (value instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        return value;
    }

    /** InnoDB keeps its own counter; point it past the ids that were just inserted. */
    private static void resetAutoIncrement(Connection mysql, List<String> tables) throws SQLException {
        for (String table : tables) {
            if (!columnsOf(mysql, table).contains("id")) {
                continue;
            }
            long next = 1;
            try (Statement statement = mysql.createStatement();
                 ResultSet rs = statement.executeQuery("select coalesce(max(id), 0) + 1 from `" + table + "`")) {
                if (rs.next()) {
                    next = rs.getLong(1);
                }
            }
            run(mysql, "ALTER TABLE `" + table + "` AUTO_INCREMENT = " + next);
        }
    }

    private static void run(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
