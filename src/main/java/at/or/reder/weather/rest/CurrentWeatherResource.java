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
import at.or.reder.weather.model.WeatherSample;
import at.or.reder.weather.model.WeatherUtils;
import at.or.reder.weather.service.WeatherService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import lombok.extern.java.Log;

/**
 *
 * @author Wolfgang Reder
 */
@ApplicationScoped
@Path("current")
@Log
public class CurrentWeatherResource
{

  @Inject
  private WeatherService weatherService;

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
    weatherService.storeCurrentWeather(result);
    return Response.noContent().build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public WeatherRecord getCurrentWeather()
  {
    return weatherService.getCurrent().orElse(null);
  }

  @GET
  @Path("day")
  @Produces(MediaType.APPLICATION_JSON)
  public WeatherSample getDay(@QueryParam("running") boolean runningDay,
                              @QueryParam("include") String[] fieldsToInclude)
  {
    LocalDateTime now = WeatherUtils.convertLocalToUTC(LocalDateTime.now());
    LocalDateTime limit;
    if (runningDay) {
      limit = WeatherUtils.convertLocalToUTC(LocalDateTime.now().minusHours(24));
    } else {
      limit = WeatherUtils.convertLocalToUTC(LocalDateTime.of(LocalDate.now(),
                                                              LocalTime.MIDNIGHT));
    }
    WeatherSample sample = weatherService.getWeatherData(limit,
                                                         now);
    if (fieldsToInclude != null && fieldsToInclude.length > 0) {
      RecordFilter filter = new RecordFilter(fieldsToInclude);
      sample.getRecords().forEach(filter);
    }
    return sample;
  }

  private static final class RecordFilter implements Consumer<WeatherRecord>
  {

    private final List<Method> nullSetter;

    public RecordFilter(String[] fieldsToInclude)
    {
      nullSetter = buildFilter(fieldsToInclude);
    }

    @Override
    public void accept(WeatherRecord t)
    {
      for (Method setter : nullSetter) {
        try {
          setter.invoke(t,
                        (Object) null);
        } catch (Exception ex) {
          log.log(Level.WARNING,
                  "Cannot invoke setter",
                  ex);
        }
      }
    }

    private Set<String> buildMethodNames(String[] fieldsToInclude)
    {
      Set<String> result = new HashSet<>();
      for (String fieldName : fieldsToInclude) {
        result.add("set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
      }
      result.add("setSampleTime");
      return result;
    }

    private static final Set<Class<?>> PRIMITIVE_CLASSES = Set.of(int.class,
                                                                  long.class,
                                                                  double.class,
                                                                  char.class,
                                                                  byte.class,
                                                                  float.class);

    private List<Method> collectSetter()
    {
      Class<WeatherRecord> clazz = WeatherRecord.class;
      Method[] methods = clazz.getMethods();
      List<Method> result = new ArrayList<>();
      for (Method method : methods) {
        if (method.getName().startsWith("set") && method.getParameterCount() == 1 && !PRIMITIVE_CLASSES.contains(method.getParameterTypes()[0])) {
          result.add(method);
        }
      }
      return result;
    }

    private List<Method> buildFilter(String[] fieldsToInclude)
    {
      Set<String> methodNames = buildMethodNames(fieldsToInclude);
      return collectSetter().stream()
              .filter(m -> !methodNames.contains(m.getName()))
              .collect(Collectors.toList());
    }

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
