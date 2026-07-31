package at.or.reder.weather;

import at.or.reder.weather.model.WeatherUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class WeatherUtilsTest {

    private static final double DELTA = 0.0001;

    // --- fahrenheitToCelsius ---

    @Test
    public void fahrenheitToCelsius_freezingPoint() {
        assertEquals(0.0, WeatherUtils.fahrenheitToCelsius(32.0), DELTA);
    }

    @Test
    public void fahrenheitToCelsius_boilingPoint() {
        assertEquals(100.0, WeatherUtils.fahrenheitToCelsius(212.0), DELTA);
    }

    @Test
    public void fahrenheitToCelsius_negativeForty() {
        // -40 is the same in both scales
        assertEquals(-40.0, WeatherUtils.fahrenheitToCelsius(-40.0), DELTA);
    }

    // --- inHgToHPa ---

    @Test
    public void inHgToHPa_standardAtmosphere() {
        // 29.9212 inHg ≈ 1013.25 hPa
        assertEquals(1013.25, WeatherUtils.inHgToHPa(29.9212), 0.05);
    }

    // --- mphToKmh ---

    @Test
    public void mphToKmh_oneMph() {
        assertEquals(1.609344, WeatherUtils.mphToKmh(1.0), DELTA);
    }

    @Test
    public void mphToKmh_zero() {
        assertEquals(0.0, WeatherUtils.mphToKmh(0.0), DELTA);
    }

    // --- inchToMm ---

    @Test
    public void inchToMm_oneInch() {
        assertEquals(25.4, WeatherUtils.inchToMm(1.0), DELTA);
    }

    @Test
    public void inchToMm_zero() {
        assertEquals(0.0, WeatherUtils.inchToMm(0.0), DELTA);
    }

    // --- parseUTCDateTime ---

    @Test
    public void parseUTCDateTime_validString() {
        ZonedDateTime result = WeatherUtils.parseUTCDateTime("2024-01-15 12:00:00");
        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(12, result.getHour());
        assertEquals(0, result.getMinute());
        assertEquals(ZoneOffset.UTC, result.getOffset());
    }

    @Test
    public void parseUTCDateTime_nullInput_returnsNow() {
        ZonedDateTime before = ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(5);
        ZonedDateTime result = WeatherUtils.parseUTCDateTime(null);
        ZonedDateTime after = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(5);
        assertNotNull(result);
        assertTrue(!result.isBefore(before) && !result.isAfter(after),
                "Expected result close to now, got: " + result);
    }

    // --- convertLocalToUTC / convertToTimezone ---

    @Test
    public void convertLocalToUTC_roundTrip() {
        LocalDateTime original = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
        LocalDateTime utc = WeatherUtils.convertLocalToUTC(original);
        // Converting back to system default should give original
        LocalDateTime roundTripped = WeatherUtils.convertToTimezone(
                utc,
                java.time.ZoneId.of("UTC"),
                java.time.ZoneId.systemDefault());
        assertEquals(original, roundTripped);
    }
}
