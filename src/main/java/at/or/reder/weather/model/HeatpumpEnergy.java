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
import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "HeatpumpEnergy")
@Table(name = "heatpumpenergy")
@SequenceGenerator(name = "heatpumpenergy_seq", sequenceName = "heatpumpenergy_seq", allocationSize = 1)
public class HeatpumpEnergy extends PanacheEntityBase {

    @Column(name = "id", updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "heatpumpenergy_seq")
    @Id
    @JsonbTransient
    private long id;

    @Column(name = "sampleday", updatable = false)
    private LocalDate sampleday;

    @Column(name = "earnedEnvironmentEnergyHeating")
    private Double earnedEnvironmentEnergyHeating;

    @Column(name = "consumedElectricalEnergyDomesticHotWater")
    private Double consumedElectricalEnergyDomesticHotWater;

    @Column(name = "consumedElectricalEnergyHeating")
    private Double consumedElectricalEnergyHeating;

    @Column(name = "heatGeneratedHeating")
    private Double heatGeneratedHeating;

    @Column(name = "earnedEnvironmentEnergyDomesticHotWater")
    private Double earnedEnvironmentEnergyDomesticHotWater;

    @Column(name = "heatGeneratedDomesticHotWater")
    private Double heatGeneratedDomesticHotWater;

    public HeatpumpEnergy() {}

    public long getId() { return id; }
    public LocalDate getSampleday() { return sampleday; }
    public Double getEarnedEnvironmentEnergyHeating() { return earnedEnvironmentEnergyHeating; }
    public Double getConsumedElectricalEnergyDomesticHotWater() { return consumedElectricalEnergyDomesticHotWater; }
    public Double getConsumedElectricalEnergyHeating() { return consumedElectricalEnergyHeating; }
    public Double getHeatGeneratedHeating() { return heatGeneratedHeating; }
    public Double getEarnedEnvironmentEnergyDomesticHotWater() { return earnedEnvironmentEnergyDomesticHotWater; }
    public Double getHeatGeneratedDomesticHotWater() { return heatGeneratedDomesticHotWater; }

    public void setSampleday(LocalDate sampleday) { this.sampleday = sampleday; }
    public void setEarnedEnvironmentEnergyHeating(Double v) { this.earnedEnvironmentEnergyHeating = v; }
    public void setConsumedElectricalEnergyDomesticHotWater(Double v) { this.consumedElectricalEnergyDomesticHotWater = v; }
    public void setConsumedElectricalEnergyHeating(Double v) { this.consumedElectricalEnergyHeating = v; }
    public void setHeatGeneratedHeating(Double v) { this.heatGeneratedHeating = v; }
    public void setEarnedEnvironmentEnergyDomesticHotWater(Double v) { this.earnedEnvironmentEnergyDomesticHotWater = v; }
    public void setHeatGeneratedDomesticHotWater(Double v) { this.heatGeneratedDomesticHotWater = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HeatpumpEnergy that)) return false;
        return Objects.equals(sampleday, that.sampleday);
    }

    @Override
    public int hashCode() { return Objects.hash(sampleday); }
}
