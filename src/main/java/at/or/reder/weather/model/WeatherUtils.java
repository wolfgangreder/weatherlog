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
package at.or.reder.weather.model;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 *
 * @author Wolfgang Reder
 */
@UtilityClass
@Log
public class WeatherUtils
{

  public static ZonedDateTime parseUTCDateTime(String dt)
  {
    if (dt != null) {
      try {
        return ZonedDateTime.ofLocal(LocalDateTime.parse(dt,
                                                         DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                     ZoneId.of("UTC"),
                                     ZoneOffset.UTC);
      } catch (DateTimeParseException ex) {
        log.log(Level.WARNING,
                "Cannot parse " + dt,
                ex);
      }
    }
    return ZonedDateTime.now(ZoneId.of("UTC"));
  }

  public static Optional<Double> parseDoubleValue(String stringValue)
  {
    try {
      return Optional.of(Double.valueOf(stringValue));
    } catch (NumberFormatException ex) {
      log.log(Level.SEVERE,
              MessageFormat.format("Cannot parse {0} to double",
                                   stringValue),
              ex);
    }
    return Optional.empty();
  }

  public static Optional<Long> parseLongValue(String stringValue)
  {
    try {
      return Optional.of(Long.valueOf(stringValue));
    } catch (NumberFormatException ex) {
      log.log(Level.SEVERE,
              MessageFormat.format("Cannot parse {0} to long",
                                   stringValue),
              ex);
    }
    return Optional.empty();
  }

  public static double kmhToMs(double kmh)
  {
    return kmh * 3.6;
  }

  public static double mphToKmh(double mph)
  {
    return mph * 1.609344;
  }

  public static double inHgToHPa(double inhg)
  {
    return inhg * 33.863889532610884;
  }

  public static double fahrenheitToCelsius(double fahr)
  {
    return (fahr - 32) * 5. / 9.;
  }

  public static double inchToMm(double inch)
  {
    return inch * 25.4;
  }

  public static LocalDateTime toLocalDateTime(String utc)
  {
    return LocalDateTime.ofInstant(parseUTCDateTime(utc).toInstant(),
                                   ZoneId.systemDefault());
  }

  public static LocalDateTime convertToTimezone(LocalDateTime dt,
                                                ZoneId source,
                                                ZoneId target)
  {
    return dt.atZone(source).withZoneSameInstant(target).toLocalDateTime();
  }

  public static LocalDateTime convertLocalToUTC(LocalDateTime dt)
  {
    return convertToTimezone(dt,
                             ZoneId.systemDefault(),
                             ZoneId.of("UTC"));
  }

}
