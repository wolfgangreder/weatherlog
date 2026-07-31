/*
 * Copyright 2024 wolfi.
 * Licensed under the Apache License, Version 2.0
 */
package at.or.reder.weather.model;

import java.time.LocalDate;

public record HeatpumpEnergyRecord(LocalDate date, double energyGenerated, double energyConsumed) {

    public double getEnergyEarned() {
        return energyGenerated - energyConsumed;
    }

    public double getEnergySaving() {
        return energyConsumed != 0d ? energyGenerated / energyConsumed : 0;
    }
}
