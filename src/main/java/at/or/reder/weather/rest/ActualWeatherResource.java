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

import at.or.reder.weather.model.WeatherRecord;
import at.or.reder.weather.model.WeatherUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;

/**
 *
 * @author Wolfgang Reder
 */
//@ApplicationScoped
//@Path("/actual")
public class ActualWeatherResource
{

  @PersistenceContext(name = "WEATHER")
  private EntityManager entityManager;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCurrentWeather()
  {
    List<WeatherRecord> resultList = entityManager.createQuery("select wr from WeatherRecord wr order by wr.sampleTime desc",
                                                               WeatherRecord.class)
            .setMaxResults(1)
            .getResultList();
    if (!resultList.isEmpty()) {
      return Response.ok(resultList.get(0)).build();
    }
    return Response.status(404).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Transactional(Transactional.TxType.REQUIRED)
  public Response receiveWeather(@FormParam("PASSKEY") String passKey,
                                 @FormParam("stationtype") String stationtype,
                                 @FormParam("runtime") long runtime,
                                 @FormParam("heap") long heap,
                                 @FormParam("dateutc") String dateutc,
                                 @FormParam("tempinf") double tempinf,
                                 @FormParam("humidityin") double humidityin,
                                 @FormParam("baromrelin") double baromrelin,
                                 @FormParam("baromabsin") double baromabsin,
                                 @FormParam("tempf") double tempf,
                                 @FormParam("humidity") double humidity,
                                 @FormParam("winddir") int winddir,
                                 @FormParam("windspeedmph") double windspeedmph,
                                 @FormParam("windgustmph") double windgustmph,
                                 @FormParam("maxdailygust") double maxdailygust,
                                 @FormParam("solarradiation") double solarradiation,
                                 @FormParam("uv") int uv,
                                 @FormParam("rainratein") double rainratein,
                                 @FormParam("eventrainin") double eventrainin,
                                 @FormParam("hourlyrainin") double hourlyrainin,
                                 @FormParam("dailyrainin") double dailyrainin,
                                 @FormParam("weeklyrainin") double weeklyrainin,
                                 @FormParam("monthlyrainin") double monthlyrainin,
                                 @FormParam("yearlyrainin") double yearlyrainin,
                                 @FormParam("totalrainin") double totalrainin,
                                 @FormParam("wh65batt") int wh65batt,
                                 @FormParam("freq") String freq,
                                 @FormParam("model") String model,
                                 @FormParam("interval") int interval)
  {
    WeatherRecord result = new WeatherRecord();
    result.setStationkey(passKey);
    result.setStationtype(stationtype);
    result.setTempin(WeatherUtils.fahrenheitToCelsius(tempinf));
    result.setRuntime(runtime);
    result.setHeap(heap);
    result.setFreq(parseFrequency(freq));
    result.setWh65batt(wh65batt);
    result.setModel(model);
    result.setInterval(interval);
    result.setTempout(WeatherUtils.fahrenheitToCelsius(tempf));
    result.setHumidityin(humidityin);
    result.setHumidityout(humidity);
    result.setPressurerel(WeatherUtils.inHgToHPa(baromrelin));
    result.setPressureabs(WeatherUtils.inHgToHPa(baromabsin));
    result.setWinddir(winddir);
    result.setWindspeed(WeatherUtils.mphToKmh(windspeedmph));
    result.setWindgust(WeatherUtils.mphToKmh(windgustmph));
    result.setMaxdailygust(WeatherUtils.mphToKmh(maxdailygust));
    result.setSolarradiation(solarradiation);
    result.setUv(uv);
    result.setRainrate(WeatherUtils.inchToMm(rainratein));
    result.setEventrain(WeatherUtils.inchToMm(eventrainin));
    result.setHourlyrain(WeatherUtils.inchToMm(hourlyrainin));
    result.setDailyrain(WeatherUtils.inchToMm(dailyrainin));
    result.setWeeklyrain(WeatherUtils.inchToMm(weeklyrainin));
    result.setMonthlyrain(WeatherUtils.inchToMm(monthlyrainin));
    result.setYearlyrain(WeatherUtils.inchToMm(yearlyrainin));
    result.setTotalrain(WeatherUtils.inchToMm(totalrainin));
    ZonedDateTime dt = WeatherUtils.parseUTCDateTime(dateutc);
    result.setSampleTime(dt.toLocalDateTime());
    entityManager.persist(result);
    return Response.noContent().build();
  }

  private static int parseFrequency(String strFrequency)
  {
    String toParse;
    if (strFrequency.endsWith("M")) {
      toParse = strFrequency.substring(0,
                                       strFrequency.length() - 1);
    } else {
      toParse = strFrequency;
    }
    return WeatherUtils.parseLongValue(toParse).map(Long::intValue).orElse(-1);
  }

}
