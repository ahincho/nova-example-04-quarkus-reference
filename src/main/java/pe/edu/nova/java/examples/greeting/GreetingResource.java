package pe.edu.nova.java.examples.greeting;

import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.java.libs.api.standard.error.FieldError;
import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Recurso JAX-RS de ejemplo que demuestra la integración con
 * {@code nova-api-standard-quarkus-extension}.
 * <p>
 * Los métodos retornan {@link ApiResponse}{@code <Greeting>} (contrato
 * framework-agnostic de nova-api-standard) armado a mano, y la extensión no
 * lo envuelve de nuevo. La extensión se encarga de:
 * <ul>
 *   <li>Responder cada error con el sobre de Nova según su capa (ADR-031):
 *       el recurso lanza un {@link ApplicationError} y la extensión decide el
 *       status, el código y el {@code traceId}.</li>
 *   <li>Configurar el ObjectMapper para serializar {@link java.time.Instant}
 *       como ISO-8601 (vía {@code ApiObjectMapperCustomizer}).</li>
 * </ul>
 * El consumidor NO escribe código de manejo de errores: si {@link #greet}
 * lanza {@code ApplicationError.invalidInput(...)}, el cliente recibe 400 + JSON
 * con la forma {@code ApiResponse} con {@code errors[].code = "BAD_REQUEST"}.
 * Una excepción inesperada, como una {@link IllegalArgumentException}, ya no
 * es un 400: sale como un 500 con el mensaje genérico del catálogo.
 */
@Path("/hello")
@Produces(MediaType.APPLICATION_JSON)
public class GreetingResource {

    @GET
    public ApiResponse<Greeting> hello() {
        return ApiResponse.ok(Greeting.of("Hello from Quarkus REST"));
    }

    /**
     * Endpoint que lanza un error de entrada inválida para demostrar que la
     * extensión lo intercepta y lo serializa como ApiResponse JSON (400
     * BAD_REQUEST), con el campo que falló y un {@code traceId} en
     * {@code metadata}.
     */
    @GET
    @Path("/error")
    public ApiResponse<Greeting> error() {
        throw ApplicationError.invalidInput("La solicitud tiene campos inválidos",
                List.of(FieldError.of("name", "El nombre no puede estar vacío")));
    }

    /**
     * Endpoint con path param que valida el input. Un nombre en blanco lanza
     * {@code ApplicationError.invalidInput(...)}, que la extensión responde
     * consistentemente como 400.
     */
    @GET
    @Path("/{name}")
    public ApiResponse<Greeting> greet(@PathParam("name") String name) {
        if (name == null || name.isBlank()) {
            throw ApplicationError.invalidInput("La solicitud tiene campos inválidos",
                    List.of(FieldError.of("name", "El nombre no puede estar en blanco")));
        }
        return ApiResponse.ok(Greeting.of("Hello, " + name + "!"));
    }
}