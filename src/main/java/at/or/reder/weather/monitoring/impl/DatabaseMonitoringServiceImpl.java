/*
 * Copyright 2024 Wolfgang Reder.
 * Licensed under the Apache License, Version 2.0
 */
package at.or.reder.weather.monitoring.impl;

import at.or.reder.weather.monitoring.DatabaseMonitoringService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.firebirdsql.gds.impl.GDSType;
import org.firebirdsql.gds.impl.wire.WireGDSFactoryPlugin;
import org.firebirdsql.management.FBStatisticsManager;
import org.firebirdsql.management.StatisticsManager;

@Default
@ApplicationScoped
public class DatabaseMonitoringServiceImpl implements DatabaseMonitoringService {

    private static final Logger LOG = Logger.getLogger(DatabaseMonitoringServiceImpl.class.getName());
    private static final Map<String, Number> VALUES = new ConcurrentHashMap<>();
    private static final Pattern PAT_HEADER_PAGE = Pattern.compile("\\A(\\t*)([^\\t]*?)(\\t*)(\\d+(\\.)?\\d*)");
    private static final Pattern PAT_DATA = Pattern.compile(
            "\\A(\\s*)(([A-Za-z ]+):\\s*)((\\d+(.?\\d+)?)%?)(,\\s*([A-Za-z ]+):\\s*((\\d+(.?\\d+)?)%?))?(,\\s*([A-Za-z ]+):\\s*((\\d+(.?\\d+)?)%?))?");
    private static final Pattern PAT_DISTRIBUTION = Pattern.compile("\\A\\s*\\d{1,2}\\s*-\\s*(\\d{1,2})%\\s*=\\s*(\\d+)");

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "firebird.host")
    String firebirdHost;

    @ConfigProperty(name = "firebird.port", defaultValue = "3051")
    int firebirdPort;

    @ConfigProperty(name = "firebird.database")
    String firebirdDatabase;

    @ConfigProperty(name = "firebird.user")
    String firebirdUser;

    @ConfigProperty(name = "firebird.pass")
    String firebirdPass;

    private Optional<FBStatisticsManager> createStatisticsManager() {
        FBStatisticsManager mgr = new FBStatisticsManager(
                GDSType.getType(WireGDSFactoryPlugin.PURE_JAVA_TYPE_NAME));
        mgr.setCharSet("UTF-8");
        mgr.setUser(firebirdUser);
        mgr.setPassword(firebirdPass);
        mgr.setServerName(firebirdHost);
        mgr.setPortNumber(firebirdPort);
        mgr.setDatabase(firebirdDatabase);
        return Optional.of(mgr);
    }

    private boolean forwardToLine(LineNumberReader reader, String lineContent) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (lineContent.equals(line)) return true;
        }
        return false;
    }

    private String createParam(String string) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < string.length(); ++i) {
            char ch = string.charAt(i);
            builder.append(Character.isAlphabetic(ch) ? Character.toLowerCase(ch) : '_');
        }
        return builder.toString();
    }

    private void createHeaderGauge(String database, String headerParam, Supplier<Number> value) {
        Gauge.builder("headerpage_" + headerParam,
                      () -> { Number n = value.get(); return n != null ? n.doubleValue() : Double.NaN; })
             .tag("dbms", "firebirdsql").tag("database", database)
             .register(meterRegistry);
    }

    private void createDataGauge(String database, String objectGroup, String tableName,
                                  String dataParam, Supplier<Number> value) {
        Gauge.builder(objectGroup + "_" + dataParam,
                      () -> { Number n = value.get(); return n != null ? n.doubleValue() : Double.NaN; })
             .tag("dbms", "firebirdsql").tag("database", database).tag("object", tableName)
             .register(meterRegistry);
    }

    private void createTotalGauge(String database, Supplier<Number> value) {
        Gauge.builder("data_total_data_pages",
                      () -> { Number n = value.get(); return n != null ? n.doubleValue() : Double.NaN; })
             .tag("dbms", "firebirdsql").tag("database", database)
             .register(meterRegistry);
    }

    private void createDbFileGauge(String database, Supplier<Number> value) {
        Gauge.builder("data_dbfile_pages",
                      () -> { Number n = value.get(); return n != null ? n.doubleValue() : Double.NaN; })
             .tag("dbms", "firebirdsql").tag("database", database)
             .register(meterRegistry);
    }

    private void createDistributionGauge(String database, String objectType, String tableName,
                                          String rangeTo, Supplier<Number> value) {
        Gauge.builder(objectType + "_distribution",
                      () -> { Number n = value.get(); return n != null ? n.doubleValue() : Double.NaN; })
             .tag("dbms", "firebirdsql").tag("database", database)
             .tag("object", tableName).tag("range", rangeTo)
             .register(meterRegistry);
    }

    private void parseHeaderPage(String database, LineNumberReader reader) throws IOException {
        if (forwardToLine(reader, "Database header page information:")) {
            String line = reader.readLine();
            while (line != null && !line.isBlank()) {
                Matcher matcher = PAT_HEADER_PAGE.matcher(line);
                if (matcher.matches()) {
                    String param = createParam(matcher.group(2));
                    String value = matcher.group(4);
                    Number numValue = value.indexOf('.') > 0 ? Double.valueOf(value) : Long.valueOf(value);
                    VALUES.put(param, numValue);
                    createHeaderGauge(database, param, () -> VALUES.get(param));
                }
                line = reader.readLine();
            }
        }
    }

    private long processDataEntry(String database, String objectGroup, String tableName, String name, String value) {
        double val = Double.parseDouble(value);
        VALUES.put(database + objectGroup + tableName + name, val);
        createDataGauge(database, objectGroup, tableName, name,
                () -> VALUES.get(database + objectGroup + tableName + name));
        return "data_page_slots".equals(name) ? Long.parseLong(value) : 0;
    }

    private long processDataLine(String database, String objectGroup, String objectName, Matcher matcher) {
        long total = 0;
        if (matcher.matches()) {
            total += processDataEntry(database, objectGroup, objectName, createParam(matcher.group(3)), matcher.group(5));
            if (matcher.groupCount() > 10 && matcher.group(8) != null)
                total += processDataEntry(database, objectGroup, objectName, createParam(matcher.group(8)), matcher.group(10));
            if (matcher.groupCount() > 15 && matcher.group(13) != null)
                total += processDataEntry(database, objectGroup, objectName, createParam(matcher.group(13)), matcher.group(15));
        }
        return total;
    }

    private boolean parseIndexData(String database, String indexName, LineNumberReader reader) throws IOException {
        String line = reader.readLine();
        while (line != null && !line.contains("Fill distribution:")) {
            processDataLine(database, "index", indexName, PAT_DATA.matcher(line));
            line = reader.readLine();
        }
        line = reader.readLine();
        while (line != null && !line.isBlank()) {
            Matcher matcher = PAT_DISTRIBUTION.matcher(line);
            if (matcher.matches()) {
                String rangeTo = matcher.group(1);
                double val = Double.parseDouble(matcher.group(2));
                VALUES.put(database + "index" + indexName + "distribution" + rangeTo, val);
                createDistributionGauge(database, "index", indexName, rangeTo,
                        () -> VALUES.get(database + "index" + indexName + "distribution" + rangeTo));
            }
            line = reader.readLine();
        }
        return line != null;
    }

    private boolean parseTableData(String database, String tableName, LineNumberReader reader,
                                    long[] totalDataPages) throws IOException {
        String line = reader.readLine();
        while (line != null && !line.contains("Fill distribution:")) {
            totalDataPages[0] += processDataLine(database, "data", tableName, PAT_DATA.matcher(line));
            line = reader.readLine();
        }
        line = reader.readLine();
        while (line != null && !line.isBlank()) {
            Matcher matcher = PAT_DISTRIBUTION.matcher(line);
            if (matcher.matches()) {
                String rangeTo = matcher.group(1);
                double val = Double.parseDouble(matcher.group(2));
                VALUES.put(database + "data" + tableName + "distribution" + rangeTo, val);
                createDistributionGauge(database, "data", tableName, rangeTo,
                        () -> VALUES.get(database + "data" + tableName + "distribution" + rangeTo));
            }
            line = reader.readLine();
        }
        return line != null;
    }

    private void parseData(String database, byte[] buffer) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(
                new InputStreamReader(new ByteArrayInputStream(buffer), StandardCharsets.UTF_8))) {
            parseHeaderPage(database, reader);
            boolean hasMore = forwardToLine(reader, "Analyzing database pages ...");
            long[] totalDataPages = {0};
            while (hasMore) {
                String objectName = reader.readLine();
                if (objectName != null) {
                    int indexPos = objectName.indexOf("Index ");
                    if (indexPos > 0) {
                        int spacePos = objectName.indexOf(' ', indexPos + 6);
                        hasMore = parseIndexData(database, objectName.substring(indexPos + 6, spacePos), reader);
                    } else {
                        int spacePos = objectName.indexOf(' ');
                        hasMore = parseTableData(database, objectName.substring(0, spacePos), reader, totalDataPages);
                    }
                } else {
                    hasMore = false;
                }
            }
            VALUES.put(database + "_data_total_data_pages", totalDataPages[0]);
            createTotalGauge(database, () -> VALUES.get(database + "_data_total_data_pages"));
        }
    }

    @Transactional
    void readMonitoringTables(String database) {
        Query query = entityManager.createNativeQuery("select mon$pages from mon$database");
        try {
            Object tmp = query.getSingleResult();
            if (tmp instanceof Number number) {
                VALUES.put(database + "_data_dbfile_pages", number.intValue());
                createDbFileGauge(database, () -> VALUES.get(database + "_data_dbfile_pages"));
            }
        } catch (PersistenceException ex) {
            LOG.log(Level.SEVERE, "Cannot read monitoring data", ex);
        }
    }

    @Override
    public Reader fetchDatabaseMetrics() {
        try {
            Optional<FBStatisticsManager> statManager = createStatisticsManager();
            if (statManager.isPresent()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                statManager.get().setLogger(bos);
                statManager.get().getDatabaseStatistics(
                        StatisticsManager.DATA_TABLE_STATISTICS
                        | StatisticsManager.INDEX_STATISTICS
                        | StatisticsManager.RECORD_VERSION_STATISTICS);
                parseData(statManager.get().getDatabase(), bos.toByteArray());
                readMonitoringTables(statManager.get().getDatabase());
                return new InputStreamReader(new ByteArrayInputStream(bos.toByteArray()), StandardCharsets.UTF_8);
            }
        } catch (SQLException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Scheduled(cron = "0 0 * * * ?")
    void onTimeout() {
        fetchDatabaseMetrics();
    }
}
