/*
 * Copyright 2024 Wolfgang Reder.
 * Licensed under the Apache License, Version 2.0
 */
package at.or.reder.weather.model;

import java.time.LocalDateTime;
import java.util.List;

public class WeatherSample {

    private LocalDateTime generated;
    private LocalDateTime queryFrom;
    private LocalDateTime queryTo;
    private List<WeatherRecord> records;

    public WeatherSample() {}

    public LocalDateTime getGenerated() { return generated; }
    public LocalDateTime getQueryFrom() { return queryFrom; }
    public LocalDateTime getQueryTo() { return queryTo; }
    public List<WeatherRecord> getRecords() { return records; }

    public WeatherSample setGenerated(LocalDateTime generated) {
        this.generated = generated;
        return this;
    }

    public WeatherSample setQueryFrom(LocalDateTime queryFrom) {
        this.queryFrom = queryFrom;
        return this;
    }

    public WeatherSample setQueryTo(LocalDateTime queryTo) {
        this.queryTo = queryTo;
        return this;
    }

    public WeatherSample setRecords(List<WeatherRecord> records) {
        this.records = records;
        return this;
    }
}
