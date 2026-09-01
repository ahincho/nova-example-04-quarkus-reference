# nova-java-quarkus-example

Instancia Quarkus 3.37 del meta-framework **Nova Platform**.

Paralelo Quarkus de [`instances/nova-java-example/`](../../nova-java-example/) (que es la instancia Spring Boot). Ambas son apps reales que consumen las librerias puras + extensions de Nova.

Sirve como **integration test vivo** del extension `nova-quarkus-api-ext` publicado en [`ahincho/nova-java-api-standard-quarkus-extension`](https://github.com/ahincho/nova-java-api-standard-quarkus-extension).

Este proyecto **valida la Fase 0** del documento [`docs/java/07-quarkus-analisis-adopcion.md`](../../../docs/java/07-quarkus-analisis-adopcion.md).

## Que hace

Endpoints JAX-RS que retornan `ApiResponse<T>` (contrato framework-agnostic de `nova-api-standard`):

| Endpoint | Status | Descripcion |
|---|---|---|
| `GET /hello` | 200 | Retorna `ApiResponse<Greeting>` con `generatedAt` ISO-8601 |
| `GET /hello/{name}` | 200 | Personalizado, valida `name` no blank |
| `GET /hello/{blank}` | 400 | `IllegalArgumentException` → `ApiError(code=BAD_REQUEST)` |
| `GET /hello/error` | 400 | Misma excepcion pero lanzada explícitamente (test del mapper) |

Sin el extension Nova, las excepciones no controladas darian un JSON default de Quarkus (con stack trace, sin `ApiResponse` envelope) y los `Instant` en `ApiMetadata` se serializarian como epoch ms en vez de ISO-8601.

## Stack

| Pieza | Version |
|---|---|
| Quarkus | 3.33.2.1 LTS |
| Java | 25 |
| Gradle | 9.5.1 |
| `nova-quarkus-api-ext` | 1.0.1 |
| `nova-api-standard` (transitiva) | 1.0.0 |

## Running the application in dev mode

```shell script
./gradlew quarkusDev
```

> Quarkus ships with a Dev UI available in dev mode at <http://localhost:8080/q/dev/>.

## Packaging and running

```shell script
./gradlew build
```

Produces `quarkus-app/quarkus-run.jar` in `build/quarkus-app/`. Run with:

```shell
java -jar build/quarkus-app/quarkus-run.jar
```

For uber-jar:

```shell
./gradlew build -Dquarkus.package.jar.type=uber-jar
java -jar build/*-runner.jar
```

## Native executable

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

## Running tests

```shell script
./gradlew test
```

Tests usan `@QuarkusTest` y RestAssured. Validan:

1. **Happy path**: `GET /hello` retorna `ApiResponse<Greeting>` con `success=true`.
2. **Exception mapping**: `GET /hello/error` (que lanza `IllegalArgumentException`) retorna 400 con `ApiError(code=BAD_REQUEST)`.
3. **Subclass mapping**: `NumberFormatException` (subclase de `IllegalArgumentException`) tambien mapea a 400.
4. **Path validation**: `name` blank o null retorna 400.
5. **Timestamp serialization**: `Instant` en `ApiMetadata.generatedAt` se serializa como ISO-8601, no como epoch ms (esto valida que `ApiObjectMapperCustomizer` del extension funciona).

## CI/CD

Workflows en `.github/workflows/`:

- `ci.yml` — pull_request + push: ejecuta build, matrix build (Java 21 + 25), OWASP, SBOM, SonarCloud, y el **Quarkus IT job** (integration test end-to-end).

El Quarkus IT job es el que valida Fase 0 de doc 07. Ver [`.github/SECRETS_SETUP.md`](./SECRETS_SETUP.md) para configurar `NOVA_PACKAGES_READ_TOKEN`.

## Documentacion relacionada

- [`docs/java/07-quarkus-analisis-adopcion.md`](../../../docs/java/07-quarkus-analisis-adopcion.md) — analisis macro de adopcion Quarkus (seccion 7 define Fase 0).
- [`docs/java/06-semantic-versioning-en-java.md`](../../../docs/java/06-semantic-versioning-en-java.md) — semver, release-please, CI/CD patterns.
- [Extension repo](https://github.com/ahincho/nova-java-api-standard-quarkus-extension) — codigo fuente del extension que esta instancia consume.
- [`instances/nova-java-example/`](../../nova-java-example/) — instancia gemela Spring Boot (mismo patron, distinto framework).

## License

Eclipse Public License 2.0 — see [LICENSE](LICENSE).

Copyright © 2026 Angel Hincho.
