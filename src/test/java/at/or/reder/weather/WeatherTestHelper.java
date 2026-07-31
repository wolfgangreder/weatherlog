package at.or.reder.weather;

import at.or.reder.weather.model.WeatherRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
class WeatherTestHelper {

    @Transactional
    public void clearWeatherData() {
        WeatherRecord.deleteAll();
    }
}
