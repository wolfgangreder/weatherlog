/*
 * Copyright 2024 wolfi.
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

import at.or.reder.weather.model.HeatpumpData;
import at.or.reder.weather.model.HeatpumpEnergy;
import at.or.reder.weather.model.HeatpumpEnergyRecord;
import at.or.reder.weather.model.HeatpumpScope;
import at.or.reder.weather.model.WeatherUtils;
import at.or.reder.weather.service.HeatpumpService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
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
import lombok.extern.java.Log;

@ApplicationScoped
@Default
@Log
public class HeatpumpServiceImpl implements HeatpumpService {

  @PersistenceContext(name = "WEATHER")
  private EntityManager entityManager;

  @Override
  public Optional<HeatpumpEnergyRecord> getEnergy(HeatpumpScope scope, LocalDate day)
  {
    try {
      HeatpumpEnergy record = entityManager.createQuery("select hpr from HeatpumpEnergy hpr where hpr.sampleday=?1",
              HeatpumpEnergy.class)
              .setParameter(1, day)
              .getSingleResult();
      return Optional.ofNullable(switch (scope) {
        case HEATING ->
          HeatpumpEnergyRecord.builder().date(record.getSampleday())
          .energyConsumed(record.getConsumedElectricalEnergyHeating())
          .energyGenerated(record.getHeatGeneratedHeating()).build();
        case WATER ->
          HeatpumpEnergyRecord.builder().date(record.getSampleday())
          .energyConsumed(record.getConsumedElectricalEnergyDomesticHotWater())
          .energyGenerated(record.getHeatGeneratedDomesticHotWater()).build();
        default ->
          null;
      });
    } catch (NoResultException | NonUniqueResultException ex) {
      log.log(Level.WARNING, "Cannot load energydata for {0}", day.toString());
    }
    return Optional.empty();
  }

  private HeatpumpData findHeatpumpData(LocalDateTime sampleTime)
  {
    HeatpumpData result;
    List<HeatpumpData> list = entityManager.createQuery("select hp from HeatpumpData hp where hp.sampletime=?1",
            HeatpumpData.class)
            .setParameter(1,
                    sampleTime)
            .setMaxResults(1)
            .getResultList();
    if (list.isEmpty()) {
      result = new HeatpumpData();
      result.setSampletime(sampleTime);
      entityManager.persist(result);
    } else {
      result = list.get(0);
    }
    return result;
  }

  private HeatpumpEnergy findHeatpumpEnergy(LocalDate sampleDay)
  {
    HeatpumpEnergy result;
    List<HeatpumpEnergy> list = entityManager.createQuery("select he from HeatpumpEnergy he where he.sampleday=?1",
            HeatpumpEnergy.class)
            .setParameter(1,
                    sampleDay)
            .setMaxResults(1)
            .getResultList();
    if (list.isEmpty()) {
      result = new HeatpumpEnergy();
      result.setSampleday(sampleDay);
      entityManager.persist(result);
    } else {
      result = list.get(0);
    }
    return result;
  }

  private Optional<LocalDateTime> parseLocalDateTime(String str)
  {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss");
    try {
      return Optional.of(WeatherUtils.convertToTimezone(LocalDateTime.parse(str,
              format),
              ZoneId.of("CET"),
              ZoneId.of("UTC")));
    } catch (DateTimeParseException ex) {
      // do nothing
    }
    return Optional.empty();
  }

  private Optional<LocalDate> parseLocalDate(String str)
  {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss");
    try {
      return Optional.of(LocalDateTime.parse(str,
              format).toLocalDate());
    } catch (DateTimeParseException ex) {
      // do nothing
    }
    return Optional.empty();
  }

  private OptionalInt findColumnIndex(String[] columns, String columnName)
  {
    for (int i = 0; i < columns.length; ++i) {
      if (columns[i].equals(columnName)) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  private String readNextNoCommentLine(LineNumberReader reader) throws IOException
  {
    String line;
    while ((line = reader.readLine()) != null) {
      if (!line.startsWith("#")) {
        return line;
      }
    }
    return null;
  }

  @Override
  @Transactional(Transactional.TxType.REQUIRED)
  public void insertZoneData(LineNumberReader reader) throws IOException
  {
    String line = readNextNoCommentLine(reader);
    if (line != null) {
      String parts[] = line.split(";");
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
              HeatpumpData data = findHeatpumpData(dt.get());
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
  public void insertHotWaterData(LineNumberReader reader) throws IOException
  {
    String line;
    while ((line = reader.readLine()) != null) {
      String parts[] = line.split(";");
      if (parts.length >= 2) {
        Optional<LocalDateTime> dt = parseLocalDateTime(parts[0]);
        if (dt.isPresent()) {
          Optional<Double> hotWater = WeatherUtils.parseDoubleValue(parts[1]);
          if (hotWater.isPresent()) {
            HeatpumpData data = findHeatpumpData(dt.get());
            data.setHotWaterTemp(hotWater.get());
          }
        }
      }
    }
  }

  @Override
  @Transactional(Transactional.TxType.REQUIRED)
  public void insertSystemData(LineNumberReader reader) throws IOException
  {
    String line = readNextNoCommentLine(reader);
    if (line != null) {
      while ((line = readNextNoCommentLine(reader)) != null) {
        String parts[] = line.split(";");
        if (parts.length >= 2) {
          Optional<LocalDateTime> dt = parseLocalDateTime(parts[0]);
          if (dt.isPresent()) {
            Optional<Double> outDoor = WeatherUtils.parseDoubleValue(parts[1]);
            if (outDoor.isPresent()) {
              HeatpumpData data = findHeatpumpData(dt.get());
              data.setOutdoorTemp(outDoor.get());
            }
          }
        }
      }
    }
  }

  Optional<Double> parseDouble(String[] parts, OptionalInt index)
  {
    if (index.isPresent()) {
      String part = parts[index.getAsInt()];
      return WeatherUtils.parseDoubleValue(part);
    }
    return Optional.empty();
  }

  @Override
  @Transactional(Transactional.TxType.REQUIRED)
  public void insertEnergyData(LineNumberReader reader) throws IOException
  {
    String line = readNextNoCommentLine(reader);
    if (line != null) {
      String[] parts = line.split(";");
      OptionalInt earnedEnvironmentEnergyHeatingIndex = findColumnIndex(parts, "EarnedEnvironmentEnergy:Heating");
      OptionalInt consumedElectricalEnergyDomesticHotWaterIndex = findColumnIndex(parts,
              "ConsumedElectricalEnergy:DomesticHotWater");
      OptionalInt consumedElectricalEnergyHeatingIndex = findColumnIndex(parts, "ConsumedElectricalEnergy:Heating");
      OptionalInt heatGeneratedHeatingIndex = findColumnIndex(parts, "HeatGenerated:Heating");
      OptionalInt earnedEnvironmentEnergyDomesticHotWaterIndex = findColumnIndex(parts,
              "EarnedEnvironmentEnergy:DomesticHotWater");
      OptionalInt heatGeneratedDomesticHotWaterIndex = findColumnIndex(parts, "HeatGenerated:DomesticHotWatery");

      while ((line = reader.readLine()) != null) {
        parts = line.split(";");
        if (parts.length >= 7) {
          Optional<LocalDate> dt = parseLocalDate(parts[0]);
          if (dt.isPresent()) {
            Optional<Double> earnedEnvironmentEnergyHeating = parseDouble(parts, earnedEnvironmentEnergyHeatingIndex);
            Optional<Double> consumedElectricalEnergyDomesticHotWater = parseDouble(parts,
                    consumedElectricalEnergyDomesticHotWaterIndex);
            Optional<Double> consumedElectricalEnergyHeating = parseDouble(parts, consumedElectricalEnergyHeatingIndex);
            Optional<Double> heatGeneratedHeating = parseDouble(parts, heatGeneratedHeatingIndex);
            Optional<Double> earnedEnvironmentEnergyDomesticHotWater = parseDouble(parts,
                    earnedEnvironmentEnergyDomesticHotWaterIndex);
            Optional<Double> heatGeneratedDomesticHotWater = parseDouble(parts, heatGeneratedDomesticHotWaterIndex);
            HeatpumpEnergy data = findHeatpumpEnergy(dt.get());
            data.setConsumedElectricalEnergyDomesticHotWater(consumedElectricalEnergyDomesticHotWater.orElse(null));
            data.setConsumedElectricalEnergyHeating(consumedElectricalEnergyHeating.orElse(null));
            data.setEarnedEnvironmentEnergyDomesticHotWater(earnedEnvironmentEnergyDomesticHotWater.orElse(null));
            data.setEarnedEnvironmentEnergyHeating(earnedEnvironmentEnergyHeating.orElse(null));
            data.setHeatGeneratedDomesticHotWater(heatGeneratedDomesticHotWater.orElse(null));
            data.setHeatGeneratedHeating(heatGeneratedHeating.orElse(null));
          }
        }
      }
    }
  }
}
