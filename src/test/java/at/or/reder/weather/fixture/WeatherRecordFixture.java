package at.or.reder.weather.fixture;

import at.or.reder.weather.model.WeatherRecord;
import java.time.LocalDateTime;

public class WeatherRecordFixture {

    private LocalDateTime sampleTime = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
    private double tempout = 20.0;
    private double tempin = 21.0;
    private double pressureabs = 1013.25;
    private double pressurerel = 1013.25;
    private double humidityout = 60.0;
    private double humidityin = 60.0;
    private double windspeed = 10.0;
    private double windgust = 10.0;
    private double maxdailygust = 10.0;
    private double winddir = 180.0;
    private double solarradiation = 500.0;
    private int uv = 3;
    private double rainrate = 0.0;
    private double eventrain = 0.0;
    private double hourlyrain = 0.0;
    private double dailyrain = 0.0;
    private double weeklyrain = 0.0;
    private double monthlyrain = 0.0;
    private double yearlyrain = 0.0;
    private double totalrain = 0.0;
    private String stationkey = "TEST_STATION";

    public WeatherRecordFixture withSampleTime(LocalDateTime sampleTime) {
        this.sampleTime = sampleTime;
        return this;
    }

    public WeatherRecordFixture withTempout(double tempout) {
        this.tempout = tempout;
        return this;
    }

    public WeatherRecordFixture withPressureabs(double pressureabs) {
        this.pressureabs = pressureabs;
        return this;
    }

    public WeatherRecord build() {
        WeatherRecord r = new WeatherRecord();
        r.setStationkey(stationkey);
        r.setSampleTime(sampleTime);
        r.setTempout(tempout);
        r.setTempin(tempin);
        r.setPressureabs(pressureabs);
        r.setPressurerel(pressurerel);
        r.setHumidityout(humidityout);
        r.setHumidityin(humidityin);
        r.setWindspeed(windspeed);
        r.setWindgust(windgust);
        r.setMaxdailygust(maxdailygust);
        r.setWinddir(winddir);
        r.setSolarradiation(solarradiation);
        r.setUv(uv);
        r.setRainrate(rainrate);
        r.setEventrain(eventrain);
        r.setHourlyrain(hourlyrain);
        r.setDailyrain(dailyrain);
        r.setWeeklyrain(weeklyrain);
        r.setMonthlyrain(monthlyrain);
        r.setYearlyrain(yearlyrain);
        r.setTotalrain(totalrain);
        return r;
    }
}
