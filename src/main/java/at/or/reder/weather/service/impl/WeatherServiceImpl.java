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
package at.or.reder.weather.service.impl;

import at.or.reder.weather.model.WeatherRecord;
import at.or.reder.weather.model.WeatherSample;
import at.or.reder.weather.model.WeatherUtils;
import at.or.reder.weather.service.WeatherService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.java.Log;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryScope;

/**
 *
 * @author Wolfgang Reder
 */
@ApplicationScoped
@Default
@Log
public class WeatherServiceImpl implements WeatherService
{

  private static final AtomicReference<WeatherRecord> currentWeather = new AtomicReference<>();

  @PersistenceContext(name = "WEATHER")
  private EntityManager entityManager;
  @Inject
  @RegistryScope(scope = "weather")
  private MetricRegistry metricRegistry;

  @PostConstruct
  void postConstruct()
  {
    registerGauge("temperatur",
                  "out",
                  WeatherRecord::getTempout);
    registerGauge("temperatur",
                  "in",
                  WeatherRecord::getTempin);
    registerGauge("pressure_abs",
                  "out",
                  WeatherRecord::getPressureabs);
    registerGauge("pressure_rel",
                  "out",
                  WeatherRecord::getPressurerel);
    registerGauge("rain_event",
                  "out",
                  WeatherRecord::getEventrain);
    registerGauge("rain_daily",
                  "out",
                  WeatherRecord::getDailyrain);
    registerGauge("rain_weekly",
                  "out",
                  WeatherRecord::getWeeklyrain);
    registerGauge("rain_total",
                  "out",
                  WeatherRecord::getTotalrain);
    registerGauge("rain_rate",
                  "out",
                  WeatherRecord::getRainrate);
    registerGauge("humidity",
                  "out",
                  WeatherRecord::getHumidityout);
    registerGauge("humidity",
                  "in",
                  WeatherRecord::getHumidityin);
    registerGauge("solarradiation",
                  "out",
                  WeatherRecord::getSolarradiation);
    registerGauge("uv",
                  "out",
                  WeatherRecord::getUv);
    registerGauge("wind_speed",
                  "out",
                  WeatherRecord::getWindspeed);
    registerGauge("wind_gust",
                  "out",
                  WeatherRecord::getWindgust);
    registerGauge("wind_max_gust",
                  "out",
                  WeatherRecord::getMaxdailygust);
    registerGauge("wind_dir",
                  "out",
                  WeatherRecord::getWinddir);
  }

  private <T> T getWeatherValue(Function<WeatherRecord, T> getter)
  {
    WeatherRecord rec = currentWeather.get();
    if (rec != null) {
      return getter.apply(rec);
    }
    return null;
  }

  private void registerGauge(String name,
                             String scope,
                             Function<WeatherRecord, Number> valueSupplier)
  {
    MetadataBuilder builder = Metadata.builder();
    Tag tags[] = new Tag[]{new Tag("scope",
                                   scope)};
    String realName = "weather_" + name;
    Metadata data = builder.withName(realName)
            .build();
    metricRegistry.gauge(data,
                         () -> getWeatherValue(valueSupplier),
                         tags);
  }

  @Override
  public Optional<WeatherRecord> getCurrent()
  {
    List<WeatherRecord> resultList = entityManager.createQuery("select wr from WeatherRecord wr order by wr.sampleTime desc",
                                                               WeatherRecord.class)
            .setMaxResults(1)
            .getResultList();
    if (!resultList.isEmpty()) {
      WeatherRecord rec = resultList.get(0);
      currentWeather.set(rec);
      return Optional.of(rec);
    }
    return Optional.empty();
  }

  @Override
  public WeatherSample getWeatherData(LocalDateTime timeFrom,
                                      LocalDateTime timeTo)
  {
    LocalDateTime now = WeatherUtils.convertLocalToUTC(LocalDateTime.now());
    List<WeatherRecord> records = entityManager.createQuery("select wr from WeatherRecord wr where wr.sampleTime>=?1 and wr.sampleTime<?2 order by wr.sampleTime",
                                                            WeatherRecord.class)
            .setParameter(1,
                          timeFrom)
            .setParameter(2,
                          timeTo)
            .getResultList()
            .stream()
            .map(wr -> wr.toBuilder().build())
            .collect(Collectors.toList());
    return new WeatherSample()
            .setGenerated(now)
            .setQueryTo(timeTo)
            .setQueryFrom(timeFrom)
            .setRecords(records);
  }

  @Override
  @Transactional(Transactional.TxType.REQUIRED)
  public void storeCurrentWeather(WeatherRecord record)
  {
    entityManager.persist(record);
    currentWeather.set(record);
  }

}
