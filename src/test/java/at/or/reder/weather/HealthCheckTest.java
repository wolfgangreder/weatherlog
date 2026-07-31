package at.or.reder.weather;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(H2TestProfile.class)
public class HealthCheckTest {

    @Test
    public void testLivenessEndpoint() {
        given()
            .when().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
    }

    @Test
    public void testReadinessEndpoint() {
        given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200);
    }

    @Test
    public void testCurrentWeatherReturns200OrNoContent() {
        // GET /weather/current — no data in test DB → 204 No Content (null return)
        given()
            .when().get("/weather/current")
            .then()
            .statusCode(anyOf(is(200), is(204)));
    }
}
