package at.or.reder.weather;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestProfile(H2TestProfile.class)
public class CurrentWeatherResourceTest {

    @Inject
    WeatherTestHelper helper;

    @BeforeEach
    void setUp() {
        // Parse JSON numbers as Double (REST-Assured Groovy parser defaults to Float)
        RestAssured.config = RestAssured.config()
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE));
        helper.clearWeatherData();
    }

    // ---- Helpers ----

    private String nowUtcString() {
        return LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * POST a weather reading with representative imperial values and a specific UTC timestamp.
     * tempf=68.0°F → 20°C, baromabsin=29.9212 inHg → 1013.25 hPa,
     * windspeedmph=6.2137 → 10.0 km/h, eventrainin=0.3937 → 10.0 mm
     */
    private void postWeatherReading(String dateutc) {
        given()
            .contentType(ContentType.URLENC)
            .formParam("PASSKEY",       "TEST")
            .formParam("stationtype",   "test")
            .formParam("dateutc",       dateutc)
            .formParam("freq",          "915M")
            .formParam("model",         "WS2900")
            .formParam("runtime",       0)
            .formParam("heap",          0)
            .formParam("tempinf",       71.6)
            .formParam("humidityin",    50.0)
            .formParam("baromrelin",    29.9212)
            .formParam("baromabsin",    29.9212)
            .formParam("tempf",         68.0)
            .formParam("humidity",      60.0)
            .formParam("winddir",       180)
            .formParam("windspeedmph",  6.2137)
            .formParam("windgustmph",   6.2137)
            .formParam("maxdailygust",  6.2137)
            .formParam("solarradiation",500.0)
            .formParam("uv",            3)
            .formParam("rainratein",    0.0)
            .formParam("eventrainin",   0.3937)
            .formParam("hourlyrainin",  0.0)
            .formParam("dailyrainin",   0.0)
            .formParam("weeklyrainin",  0.0)
            .formParam("monthlyrainin", 0.0)
            .formParam("yearlyrainin",  0.0)
            .formParam("totalrainin",   0.0)
            .formParam("wh65batt",      0)
            .formParam("interval",      60)
        .when()
            .post("/weather/current")
        .then()
            .statusCode(204);
    }

    // ---- Tests ----

    @Test
    public void getCurrentWeather_emptyDatabase_returns204() {
        given()
            .when().get("/weather/current")
            .then().statusCode(204);
    }

    @Test
    public void postWeather_returns204() {
        postWeatherReading(nowUtcString());
        // 204 is asserted inside postWeatherReading
    }

    @Test
    public void postThenGet_tempoutConvertedFromFahrenheit() {
        postWeatherReading(nowUtcString());

        given()
            .when().get("/weather/current")
            .then()
            .statusCode(200)
            .body("tempout", closeTo(20.0, 0.05));
    }

    @Test
    public void postThenGet_pressureabsConvertedFromInHg() {
        postWeatherReading(nowUtcString());

        given()
            .when().get("/weather/current")
            .then()
            .statusCode(200)
            .body("pressureabs", closeTo(1013.25, 0.5));
    }

    @Test
    public void postThenGet_windspeedConvertedFromMph() {
        postWeatherReading(nowUtcString());

        given()
            .when().get("/weather/current")
            .then()
            .statusCode(200)
            .body("windspeed", closeTo(10.0, 0.05));
    }

    @Test
    public void postThenGet_eventrainConvertedFromInch() {
        postWeatherReading(nowUtcString());

        given()
            .when().get("/weather/current")
            .then()
            .statusCode(200)
            .body("eventrain", closeTo(10.0, 0.05));
    }

    @Test
    public void getDayWeather_emptyDatabase_returnsEmptyRecordsList() {
        given()
            .when().get("/weather/current/day")
            .then()
            .statusCode(200)
            .body("records", hasSize(0));
    }

    @Test
    public void getDayWeather_afterPost_returnsOneRecord() {
        postWeatherReading(nowUtcString());

        given()
            .queryParam("running", true)
            .when().get("/weather/current/day")
            .then()
            .statusCode(200)
            .body("records", hasSize(1));
    }

    @Test
    public void postTwice_getCurrentReturnsLaterRecord() {
        String earlier = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String later   = nowUtcString();

        // Earlier reading: tempf=50°F → 10°C
        given()
            .contentType(ContentType.URLENC)
            .formParam("PASSKEY", "TEST").formParam("stationtype", "test")
            .formParam("dateutc", earlier).formParam("freq", "915M")
            .formParam("model", "WS2900").formParam("runtime", 0).formParam("heap", 0)
            .formParam("tempinf", 71.6).formParam("humidityin", 50.0)
            .formParam("baromrelin", 29.9212).formParam("baromabsin", 29.9212)
            .formParam("tempf", 50.0)
            .formParam("humidity", 60.0).formParam("winddir", 180)
            .formParam("windspeedmph", 0.0).formParam("windgustmph", 0.0)
            .formParam("maxdailygust", 0.0).formParam("solarradiation", 0.0)
            .formParam("uv", 0).formParam("rainratein", 0.0).formParam("eventrainin", 0.0)
            .formParam("hourlyrainin", 0.0).formParam("dailyrainin", 0.0)
            .formParam("weeklyrainin", 0.0).formParam("monthlyrainin", 0.0)
            .formParam("yearlyrainin", 0.0).formParam("totalrainin", 0.0)
            .formParam("wh65batt", 0).formParam("interval", 60)
        .when().post("/weather/current").then().statusCode(204);

        // Later reading: tempf=68°F → 20°C
        postWeatherReading(later);

        // getCurrent should return the later record (20°C)
        given()
            .when().get("/weather/current")
            .then()
            .statusCode(200)
            .body("tempout", closeTo(20.0, 0.05));
    }
}
