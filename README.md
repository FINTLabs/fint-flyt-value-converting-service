# FINT Flyt Value Converting Service

Spring Boot (Web + Data JPA) service that stores value-conversion maps between FINT Flyt applications, exposes them over an internal HTTP API, and serves Kafka request/reply lookups so downstream processes can resolve conversion rules on demand.

## Highlights

- **HTTP API** - Spring MVC controller for listing, fetching, and persisting conversion maps with pageable queries and optional payload trimming.
- **PostgreSQL-backed store** - `ValueConversion` entities plus `converting_map` rows maintained through Flyway migrations.
- **Kafka request/reply integration** — Request listener returns conversion details by ID on a short-lived topic, enabling orchestration services to avoid direct database access.
- **Fine-grained authorization** — OAuth2 resource server user-permissions consumer to filter results by the caller’s allowed source-application IDs.
- **Operational guardrails** — Request validation through Kotlin nullability, standardized `ProblemDetail` error responses, standard Actuator endpoints, and Prometheus metrics out of the box.

## Architecture Overview

| Component                                     | Responsibility                                                                                               |
|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `ValueConversionController`                   | Handles `/api/intern/value-convertings` requests, enforces paging, validation, and optional authz filtering. |
| `ValueConversionService`                      | Mediates repository access, maps entities ↔ DTOs, and applies `excludeConvertingMap` semantics.              |
| `ValueConversionMapper`                       | Trims map keys/values and converts between `ValueConversion` JPA entities and API DTOs.                      |
| `ValueConversionRepository`                   | Spring Data JPA repository, including `findAllByFromApplicationIdIn(...)` for user-scoped listings.          |
| `ValueConversionRequestConsumerConfiguration` | Creates the Kafka request listener/container, provisions the topic, and replies with repository lookups.     |
| `GlobalExceptionHandler`                      | Maps application exceptions to HTTP `ProblemDetail` responses (`400`/`404`/`422`/`500`).                     |

## HTTP API

Base path: `/api/intern/value-convertings`

| Method | Path                   | Description                                                                              | Request body                                 | Response                                      |
|--------|------------------------|------------------------------------------------------------------------------------------|----------------------------------------------|-----------------------------------------------|
| `GET`  | `/`                    | Paginated list of conversions; supports sorting and omitting the converting map payload. | –                                            | `200 OK` with `Page<ValueConversionResponse>` |
| `GET`  | `/{valueConversionId}` | Fetch a single conversion including its map unless trimmed by the caller.                | –                                            | `200 OK`, `404` if missing                    |
| `POST` | `/`                    | Create a new conversion; validation errors return `422 ProblemDetail`.                   | JSON `ValueConversionRequest` (map required) | `200 OK` with `ValueConversionResponse`       |

Query parameters for `GET /`:
`page` (0-based), `size`, `sortProperty`, `sortDirection` (`ASC|DESC`), and optional `excludeConvertingMap=true` to skip the map for list views.

`ValueConversionRequest` payload structure:

```json
{
    "displayName": "FS -> ERP",
    "fromApplicationId": 1001,
    "fromTypeId": "FS.Student",
    "toApplicationId": "erp",
    "toTypeId": "ERP.Student",
    "convertingMap": {
      "FS:STATE:ACTIVE": "ERP:STATUS:ENABLED"
    }
}
```

Required fields: `displayName`, `fromApplicationId` (`Long`), `fromTypeId`, `toApplicationId`, `toTypeId`, and `convertingMap` (`map<string,string>`).

Unknown fields in `ValueConversionRequest` are ignored during deserialization.

Errors use standard Spring semantics:
- `400` for invalid query parameters (for example `size < 1` or `size > 1000`)
- `401/403` for auth issues
- `404` for unknown IDs
- `422` for request-body validation failures (including duplicate `convertingMap` keys after trimming)
- `500` for internal errors (including unhandled exceptions, returned with generic detail)

All of these are returned as `application/problem+json` from `GlobalExceptionHandler`.

Example error response:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation error: 'size' must be greater than or equal to 1"
}
```

## Kafka Integration

- ValueConversionRequestConsumerConfiguration.valueConversionByIdRequestConsumer provisions a request topic named `value-converting` (parameterized by `value-converting-id`) with a 10-minute retention window.
- Backed by RequestTopicService/RequestListenerContainerFactory from no.novari.kafka; replies carry a `ValueConversion` entity or `null` when missing.
- Consumer uses the shared error-handler factory: failures are not retried and skipped records are logged.
- Topic naming inherits org/application prefixes from TopicNamePrefixParameters, aligning with other Flyt services.

## Scheduled Tasks

The service has no cron-style jobs; state is managed via HTTP and Kafka interactions. Schema evolution is handled by Flyway migrations (db/migration/V1__init.sql) during startup.

## Configuration

Spring profiles layered via application.yaml: flyt-kafka, flyt-web-resource-server, flyt-postgres, and flyt-logging.

| Property                                                | Description                                                                            |
|---------------------------------------------------------|----------------------------------------------------------------------------------------|
| fint.application-id                                     | Service ID; reused for Kafka consumer group and topic naming.                          |
| fint.database.{url,username,password}                   | PostgreSQL connection supplied as secrets per environment.                             |
| spring.datasource.hikari.schema                         | Schema namespace (per overlay) used for both JDBC and Flyway.                          |
| novari.kafka.topic.{org-id,domain-context}              | Kafka naming segments; overlays override org-id to isolate ACLs.                       |
| novari.flyt.web-resource-server.security.api.internal.* | Restricts internal endpoints to approved org/role pairs (populated via overlays).      |
| spring.security.oauth2.resourceserver.jwt.issuer-uri    | IdP issuer for JWT validation (default https://idp.felleskomponent.no/nidp/oauth/nam). |
| spring.kafka.bootstrap-servers                          | Kafka bootstrap list; local profile defaults to localhost:9092.                        |
| server.port                                             | Defaults to 8094 under the local-staging profile.                                      |

## Running Locally

Prerequisites: Java 25+, Gradle wrapper, Docker (for PostgreSQL), and a Kafka broker (e.g., local kafka or Redpanda). You can start the database with the bundled helper:

```shell
./start-postgres                # launches postgres:latest on port 5440
```

Then run the service with development profiles:

```shell
export SPRING_PROFILES_ACTIVE=local-staging
./gradlew clean build           # compile + test
./gradlew bootRun               # starts on http://localhost:8094
# in another terminal, run tests as needed
./gradlew test
```

Verify the API:

```shell
curl -H "Authorization: Bearer <token>" \
"http://localhost:8094/api/intern/value-convertings?page=0&size=20&sortProperty=id&sortDirection=ASC"
```

Local profile defaults:

- PostgreSQL: jdbc:postgresql://localhost:5440/fint-flyt-value-converting-service with username postgres/password.
- Kafka: localhost:9092.
- Internal security: authorized-org-id-role-pairs-json grants DEVELOPER for vigo.no to ease testing.

## Deployment

Kustomize layout:

- kustomize/base/ — common FLAIS Application manifest (flais.yaml) plus shared config.
- kustomize/overlays/<org>/<env>/ — namespace-specific overlays that set org IDs, ingress base paths, Kafka topics, and security JSON.
- kustomize/templates/overlay*.yaml.tpl — templated overlay sources.
- script/render-overlay.sh — regenerates every kustomize/overlays/**/kustomization.yaml using envsubst.

After editing templates or per-org metadata:

```shell
./script/render-overlay.sh
```

Commit both the template changes and the regenerated overlay files.

## Security

- OAuth2 resource server validates JWTs from https://idp.felleskomponent.no.
- Internal endpoints (`/api/intern/value-convertings/**`) are protected by the shared flyt-web-resource-server filter set; overlays supply org/role mappings.
- UserAuthorizationService limits reads/writes to the caller’s authorized fromApplicationId set.

## Observability & Operations

- Actuator readiness/live at /actuator/health, metrics at /actuator/prometheus.
- Logs follow Spring Boot defaults.
- Kafka request listener metrics surface via Micrometer (tied to Prometheus when configured).

## Development Tips

- ValueConversionMapper trims whitespace on both keys and values. If trimming causes duplicate keys, the request is rejected with `422`.
- Use the excludeConvertingMap flag on GET / when only metadata is needed to avoid transmitting large maps.
- Kafka replies return null for unknown IDs; downstream services must handle empty responses gracefully.

## Contributing

1. Create a topic branch.
2. Run ./gradlew test (and any relevant integration checks) before opening a PR.
3. Update or add tests for new behavior.
4. If you change anything under kustomize/, re-run ./script/render-overlay.sh and commit the resulting manifests.

———

FINT Flyt Value Converting Service is maintained by the FINT Flyt team. Reach out in the internal Slack channel or open an issue if you need enhancements or run into problems.
