# AGENTS.md

## Project overview

Jakarta EE 10 / MicroProfile 6.1 web application (WAR), running on Open Liberty. Firebird SQL database. Java 21. Build tool: Maven (wrapper included).

## Key commands

```bash
./mvnw package                # compile + build WAR (target/weatherlog.war)
./mvnw liberty:run            # start Liberty server (blocking)
./mvnw liberty:dev            # dev mode with hot reload (blocking)
./mvnw liberty:stop           # stop a running Liberty server
./mvnw test                   # unit tests (Surefire)
./mvnw verify                 # unit + integration tests (Failsafe); Liberty must be running
```

## Pre-commit hook

A git pre-commit hook runs `trivy --exit-code 1 fs ./` — commits are blocked if Trivy finds vulnerabilities. Trivy must be installed locally.

## Runtime configuration

- Liberty reads `src/main/liberty/config/server.env` for environment overrides (referenced in `pom.xml` via `<serverEnvFile>`). This file is **not** committed; create it locally to override defaults.
- Default Firebird connection: host `192.168.1.19`, port `3051`, database `weather`, user `sysdba`.
- Variables can be overridden via `server.env` or Docker environment variables (`firebird.host`, `firebird.pass`, etc.).

## JDBC driver placement

Jaybird (Firebird JDBC) is **not** `provided` scope — it is bundled in the WAR and also copied by the Liberty Maven plugin into `target/liberty/wlp/usr/shared/resources/` at build time. Do not change its scope to `provided` without updating the Liberty datasource config.

## Docker

Build the application image:
```bash
./mvnw package
docker build -t weatherlog:<version> .
```
The root `Dockerfile` uses `icr.io/appcafe/open-liberty:kernel-slim-java21-openj9-ubi-minimal`.

Start the full stack (app + Firebird + Prometheus + Grafana):
```bash
docker compose -f compose/docker-compose.yml up -d
```
Note: the image tag in `compose/docker-compose.yml` (`weatherlog:0.0.2-SNAPSHOT`) may be stale relative to the current `pom.xml` version; update it when building a new image.

The `compose/Dockerfile` is a leftover from an older project (`simpleweather`) — it is not used for this application.

## Monitoring

MicroProfile Metrics are exposed at `/metrics` with authentication disabled (`<mpMetrics authentication="false"/>`). Prometheus scrapes `wolfi-weatherlog-1:9080/metrics` per `compose/prometheus.yml`. Grafana is available at port `3000` (default credentials `admin/admin`).

## Database migrations

Liquibase (`liquibase-core`) is on the classpath. Migration changelogs live under `src/main/resources/`.

## Integration tests

Failsafe (integration tests) reads `${liberty.var.http.port}` as the HTTP port. Run `./mvnw verify` with Liberty already started, or configure the failsafe plugin lifecycle to start/stop Liberty automatically.

## Source layout

- `src/main/java/at/or/reder/weather/` — all application Java sources
- `src/main/liberty/config/server.xml` — Liberty feature list, datasources, ports
- `src/main/resources/META-INF/microprofile-config.properties` — MicroProfile config
- `src/main/resources/META-INF/persistence.xml` — JPA persistence unit
- `compose/` — Docker Compose stack and Prometheus config
