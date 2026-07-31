package at.or.reder.weather;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/**
 * Forces H2 in-memory datasource regardless of system property overrides
 * (e.g. from a local config/application.properties with production Firebird settings).
 * QuarkusTestProfile.getConfigOverrides() has higher priority than system properties.
 */
public class H2TestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.datasource.weather.db-kind",              "h2",
            "quarkus.datasource.weather.jdbc.url",             "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
            "quarkus.datasource.weather.username",             "sa",
            "quarkus.datasource.weather.password",             "",
            "quarkus.datasource.weather.jdbc.driver",          "org.h2.Driver",
            "quarkus.hibernate-orm.dialect",                   "org.hibernate.dialect.H2Dialect",
            "quarkus.hibernate-orm.database.generation",       "drop-and-create",
            "quarkus.liquibase.weather.migrate-at-start",      "false"
        );
    }
}
