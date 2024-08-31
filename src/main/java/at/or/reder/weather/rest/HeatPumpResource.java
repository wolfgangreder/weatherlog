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
package at.or.reder.weather.rest;

import at.or.reder.weather.model.HeatpumpData;
import at.or.reder.weather.model.HeatpumpEnergy;
import at.or.reder.weather.model.WeatherUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Wolfgang Reder
 */
@Path("/heatpump")
@ApplicationScoped
public class HeatPumpResource
{

  @PersistenceContext(name = "WEATHER")
  private EntityManager em;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Transactional(Transactional.TxType.REQUIRED)
  public Response uploadData(@Context HttpServletRequest request) throws ServletException, IOException
  {
    Collection<Part> parts = request.getParts();
    for (Part part : parts) {
      String filename = part.getSubmittedFileName();
      try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(part.getInputStream()))) {
        if (filename.startsWith("domestic_hot_water")) {
          parseHotWater(reader);
        } else if (filename.startsWith("system")) {
          parseSystemData(reader);
        } else if (filename.startsWith("zone")) {
          parseZoneData(reader);
        } else if (filename.startsWith("energy")) {
          parseEnergyData(reader);
        }
      }
    }
    return Response.noContent().build();
  }

  private HeatpumpData findHeatpumpData(LocalDateTime sampleTime)
  {
    HeatpumpData result;
    List<HeatpumpData> list = em.createQuery("select hp from HeatpumpData hp where hp.sampletime=?1",
                                             HeatpumpData.class)
            .setParameter(1,
                          sampleTime)
            .setMaxResults(1)
            .getResultList();
    if (list.isEmpty()) {
      result = new HeatpumpData();
      result.setSampletime(sampleTime);
      em.persist(result);
    } else {
      result = list.get(0);
    }
    return result;
  }

  private HeatpumpEnergy findHeatpumpEnergy(LocalDate sampleDay)
  {
    HeatpumpEnergy result;
    List<HeatpumpEnergy> list = em.createQuery("select he from HeatpumpEnergy he where he.sampleday=?1",
                                               HeatpumpEnergy.class)
            .setParameter(1,
                          sampleDay)
            .setMaxResults(1)
            .getResultList();
    if (list.isEmpty()) {
      result = new HeatpumpEnergy();
      result.setSampleday(sampleDay);
      em.persist(result);
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

  private void parseZoneData(LineNumberReader reader) throws IOException
  {
    String line;
    while ((line = reader.readLine()) != null) {
      String parts[] = line.split(";");
      if (parts.length >= 3) {
        Optional<LocalDateTime> dt = parseLocalDateTime(parts[0]);
        if (dt.isPresent()) {
          Optional<Double> currentRoom = WeatherUtils.parseDoubleValue(parts[1]);
          Optional<Double> roomSet = WeatherUtils.parseDoubleValue(parts[2]);
          if (currentRoom.isPresent() && roomSet.isPresent()) {
            HeatpumpData data = findHeatpumpData(dt.get());
            data.setRoomTemp(currentRoom.get());
            data.setRoomTempSet(roomSet.get());
          }
        }
      }
    }
  }

  private void parseHotWater(LineNumberReader reader) throws IOException
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

  private void parseSystemData(LineNumberReader reader) throws IOException
  {
    String line;
    while ((line = reader.readLine()) != null) {
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

  private void parseEnergyData(LineNumberReader reader) throws IOException
  {
    String line;
    while ((line = reader.readLine()) != null) {
      String parts[] = line.split(";");
      if (parts.length >= 7) {
        Optional<LocalDate> dt = parseLocalDate(parts[0]);
        if (dt.isPresent()) {
          Optional<Double> earnedEnvironmentEnergyHeating = WeatherUtils.parseDoubleValue(parts[1]);
          Optional<Double> consumedElectricalEnergyDomesticHotWater = WeatherUtils.parseDoubleValue(parts[2]);
          Optional<Double> consumedElectricalEnergyHeating = WeatherUtils.parseDoubleValue(parts[3]);
          Optional<Double> heatGeneratedHeating = WeatherUtils.parseDoubleValue(parts[4]);
          Optional<Double> earnedEnvironmentEnergyDomesticHotWater = WeatherUtils.parseDoubleValue(parts[5]);
          Optional<Double> heatGeneratedDomesticHotWater = WeatherUtils.parseDoubleValue(parts[6]);
          if (earnedEnvironmentEnergyHeating.isPresent()
              && consumedElectricalEnergyDomesticHotWater.isPresent()
              && consumedElectricalEnergyHeating.isPresent()
              && heatGeneratedHeating.isPresent()
              && earnedEnvironmentEnergyDomesticHotWater.isPresent()
              && heatGeneratedDomesticHotWater.isPresent()) {
            HeatpumpEnergy data = findHeatpumpEnergy(dt.get());
            data.setConsumedElectricalEnergyDomesticHotWater(consumedElectricalEnergyDomesticHotWater.get());
            data.setConsumedElectricalEnergyHeating(consumedElectricalEnergyHeating.get());
            data.setEarnedEnvironmentEnergyDomesticHotWater(earnedEnvironmentEnergyDomesticHotWater.get());
            data.setEarnedEnvironmentEnergyHeating(earnedEnvironmentEnergyHeating.get());
            data.setHeatGeneratedDomesticHotWater(heatGeneratedDomesticHotWater.get());
            data.setHeatGeneratedHeating(heatGeneratedHeating.get());
          }
        }
      }
    }
  }

}
