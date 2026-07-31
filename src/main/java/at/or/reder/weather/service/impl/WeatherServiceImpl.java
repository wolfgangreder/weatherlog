/*
 * Copyright 2024 Wolfgang Reder.
 * Licensed under the Apache License, Version 2.0
 */
package at.or.reder.weather.service.impl;

import at.or.reder.weather.model.WeatherRecord;
import at.or.reder.weather.model.WeatherSample;
import at.or.reder.weather.model.WeatherUtils;
import at.or.reder.weather.service.WeatherService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.panache.common.Sort;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Logger;

@ApplicationScoped
@Default
public class WeatherServiceImpl implements WeatherService {

    private static final Logger LOG = Logger.getLogger(WeatherServiceImpl.class.getName());
    private static final AtomicReference<WeatherRecord> currentWeather = new AtomicReference<>();

    @Inject
    MeterRegistry meterRegistry;

    @PostConstruct
    void postConstruct() {
        registerGauge("temperatur",    "out", WeatherRecord::getTempout);
        registerGauge("temperatur",    "in",  WeatherRecord::getTempin);
        registerGauge("pressure_abs",  "out", WeatherRecord::getPressureabs);
        registerGauge("pressure_rel",  "out", WeatherRecord::getPressurerel);
        registerGauge("rain_event",    "out", WeatherRecord::getEventrain);
        registerGauge("rain_daily",    "out", WeatherRecord::getDailyrain);
        registerGauge("rain_weekly",   "out", WeatherRecord::getWeeklyrain);
        registerGauge("rain_total",    "out", WeatherRecord::getTotalrain);
        registerGauge("rain_rate",     "out", WeatherRecord::getRainrate);
        registerGauge("humidity",      "out", WeatherRecord::getHumidityout);
        registerGauge("humidity",      "in",  WeatherRecord::getHumidityin);
        registerGauge("solarradiation","out", WeatherRecord::getSolarradiation);
        registerGauge("uv",            "out", WeatherRecord::getUv);
        registerGauge("wind_speed",    "out", WeatherRecord::getWindspeed);
        registerGauge("wind_gust",     "out", WeatherRecord::getWindgust);
        registerGauge("wind_max_gust", "out", WeatherRecord::getMaxdailygust);
        registerGauge("wind_dir",      "out", WeatherRecord::getWinddir);
    }

    private void registerGauge(String name, String scope, Function<WeatherRecord, Number> valueSupplier) {
        Gauge.builder("weather_" + name,
                      () -> {
                          WeatherRecord rec = currentWeather.get();
                          if (rec == null) return Double.NaN;
                          Number val = valueSupplier.apply(rec);
                          return val != null ? val.doubleValue() : Double.NaN;
                      })
             .tag("scope", scope)
             .register(meterRegistry);
    }

    @Override
    public Optional<WeatherRecord> getCurrent() {
        List<WeatherRecord> results = WeatherRecord
                .findAll(Sort.by("sampleTime").descending())
                .page(0, 1)
                .list();
        if (!results.isEmpty()) {
            WeatherRecord rec = results.get(0);
            currentWeather.set(rec);
            return Optional.of(rec);
        }
        return Optional.empty();
    }

    @Override
    public WeatherSample getWeatherData(LocalDateTime timeFrom, LocalDateTime timeTo) {
        LocalDateTime now = WeatherUtils.convertLocalToUTC(LocalDateTime.now());
        List<WeatherRecord> records = WeatherRecord
                .find("sampleTime >= ?1 and sampleTime < ?2",
                      Sort.by("sampleTime"),
                      timeFrom, timeTo)
                .list();
        return new WeatherSample()
                .setGenerated(now)
                .setQueryTo(timeTo)
                .setQueryFrom(timeFrom)
                .setRecords(records);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void storeCurrentWeather(WeatherRecord record) {
        record.persist();
        currentWeather.set(record);
    }
}
