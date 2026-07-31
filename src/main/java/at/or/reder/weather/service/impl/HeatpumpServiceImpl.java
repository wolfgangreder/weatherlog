/*
 * Copyright 2024 wolfi.
 * Licensed under the Apache License, Version 2.0
 */
package at.or.reder.weather.service.impl;

import at.or.reder.weather.model.HeatpumpData;
import at.or.reder.weather.model.HeatpumpEnergy;
import at.or.reder.weather.model.HeatpumpEnergyRecord;
import at.or.reder.weather.model.HeatpumpScope;
import at.or.reder.weather.model.WeatherUtils;
import at.or.reder.weather.service.HeatpumpService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.LineNumberReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
@Default
public class HeatpumpServiceImpl implements HeatpumpService {

    private static final Logger LOG = Logger.getLogger(HeatpumpServiceImpl.class.getName());

    @Override
    public Optional<HeatpumpEnergyRecord> getEnergy(HeatpumpScope scope, LocalDate day) {
        HeatpumpEnergy record = HeatpumpEnergy.find("sampleday", day).firstResult();
        if (record == null) {
            LOG.log(Level.WARNING, "Cannot load energydata for {0}", day.toString());
            return Optional.empty();
        }
        return Optional.ofNullable(switch (scope) {
            case HEATING -> new HeatpumpEnergyRecord(
                    record.getSampleday(),
                    record.getHeatGeneratedHeating() != null ? record.getHeatGeneratedHeating() : 0d,
                    record.getConsumedElectricalEnergyHeating() != null ? record.getConsumedElectricalEnergyHeating() : 0d);
            case WATER -> new HeatpumpEnergyRecord(
                    record.getSampleday(),
                    record.getHeatGeneratedDomesticHotWater() != null ? record.getHeatGeneratedDomesticHotWater() : 0d,
                    record.getConsumedElectricalEnergyDomesticHotWater() != null ? record.getConsumedElectricalEnergyDomesticHotWater() : 0d);
        });
    }

    @Transactional(Transactional.TxType.REQUIRED)
    HeatpumpData findOrCreateHeatpumpData(LocalDateTime sampleTime) {
        List<HeatpumpData> list = HeatpumpData.find("sampletime", sampleTime).page(0, 1).list();
        if (list.isEmpty()) {
            HeatpumpData result = new HeatpumpData();
            result.setSampletime(sampleTime);
            result.persist();
            return result;
        }
        return list.get(0);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    HeatpumpEnergy findOrCreateHeatpumpEnergy(LocalDate sampleDay) {
        List<HeatpumpEnergy> list = HeatpumpEnergy.find("sampleday", sampleDay).page(0, 1).list();
        if (list.isEmpty()) {
            HeatpumpEnergy result = new HeatpumpEnergy();
            result.setSampleday(sampleDay);
            result.persist();
            return result;
        }
        return list.get(0);
    }

    private Optional<LocalDateTime> parseLocalDateTime(String str) {
        try {
            return Optional.of(WeatherUtils.convertToTimezone(
                    LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss")),
                    ZoneId.of("CET"), ZoneId.of("UTC")));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    private Optional<LocalDate> parseLocalDate(String str) {
        try {
            return Optional.of(LocalDateTime.parse(str,
                    DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss")).toLocalDate());
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    private OptionalInt findColumnIndex(String[] columns, String columnName) {
        for (int i = 0; i < columns.length; ++i) {
            if (columns[i].equals(columnName)) return OptionalInt.of(i);
        }
        return OptionalInt.empty();
    }

    private String readNextNoCommentLine(LineNumberReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("#")) return line;
        }
        return null;
    }

    Optional<Double> parseDouble(String[] parts, OptionalInt index) {
        if (index.isPresent()) {
            String part = parts[index.getAsInt()];
            if (!StringUtils.isBlank(part)) return WeatherUtils.parseDoubleValue(part);
        }
        return Optional.empty();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void insertZoneData(LineNumberReader reader) throws IOException {
        String line = readNextNoCommentLine(reader);
        if (line != null) {
            String[] parts = line.split(";");
            OptionalInt currentRoomIndex = findColumnIndex(parts, "CurrentRoomTemperature");
            OptionalInt setpointIndex = findColumnIndex(parts, "RoomTemperatureSetpoint");
            while ((line = readNextNoCommentLine(reader)) != null) {
                parts = line.split(";");
                if (parts.length >= 3) {
                    Optional<LocalDateTime> dt = parseLocalDateTime(parts[0]);
                    if (dt.isPresent()) {
                        Optional<Double> currentRoom = parseDouble(parts, currentRoomIndex);
                        Optional<Double> roomSet = parseDouble(parts, setpointIndex);
                        if (currentRoom.isPresent() && roomSet.isPresent()) {
                            HeatpumpData data = findOrCreateHeatpumpData(dt.get());
                            data.setRoomTemp(currentRoom.get());
                            data.setRoomTempSet(roomSet.get());
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void insertHotWaterData(LineNumberReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(";");
            if (parts.length >= 2) {
                Optional<LocalDateTime> dt = parseLocalDateTime(parts[0]);
                if (dt.isPresent()) {
                    Optional<Double> hotWater = WeatherUtils.parseDoubleValue(parts[1]);
                    if (hotWater.isPresent()) {
                        HeatpumpData data = findOrCreateHeatpumpData(dt.get());
                        data.setHotWaterTemp(hotWater.get());
                    }
                }
            }
        }
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void insertSystemData(LineNumberReader reader) throws IOException {
        String line = readNextNoCommentLine(reader);
        if (line != null) {
            while ((line = readNextNoCommentLine(reader)) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 2) {
                    Optional<LocalDateTime> dt = parseLocalDateTime(parts[0]);
                    if (dt.isPresent()) {
                        Optional<Double> outDoor = WeatherUtils.parseDoubleValue(parts[1]);
                        if (outDoor.isPresent()) {
                            HeatpumpData data = findOrCreateHeatpumpData(dt.get());
                            data.setOutdoorTemp(outDoor.get());
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void insertEnergyData(LineNumberReader reader) throws IOException {
        String line = readNextNoCommentLine(reader);
        if (line != null) {
            String[] parts = line.split(";");
            OptionalInt earnedEnvironmentEnergyHeatingIndex = findColumnIndex(parts, "EarnedEnvironmentEnergy:Heating");
            OptionalInt consumedElectricalEnergyDomesticHotWaterIndex = findColumnIndex(parts, "ConsumedElectricalEnergy:DomesticHotWater");
            OptionalInt consumedElectricalEnergyHeatingIndex = findColumnIndex(parts, "ConsumedElectricalEnergy:Heating");
            OptionalInt heatGeneratedHeatingIndex = findColumnIndex(parts, "HeatGenerated:Heating");
            OptionalInt earnedEnvironmentEnergyDomesticHotWaterIndex = findColumnIndex(parts, "EarnedEnvironmentEnergy:DomesticHotWater");
            OptionalInt heatGeneratedDomesticHotWaterIndex = findColumnIndex(parts, "HeatGenerated:DomesticHotWatery");

            while ((line = reader.readLine()) != null) {
                parts = line.split(";");
                if (parts.length >= 7) {
                    Optional<LocalDate> dt = parseLocalDate(parts[0]);
                    if (dt.isPresent()) {
                        HeatpumpEnergy data = findOrCreateHeatpumpEnergy(dt.get());
                        data.setConsumedElectricalEnergyDomesticHotWater(parseDouble(parts, consumedElectricalEnergyDomesticHotWaterIndex).orElse(null));
                        data.setConsumedElectricalEnergyHeating(parseDouble(parts, consumedElectricalEnergyHeatingIndex).orElse(null));
                        data.setEarnedEnvironmentEnergyDomesticHotWater(parseDouble(parts, earnedEnvironmentEnergyDomesticHotWaterIndex).orElse(null));
                        data.setEarnedEnvironmentEnergyHeating(parseDouble(parts, earnedEnvironmentEnergyHeatingIndex).orElse(null));
                        data.setHeatGeneratedDomesticHotWater(parseDouble(parts, heatGeneratedDomesticHotWaterIndex).orElse(null));
                        data.setHeatGeneratedHeating(parseDouble(parts, heatGeneratedHeatingIndex).orElse(null));
                    }
                }
            }
        }
    }
}
