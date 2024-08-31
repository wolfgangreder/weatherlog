/*
 * Copyright 2024 Wolfgang Reder.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.or.reder.weather.monitoring.impl;

import at.or.reder.weather.monitoring.DatabaseMonitoringService;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.extern.java.Log;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryScope;
import org.firebirdsql.gds.impl.GDSType;
import org.firebirdsql.gds.impl.wire.WireGDSFactoryPlugin;
import org.firebirdsql.management.FBStatisticsManager;
import org.firebirdsql.management.StatisticsManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Wolfgang Reder
 */
@Default
@Singleton
@Log
public class DatabaseMonitoringServiceImpl implements DatabaseMonitoringService
{

  private static final Map<String, Number> VALUES = new ConcurrentHashMap<>();
  private static final Pattern PAT_HEADER_PAGE = Pattern.compile("\\A(\\t*)([^\\t]*?)(\\t*)(\\d+(\\.)?\\d*)");
  private static final Pattern PAT_DATA = Pattern.compile(
          "\\A(\\s*)(([A-Za-z ]+):\\s*)((\\d+(.?\\d+)?)%?)(,\\s*([A-Za-z ]+):\\s*((\\d+(.?\\d+)?)%?))?(,\\s*([A-Za-z ]+):\\s*((\\d+(.?\\d+)?)%?))?");
  private static final Pattern PAT_DISTRIBUTION = Pattern.compile("\\A\\s*\\d{1,2}\\s*-\\s*(\\d{1,2})%\\s*=\\s*(\\d+)");

  @Inject
  private MBeanServer mbs;
  @Inject
  @RegistryScope(scope = "database")
  private MetricRegistry registry;

  private Optional<String> findServerXML(Collection<?> filePaths)
  {
    return filePaths.stream()
            .map(Object::toString)
            .filter(s -> s.endsWith("server.xml"))
            .findFirst();
  }

  private Optional<Path> getServerConfig() throws JMException
  {
    ObjectName name = new ObjectName("WebSphere:feature=kernel,name=ServerInfo");
    Object userDir = mbs.getAttribute(name,
                                      "UserDirectory");
    Object serverName = mbs.getAttribute(name,
                                         "Name");
    name = new ObjectName("WebSphere:name=com.ibm.websphere.config.mbeans.ServerXMLConfigurationMBean");
    Collection<?> filePaths = (Collection) mbs.invoke(name,
                                                      "fetchConfigurationFilePaths",
                                                      null,
                                                      null);
    if (userDir != null && serverName != null && filePaths != null && !filePaths.isEmpty()) {
      try {
        Path serverPath = Path.of(userDir.toString(),
                                  "servers",
                                  serverName.toString()).toAbsolutePath();
        Optional<String> filePath = findServerXML(filePaths);
        if (filePath.isPresent()) {
          filePath = filePath.map(f -> f.replace("${server.config.dir}",
                                                 serverPath.toString()));
          Path result = Path.of(filePath.get()).toAbsolutePath();
          if (Files.isReadable(result) && Files.isRegularFile(result)) {
            return Optional.of(result);
          }
        }
      } catch (InvalidPathException ex) {

      }
    }
    return Optional.empty();
  }

  public DocumentBuilder createDocumentBuilder() throws ParserConfigurationException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
    return factory.newDocumentBuilder();
  }

  private Document createDocumentBuilder(Path path)
  {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
      return factory.newDocumentBuilder().parse(path.toFile());
    } catch (ParserConfigurationException | SAXException | IOException ex) {
      log.log(Level.SEVERE,
              null,
              ex);
    }
    return null;
  }

  private Map<String, String> collectVariables(Document doc,
                                               XPath xpath) throws XPathExpressionException
  {
    NodeList list = (NodeList) xpath.evaluate("/server/variable",
                                              doc,
                                              XPathConstants.NODESET);
    Map<String, String> result = new HashMap<>();
    for (int i = 0; i < list.getLength(); ++i) {
      Node node = list.item(i);
      NamedNodeMap attr = node.getAttributes();
      String key = attr.getNamedItem("name").getNodeValue();
      String value = attr.getNamedItem("defaultValue").getNodeValue();
      result.put(key,
                 value);
    }
    result.putAll(System.getenv());
    return result;
  }

  private String applyVariables(String string,
                                Map<String, String> variables)
  {
    String result = string;
    for (Map.Entry<String, String> e : variables.entrySet()) {
      result = result.replace("${" + e.getKey() + "}",
                              e.getValue());
    }
    return result;
  }

  private Optional<FBStatisticsManager> createStatisticsManager() throws JMException, XPathExpressionException
  {
    Pattern pattern = Pattern.compile("([a-zA-Z0-9\\.]+)/((\\d+):)?(.*)");
    Optional<Path> configFile = getServerConfig();
    if (configFile.isEmpty()) {
      return Optional.empty();
    }
    Document doc = configFile.map(this::createDocumentBuilder).orElse(null);
    XPathFactory xpathFactory = XPathFactory.newInstance();
    XPath path = xpathFactory.newXPath();
    XPathExpression urlExpression = path.compile("/server/dataSource[@id='weatherdatasource']/properties/@database");
    XPathExpression passwordExpression = path.compile("/server/dataSource[@id='weatherdatasource']/properties/@password");
    XPathExpression usernameExpression = path.compile("/server/dataSource[@id='weatherdatasource']/properties/@userName");
    XPathExpression charsetExpression = path.compile("/server/dataSource[@id='weatherdatasource']/properties/@charSet");
    Map<String, String> variables = collectVariables(doc,
                                                     path);
    String url = applyVariables(urlExpression.evaluate(doc),
                                variables);
    String password = applyVariables(passwordExpression.evaluate(doc),
                                     variables);
    String username = applyVariables(usernameExpression.evaluate(doc),
                                     variables);
    String charset = applyVariables(charsetExpression.evaluate(doc),
                                    variables);
    FBStatisticsManager mgr = new FBStatisticsManager(GDSType.getType(WireGDSFactoryPlugin.PURE_JAVA_TYPE_NAME));
    Matcher matcher = pattern.matcher(url);
    if (matcher.matches()) {
      mgr.setCharSet(charset);
      mgr.setUser(username);
      mgr.setPassword(password);
      mgr.setServerName(matcher.group(1));
      if (matcher.group(3) != null && !matcher.group(3).isBlank()) {
        mgr.setPortNumber(Integer.parseInt(matcher.group(3)));
      }
      mgr.setDatabase(matcher.group(4));
      return Optional.of(mgr);
    } else {
      log.log(Level.SEVERE,
              "Url does not match!");
    }
    return Optional.empty();
  }

  private boolean forwardToLine(LineNumberReader reader,
                                String lineContent) throws IOException
  {
    String line;
    while ((line = reader.readLine()) != null) {
      if (lineContent.equals(line)) {
        return true;
      }
    }
    return false;
  }

  private String createParam(String string)
  {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < string.length(); ++i) {
      char ch = string.charAt(i);
      if (Character.isAlphabetic(ch)) {
        builder.append(Character.toLowerCase(ch));
      } else {
        builder.append('_');
      }
    }
    return builder.toString();
  }

  private void createHeaderGauge(String database,
                                 String headerParam,
                                 Supplier<Number> value)
  {
    MetadataBuilder builder = Metadata.builder();
    Tag tags[] = new Tag[]{new Tag("dbms",
                                   "firebirdsql"),
                           new Tag("database",
                                   database)};
    Metadata data = builder.withName("headerpage_" + headerParam).withUnit(MetricUnits.NONE).build();
    registry.gauge(data,
                   value,
                   tags);
  }

  private void parseHeaderPage(String database,
                               LineNumberReader reader) throws IOException
  {
    String line;
    if (forwardToLine(reader,
                      "Database header page information:")) {

      line = reader.readLine();
      while (line != null && !line.isBlank()) {
        Matcher matcher = PAT_HEADER_PAGE.matcher(line);
        if (matcher.matches()) {
          String param = createParam(matcher.group(2));
          String value = matcher.group(4);
          Number numValue;
          if (value.indexOf('.') > 0) {
            numValue = Double.valueOf(value);
          } else {
            numValue = Long.valueOf(value);
          }
          VALUES.put(param,
                     numValue);
          createHeaderGauge(database,
                            param,
                            () -> VALUES.get(param));
        }
        line = reader.readLine();
      }
    }
  }

  private void createDataGauge(String database,
                               String objectGroup,
                               String tableName,
                               String dataParam,
                               Supplier<Number> value,
                               String unit)
  {
    MetadataBuilder builder = Metadata.builder();
    Tag tags[] = new Tag[]{new Tag("dbms",
                                   "firebirdsql"),
                           new Tag("database",
                                   database),
                           new Tag("object",
                                   tableName)};
    Metadata data = builder.withName(objectGroup + "_" + dataParam).withUnit(unit).build();
    registry.gauge(data,
                   value,
                   tags);
  }

  private void createTotalGauge(String database,
                                Supplier<Number> value)
  {
    MetadataBuilder builder = Metadata.builder();
    Tag tags[] = new Tag[]{new Tag("dbms",
                                   "firebirdsql"),
                           new Tag("database",
                                   database)};
    Metadata data = builder.withName("data_total_data_pages").withUnit(MetricUnits.NONE).build();
    registry.gauge(data,
                   value,
                   tags);
  }

  private void createDistributionGauge(String database,
                                       String objectType,
                                       String tableName,
                                       String rangeTo,
                                       Supplier<Number> value)
  {
    MetadataBuilder builder = Metadata.builder();
    Tag tags[] = new Tag[]{new Tag("dbms",
                                   "firebirdsql"),
                           new Tag("database",
                                   database),
                           new Tag("object",
                                   tableName),
                           new Tag("range",
                                   rangeTo)};
    Metadata data = builder.withName(objectType + "_distribution").withUnit(MetricUnits.PERCENT).build();
    registry.gauge(data,
                   value,
                   tags);

  }

  private long processDataEntry(String database,
                                String objectGroup,
                                String tableName,
                                String name,
                                String value,
                                String unit)
  {
    double val = Double.parseDouble(value);
    VALUES.put(database + objectGroup + tableName + name,
               val);
    createDataGauge(database,
                    objectGroup,
                    tableName,
                    name,
                    () -> VALUES.get(database + objectGroup + tableName + name),
                    unit);
    if ("data_page_slots".equals(name)) {
      return Long.parseLong(value);
    } else {
      return 0;
    }
  }

  private boolean parseIndexData(String database,
                                 String indexName,
                                 LineNumberReader reader) throws IOException
  {
    String line = reader.readLine();
    while (line != null && !line.contains("Fill distribution:")) {
      Matcher matcher = PAT_DATA.matcher(line);
      processDataLine(database,
                      "index",
                      indexName,
                      matcher);
      line = reader.readLine();
    }
    line = reader.readLine();
    while (line != null && !line.isBlank()) {
      Matcher matcher = PAT_DISTRIBUTION.matcher(line);
      if (matcher.matches()) {
        String rangeTo = matcher.group(1);
        double val = Double.parseDouble(matcher.group(2));
        VALUES.put(database + "index" + indexName + "distribution" + rangeTo,
                   val);
        createDistributionGauge(database,
                                "index",
                                indexName,
                                rangeTo,
                                () -> VALUES.get(database + "index" + indexName + "distribution" + rangeTo));
      }
      line = reader.readLine();
    }
    return line != null;
  }

  private long processDataLine(String database,
                               String objectGroup,
                               String objectName,
                               Matcher matcher)
  {
    long totalDataPages = 0;
    if (matcher.matches()) {
      String name = createParam(matcher.group(3));
      String val = matcher.group(5);
      totalDataPages += processDataEntry(database,
                                         objectGroup,
                                         objectName,
                                         name,
                                         val,
                                         matcher.group(4).indexOf('%') > 0 ? MetricUnits.PERCENT : MetricUnits.NONE);
      if (matcher.groupCount() > 10 && matcher.group(8) != null) {
        name = createParam(matcher.group(8));
        val = matcher.group(10);
        totalDataPages += processDataEntry(database,
                                           objectGroup,
                                           objectName,
                                           name,
                                           val,
                                           matcher.group(4).indexOf('%') > 0 ? MetricUnits.PERCENT : MetricUnits.NONE);
      }
      if (matcher.groupCount() > 15 && matcher.group(13) != null) {
        name = createParam(matcher.group(13));
        val = matcher.group(15);
        totalDataPages += processDataEntry(database,
                                           objectGroup,
                                           objectName,
                                           name,
                                           val,
                                           matcher.group(4).indexOf('%') > 0 ? MetricUnits.PERCENT : MetricUnits.NONE);
      }
    }
    return totalDataPages;
  }

  private boolean parseTableData(String database,
                                 String tableName,
                                 LineNumberReader reader,
                                 AtomicLong totalDataPages) throws IOException
  {
    String line = reader.readLine();
    while (line != null && !line.contains("Fill distribution:")) {
      Matcher matcher = PAT_DATA.matcher(line);
      totalDataPages.addAndGet(processDataLine(database,
                                               "data",
                                               tableName,
                                               matcher));
      line = reader.readLine();
    }
    line = reader.readLine();
    while (line != null && !line.isBlank()) {
      Matcher matcher = PAT_DISTRIBUTION.matcher(line);
      if (matcher.matches()) {
        String rangeTo = matcher.group(1);
        double val = Double.parseDouble(matcher.group(2));
        VALUES.put(database + "data" + tableName + "distribution" + rangeTo,
                   val);
        createDistributionGauge(database,
                                "data",
                                tableName,
                                rangeTo,
                                () -> VALUES.get(database + "data" + tableName + "distribution" + rangeTo));
      }
      line = reader.readLine();
    }
    return line != null;
  }

  private void parseData(String database,
                         byte[] buffer) throws IOException
  {
    try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(buffer),
                                                                              StandardCharsets.UTF_8))) {
      parseHeaderPage(database,
                      reader);
      boolean hasMore = forwardToLine(reader,
                                      "Analyzing database pages ...");
      String objectName;
      AtomicLong totalDatapages = new AtomicLong();

      while (hasMore) {
        objectName = reader.readLine();
        if (objectName != null) {
          int indexPos = objectName.indexOf("Index ");
          if (indexPos > 0) {
            int spacePos = objectName.indexOf(' ',
                                              indexPos + 6);
            hasMore = parseIndexData(database,
                                     objectName.substring(indexPos + 6,
                                                          spacePos),
                                     reader);
          } else {
            int spacePos = objectName.indexOf(' ');
            hasMore = parseTableData(database,
                                     objectName.substring(0,
                                                          spacePos),
                                     reader,
                                     totalDatapages);
          }
        } else {
          hasMore = false;
        }
      }
      VALUES.put(database + "_data_total_data_pages",
                 totalDatapages.get());
      createTotalGauge(database,
                       () -> VALUES.get(database + "_data_total_data_pages"));

    }
  }

  @Override
  public Reader fetchDatabaseMetrics()
  {
    try {
      Optional<FBStatisticsManager> statManager = createStatisticsManager();
      if (statManager.isPresent()) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        statManager.get().setLogger(bos);
        statManager.get().getDatabaseStatistics(StatisticsManager.DATA_TABLE_STATISTICS
                                                + StatisticsManager.INDEX_STATISTICS
                                                + StatisticsManager.RECORD_VERSION_STATISTICS);
        parseData(statManager.get().getDatabase(),
                  bos.toByteArray());
        return new InputStreamReader(new ByteArrayInputStream(bos.toByteArray()),
                                     StandardCharsets.UTF_8);
      }
    } catch (SQLException | IOException | JMException | XPathExpressionException ex) {
      log.log(Level.SEVERE,
              null,
              ex);
    }
    return null;
  }

  @Schedule(hour = "*/1", minute = "0", second = "0")
  void onTimeout()
  {
    fetchDatabaseMetrics();
  }

}
