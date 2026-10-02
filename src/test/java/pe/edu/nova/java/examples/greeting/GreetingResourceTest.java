package pe.edu.nova.java.examples.greeting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests de integración del {@link GreetingResource} consumiendo
 * {@code nova-api-standard-quarkus-extension}.
 * <p>
 * Estos tests validan que la extensión funciona en una app Quarkus real: su
 * módulo de deployment registra sus beans al construir la aplicación, sin
 * {@code quarkus.index-dependency}, y los errores salen en el sobre de Nova con
 * su {@code traceId}. El contrato de la extensión lo prueba su propio módulo de
 * deployment; este ejemplo comprueba que una app que la consume responde igual.
 */
@QuarkusTest
class GreetingResourceTest {

    @Test
    void helloReturnsSuccessApiResponseWithGreetingPayload() {
        given()
            .when().get("/hello")
            .then()
                .statusCode(200)
                .contentType("application/json")
                .body("success", is(true))
                .body("status", is(200))
                .body("data.message", equalTo("Hello from Quarkus REST"))
                .body("data.generatedAt", notNullValue())
                .body("errors.size()", is(0));
    }

    @Test
    void errorEndpointAnswersInvalidInputAsBadRequestEnvelope() {
        given()
            .when().get("/hello/error")
            .then()
                .statusCode(400)
                .contentType("application/json")
                .body("success", is(false))
                .body("status", is(400))
                .body("data", nullValue())
                .body("errors.size()", is(1))
                .body("errors[0].code", equalTo("BAD_REQUEST"))
                .body("errors[0].message", equalTo("El nombre no puede estar vacío"))
                .body("errors[0].field", equalTo("name"))
                .body("metadata.traceId", notNullValue())
                .body("metadata.timestamp", notNullValue());
    }

    @Test
    void pathParamEndpointReturnsPersonalizedGreeting() {
        given()
            .when().get("/hello/World")
            .then()
                .statusCode(200)
                .body("success", is(true))
                .body("status", is(200))
                .body("data.message", equalTo("Hello, World!"));
    }

    @Test
    void blankPathParamMapsToBadRequest() {
        // Un PathParam en blanco (por ejemplo "   ") se valida en el recurso y lanza
        // ApplicationError.invalidInput, que la extensión responde como 400 BAD_REQUEST.
        // NOTA: JAX-RS no URL-decodea los path params automáticamente en Quarkus REST
        // (resteasy-reactive), así que pasamos el valor como path param tipado (no como
        // string en la URL) para evitar el re-encoding de RestAssured.
        given()
            .pathParam("name", "   ")
            .when().get("/hello/{name}")
            .then()
                .statusCode(400)
                .body("success", is(false))
                .body("status", is(400))
                .body("errors[0].code", equalTo("BAD_REQUEST"))
                .body("errors[0].message", equalTo("El nombre no puede estar en blanco"))
                .body("errors[0].field", equalTo("name"))
                .body("metadata.traceId", notNullValue());
    }
}