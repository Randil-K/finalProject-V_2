package lk.tideline.cleanup.config.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MariaDB without enum check constraints, for the same reason as {@link TidelineMySQLDialect}.
 *
 * <p>MariaDB speaks the MySQL protocol and is what XAMPP installs under the name "MySQL", so the
 * same JDBC driver and URL are used for both. Only the dialect differs, which is why it is set by
 * {@code DB_DIALECT} — leave it alone for MariaDB, point it at {@link TidelineMySQLDialect} for an
 * Oracle MySQL server.
 */
public class TidelineMariaDBDialect extends MariaDBDialect {

    /**
     * MariaDB announces itself as {@code 5.5.5-10.4.32-MariaDB} — a disguise from the days when
     * clients refused to talk to anything above 5.5. Taken at face value Hibernate falls back to a
     * legacy mode it warns about, so the real version is read out of the string behind it.
     */
    private static final Pattern VERSION = Pattern.compile("(?:5\\.5\\.5-)?(\\d+)\\.(\\d+)\\.(\\d+)");

    private static final DatabaseVersion ASSUMED = DatabaseVersion.make(10, 4);

    public TidelineMariaDBDialect() {
        super(ASSUMED);
    }

    public TidelineMariaDBDialect(DialectResolutionInfo info) {
        super(versionOf(info));
    }

    static DatabaseVersion versionOf(DialectResolutionInfo info) {
        String announced = info == null ? null : info.getDatabaseVersion();
        if (announced != null) {
            Matcher matcher = VERSION.matcher(announced);
            if (matcher.find()) {
                return DatabaseVersion.make(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)));
            }
        }
        return ASSUMED;
    }

    @Override
    public String getCheckCondition(String columnName, String[] values) {
        return null;
    }

    @Override
    public String getCheckCondition(String columnName, Class<? extends Enum<?>> enumType) {
        return null;
    }
}
