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
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "HeatpumpData")
@Table(name = "heatpumpdata")
@SequenceGenerator(name = "heatpumpdata_seq", sequenceName = "heatpumpdata_seq", allocationSize = 1)
public class HeatpumpData extends PanacheEntityBase {

    @Column(name = "id", updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "heatpumpdata_seq")
    @Id
    @JsonbTransient
    private long id;

    @Column(name = "sampletime", updatable = false)
    private LocalDateTime sampletime;

    @Column(name = "roomTemp")
    private double roomTemp;

    @Column(name = "roomTempSet")
    private double roomTempSet;

    @Column(name = "outdoorTemp")
    private double outdoorTemp;

    @Column(name = "hotWaterTemp")
    private double hotWaterTemp;

    public HeatpumpData() {}

    public long getId() { return id; }
    public LocalDateTime getSampletime() { return sampletime; }
    public double getRoomTemp() { return roomTemp; }
    public double getRoomTempSet() { return roomTempSet; }
    public double getOutdoorTemp() { return outdoorTemp; }
    public double getHotWaterTemp() { return hotWaterTemp; }

    public void setSampletime(LocalDateTime sampletime) { this.sampletime = sampletime; }
    public void setRoomTemp(double roomTemp) { this.roomTemp = roomTemp; }
    public void setRoomTempSet(double roomTempSet) { this.roomTempSet = roomTempSet; }
    public void setOutdoorTemp(double outdoorTemp) { this.outdoorTemp = outdoorTemp; }
    public void setHotWaterTemp(double hotWaterTemp) { this.hotWaterTemp = hotWaterTemp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HeatpumpData that)) return false;
        return Objects.equals(sampletime, that.sampletime);
    }

    @Override
    public int hashCode() { return Objects.hash(sampletime); }
}
