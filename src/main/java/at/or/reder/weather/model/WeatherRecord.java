/*
 * Copyright 2024 Wolfgang Reder.
 * Licensed under the Apache License, Version 2.0
 */
package at.or.reder.weather.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "WeatherRecord")
@Table(name = "stationdata")
@SequenceGenerator(name = "stationdata_seq", sequenceName = "stationdata_seq", allocationSize = 1)
public class WeatherRecord extends PanacheEntityBase {

    @Column(name = "id", updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stationdata_seq")
    @Id
    @JsonbTransient
    private long id;

    @Column(name = "stationkey", length = 32, updatable = false)
    @JsonbTransient
    private String stationkey;

    @Transient @JsonbTransient private String stationtype;
    @Transient @JsonbTransient private long runtime;
    @Transient @JsonbTransient private long heap;

    @Column(name = "sampletime", updatable = false)
    private LocalDateTime sampleTime;

    @Column(name = "tempin", updatable = false)    private Double tempin;
    @Column(name = "tempout", updatable = false)   private Double tempout;
    @Column(name = "humidityin", updatable = false) private Double humidityin;
    @Column(name = "humidityout", updatable = false) private Double humidityout;
    @Column(name = "pressurerel", updatable = false) private Double pressurerel;
    @Column(name = "pressureabs", updatable = false) private Double pressureabs;
    @Column(name = "winddir", updatable = false)   private Double winddir;
    @Column(name = "windspeed", updatable = false) private Double windspeed;
    @Column(name = "windgust", updatable = false)  private Double windgust;
    @Column(name = "maxdailygust", updatable = false) private Double maxdailygust;
    @Column(name = "solarradiation", updatable = false) private Double solarradiation;
    @Column(name = "uv", updatable = false)        private Integer uv;
    @Column(name = "rainrate", updatable = false)  private Double rainrate;
    @Column(name = "eventrain", updatable = false) private Double eventrain;
    @Column(name = "hourlyrain", updatable = false) private Double hourlyrain;
    @Column(name = "dailyrain", updatable = false) private Double dailyrain;
    @Column(name = "weeklyrain", updatable = false) private Double weeklyrain;
    @Column(name = "monthlyrain", updatable = false) private Double monthlyrain;
    @Column(name = "yearlyrain", updatable = false) private Double yearlyrain;
    @Column(name = "totalrain", updatable = false) private Double totalrain;

    @Transient @JsonbTransient private int wh65batt;
    @Transient @JsonbTransient private int freq;
    @Transient @JsonbTransient private String model;
    @Transient @JsonbTransient private int interval;

    public WeatherRecord() {}

    public long getId() { return id; }
    public String getStationkey() { return stationkey; }
    public String getStationtype() { return stationtype; }
    public long getRuntime() { return runtime; }
    public long getHeap() { return heap; }
    public LocalDateTime getSampleTime() { return sampleTime; }
    public Double getTempin() { return tempin; }
    public Double getTempout() { return tempout; }
    public Double getHumidityin() { return humidityin; }
    public Double getHumidityout() { return humidityout; }
    public Double getPressurerel() { return pressurerel; }
    public Double getPressureabs() { return pressureabs; }
    public Double getWinddir() { return winddir; }
    public Double getWindspeed() { return windspeed; }
    public Double getWindgust() { return windgust; }
    public Double getMaxdailygust() { return maxdailygust; }
    public Double getSolarradiation() { return solarradiation; }
    public Integer getUv() { return uv; }
    public Double getRainrate() { return rainrate; }
    public Double getEventrain() { return eventrain; }
    public Double getHourlyrain() { return hourlyrain; }
    public Double getDailyrain() { return dailyrain; }
    public Double getWeeklyrain() { return weeklyrain; }
    public Double getMonthlyrain() { return monthlyrain; }
    public Double getYearlyrain() { return yearlyrain; }
    public Double getTotalrain() { return totalrain; }
    public int getWh65batt() { return wh65batt; }
    public int getFreq() { return freq; }
    public String getModel() { return model; }
    public int getInterval() { return interval; }

    public void setStationkey(String v) { this.stationkey = v; }
    public void setStationtype(String v) { this.stationtype = v; }
    public void setRuntime(long v) { this.runtime = v; }
    public void setHeap(long v) { this.heap = v; }
    public void setSampleTime(LocalDateTime v) { this.sampleTime = v; }
    public void setTempin(Double v) { this.tempin = v; }
    public void setTempout(Double v) { this.tempout = v; }
    public void setHumidityin(Double v) { this.humidityin = v; }
    public void setHumidityout(Double v) { this.humidityout = v; }
    public void setPressurerel(Double v) { this.pressurerel = v; }
    public void setPressureabs(Double v) { this.pressureabs = v; }
    public void setWinddir(Double v) { this.winddir = v; }
    public void setWindspeed(Double v) { this.windspeed = v; }
    public void setWindgust(Double v) { this.windgust = v; }
    public void setMaxdailygust(Double v) { this.maxdailygust = v; }
    public void setSolarradiation(Double v) { this.solarradiation = v; }
    public void setUv(Integer v) { this.uv = v; }
    public void setRainrate(Double v) { this.rainrate = v; }
    public void setEventrain(Double v) { this.eventrain = v; }
    public void setHourlyrain(Double v) { this.hourlyrain = v; }
    public void setDailyrain(Double v) { this.dailyrain = v; }
    public void setWeeklyrain(Double v) { this.weeklyrain = v; }
    public void setMonthlyrain(Double v) { this.monthlyrain = v; }
    public void setYearlyrain(Double v) { this.yearlyrain = v; }
    public void setTotalrain(Double v) { this.totalrain = v; }
    public void setWh65batt(int v) { this.wh65batt = v; }
    public void setFreq(int v) { this.freq = v; }
    public void setModel(String v) { this.model = v; }
    public void setInterval(int v) { this.interval = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeatherRecord that)) return false;
        return Objects.equals(sampleTime, that.sampleTime);
    }

    @Override
    public int hashCode() { return Objects.hash(sampleTime); }
}
