package at.or.reder.weather;

import at.or.reder.weather.fixture.WeatherRecordFixture;
import at.or.reder.weather.model.WeatherRecord;
import at.or.reder.weather.model.WeatherSample;
import at.or.reder.weather.service.WeatherService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(H2TestProfile.class)
public class WeatherServiceTest {

    @Inject
    WeatherService weatherService;

    @Inject
    WeatherTestHelper helper;

    @BeforeEach
    void setUp() {
        helper.clearWeatherData();
    }

    // --- getCurrent ---

    @Test
    public void getCurrent_emptyDatabase_returnsEmpty() {
        Optional<WeatherRecord> result = weatherService.getCurrent();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getCurrent_afterStore_returnsStoredRecord() {
        WeatherRecord record = new WeatherRecordFixture()
                .withSampleTime(LocalDateTime.of(2024, 1, 15, 12, 0, 0))
                .withTempout(15.5)
                .build();

        weatherService.storeCurrentWeather(record);

        Optional<WeatherRecord> result = weatherService.getCurrent();
        assertTrue(result.isPresent());
        assertEquals(LocalDateTime.of(2024, 1, 15, 12, 0, 0), result.get().getSampleTime());
        assertEquals(15.5, result.get().getTempout(), 0.001);
    }

    @Test
    public void getCurrent_multipleRecords_returnsMostRecent() {
        WeatherRecord older = new WeatherRecordFixture()
                .withSampleTime(LocalDateTime.of(2024, 1, 15, 10, 0, 0))
                .withTempout(10.0)
                .build();
        WeatherRecord newer = new WeatherRecordFixture()
                .withSampleTime(LocalDateTime.of(2024, 1, 15, 11, 0, 0))
                .withTempout(12.0)
                .build();

        weatherService.storeCurrentWeather(older);
        weatherService.storeCurrentWeather(newer);

        Optional<WeatherRecord> result = weatherService.getCurrent();
        assertTrue(result.isPresent());
        assertEquals(LocalDateTime.of(2024, 1, 15, 11, 0, 0), result.get().getSampleTime());
    }

    // --- getWeatherData ---

    @Test
    public void getWeatherData_emptyDatabase_returnsEmptyRecordList() {
        LocalDateTime from = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2024, 1, 15, 23, 59, 59);

        WeatherSample sample = weatherService.getWeatherData(from, to);

        assertNotNull(sample);
        assertNotNull(sample.getRecords());
        assertTrue(sample.getRecords().isEmpty());
    }

    @Test
    public void getWeatherData_recordInsideWindow_included() {
        LocalDateTime from   = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime inside = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        LocalDateTime to     = LocalDateTime.of(2024, 1, 15, 23, 59, 59);

        weatherService.storeCurrentWeather(new WeatherRecordFixture().withSampleTime(inside).build());

        WeatherSample sample = weatherService.getWeatherData(from, to);
        assertEquals(1, sample.getRecords().size());
        assertEquals(inside, sample.getRecords().get(0).getSampleTime());
    }

    @Test
    public void getWeatherData_recordBeforeWindow_excluded() {
        LocalDateTime before = LocalDateTime.of(2024, 1, 14, 23, 59, 0);
        LocalDateTime from   = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime to     = LocalDateTime.of(2024, 1, 15, 23, 59, 59);

        weatherService.storeCurrentWeather(new WeatherRecordFixture().withSampleTime(before).build());

        WeatherSample sample = weatherService.getWeatherData(from, to);
        assertTrue(sample.getRecords().isEmpty());
    }

    @Test
    public void getWeatherData_recordAtFrom_included() {
        LocalDateTime from = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2024, 1, 15, 23, 59, 59);

        weatherService.storeCurrentWeather(new WeatherRecordFixture().withSampleTime(from).build());

        WeatherSample sample = weatherService.getWeatherData(from, to);
        assertEquals(1, sample.getRecords().size());
    }

    @Test
    public void getWeatherData_recordAtTo_excluded() {
        LocalDateTime from = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2024, 1, 15, 23, 59, 59);

        weatherService.storeCurrentWeather(new WeatherRecordFixture().withSampleTime(to).build());

        WeatherSample sample = weatherService.getWeatherData(from, to);
        assertTrue(sample.getRecords().isEmpty());
    }

    @Test
    public void getWeatherData_multipleRecords_returnedOrderedBySampleTime() {
        LocalDateTime from = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime t1   = LocalDateTime.of(2024, 1, 15, 8, 0, 0);
        LocalDateTime t2   = LocalDateTime.of(2024, 1, 15, 16, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2024, 1, 15, 23, 59, 59);

        // Store in reverse order to verify ordering is by sampleTime, not insert order
        weatherService.storeCurrentWeather(new WeatherRecordFixture().withSampleTime(t2).build());
        weatherService.storeCurrentWeather(new WeatherRecordFixture().withSampleTime(t1).build());

        WeatherSample sample = weatherService.getWeatherData(from, to);
        assertEquals(2, sample.getRecords().size());
        assertEquals(t1, sample.getRecords().get(0).getSampleTime());
        assertEquals(t2, sample.getRecords().get(1).getSampleTime());
    }

    @Test
    public void getWeatherData_generatedFieldNotNull() {
        LocalDateTime from = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2024, 1, 15, 23, 59, 59);

        WeatherSample sample = weatherService.getWeatherData(from, to);
        assertNotNull(sample.getGenerated());
    }
}
