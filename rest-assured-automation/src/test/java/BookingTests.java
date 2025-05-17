import Entities.Booking;
import Entities.BookingDates;
import Entities.User;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;

public class BookingTests {
    public static Faker faker;
    private static RequestSpecification request;
    private static Booking booking;
    private static BookingDates bookingDates;
    private static User user;

    @BeforeAll
    public static void Setup(){
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        faker = new Faker();
        user = user = new User(
                faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName()
        );


        bookingDates = new BookingDates("2018-01-02", "2018-01-03");
        booking = new Booking(user.getFirstName(), user.getLastName(),
                (float)faker.number().randomDouble(2, 50, 100000),
                true,bookingDates,
                "");
        RestAssured.filters(new RequestLoggingFilter(),new ResponseLoggingFilter(), new ErrorLoggingFilter());
    }

    @BeforeEach
    void setRequest(){
        request = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .auth().basic("admin", "password123");
    }

    @Test
    public void pingServer_returnOk() {
        given()
                .when()
                .get("/ping")
                .then()
                .statusCode(201);
    }


    @Test
    public void getAllBookingsById_returnOk(){
        Response response = request
                .when()
                .get("/booking")
                .then()
                .extract()
                .response();


        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void  getAllBookingsByUserFirstName_BookingExists_returnOk(){
        request
                .when()
                .queryParam("firstName", "Carol")
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0)));

    }

    @Test
    public void  CreateBooking_WithValidData_returnOk(){
        Booking test = booking;
        given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .when()
                .body(booking)
                .post("/booking")
                .then()
                .body(matchesJsonSchemaInClasspath("createBookRequestSchema.json"))
                .and()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON).and().time(lessThan(2000L));
    }

    @Test
    public void updateBooking_returnOk() {
        Booking newBooking = new Booking("John", "Doe", 100.0f, true, new BookingDates("2022-01-01", "2022-01-02"), "Breakfast");

        int bookingId = given()
                .contentType(ContentType.JSON)
                .body(newBooking)
                .when()
                .post("/booking")
                .then()
                .statusCode(200)
                .extract()
                .path("bookingid");

        String token = given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"admin\", \"password\": \"password123\"}")
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .extract()
                .path("token");

        newBooking.setTotalprice(160.0f);

        given()
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .body(newBooking)
                .when()
                .put("/booking/" + bookingId)
                .then()
                .statusCode(200)
                .body("totalprice", equalTo(160));
    }


    @Test
    public void deleteBooking_returnOk() {
        Booking newBooking = new Booking("Jane", "Doe", 200.0f, true,
                new BookingDates("2022-02-01", "2022-02-02"), "Lunch");

        int bookingId = given()
                .contentType(ContentType.JSON)
                .body(newBooking)
                .when()
                .post("/booking")
                .then()
                .statusCode(200)
                .extract()
                .path("bookingid");

        String token = given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"admin\", \"password\": \"password123\"}")
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .extract()
                .path("token");

        given()
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .when()
                .delete("/booking/" + bookingId)
                .then()
                .statusCode(anyOf(equalTo(201), equalTo(200)));
    }
}