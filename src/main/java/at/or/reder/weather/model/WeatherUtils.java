/*
 * Copyright 2024 Wolfgang Reder.
 * Licensed under the Apache License, Version 2.0
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
import java.util.logging.Logger;

public final class WeatherUtils {

    private static final Logger LOG = Logger.getLogger(WeatherUtils.class.getName());

    private WeatherUtils() {}

    public static ZonedDateTime parseUTCDateTime(String dt) {
        if (dt != null) {
            try {
                return ZonedDateTime.ofLocal(LocalDateTime.parse(dt,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        ZoneId.of("UTC"),
                        ZoneOffset.UTC);
            } catch (DateTimeParseException ex) {
                LOG.log(Level.WARNING, "Cannot parse " + dt, ex);
            }
        }
        return ZonedDateTime.now(ZoneId.of("UTC"));
    }

    public static Optional<Double> parseDoubleValue(String stringValue) {
        try {
            return Optional.of(Double.valueOf(stringValue));
        } catch (NumberFormatException ex) {
            LOG.log(Level.SEVERE,
                    MessageFormat.format("Cannot parse {0} to double", stringValue), ex);
        }
        return Optional.empty();
    }

    public static Optional<Long> parseLongValue(String stringValue) {
        try {
            return Optional.of(Long.valueOf(stringValue));
        } catch (NumberFormatException ex) {
            LOG.log(Level.SEVERE,
                    MessageFormat.format("Cannot parse {0} to long", stringValue), ex);
        }
        return Optional.empty();
    }

    public static double kmhToMs(double kmh) { return kmh * 3.6; }
    public static double mphToKmh(double mph) { return mph * 1.609344; }
    public static double inHgToHPa(double inhg) { return inhg * 33.863889532610884; }
    public static double fahrenheitToCelsius(double fahr) { return (fahr - 32) * 5. / 9.; }
    public static double inchToMm(double inch) { return inch * 25.4; }

    public static LocalDateTime toLocalDateTime(String utc) {
        return LocalDateTime.ofInstant(parseUTCDateTime(utc).toInstant(), ZoneId.systemDefault());
    }

    public static LocalDateTime convertToTimezone(LocalDateTime dt, ZoneId source, ZoneId target) {
        return dt.atZone(source).withZoneSameInstant(target).toLocalDateTime();
    }

    public static LocalDateTime convertLocalToUTC(LocalDateTime dt) {
        return convertToTimezone(dt, ZoneId.systemDefault(), ZoneId.of("UTC"));
    }
}
