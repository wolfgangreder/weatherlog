# weatherlog: Kubernetes / Helm deployment design

Date: 2026-09-02

## Context

`helm/weatherlog/` already exists (a full Helm chart with Chart.yaml, values.yaml,
and templates for configmap/deployment/ingress/secret/service/serviceaccount/_helpers).
It has drifted from the app's actual runtime configuration:

- `values.yaml` models Firebird connection as separate `host`/`port`/`database`
  fields, mapped to env vars `FIREBIRD_HOST`/`FIREBIRD_PORT`/`FIREBIRD_DATABASE`/`FIREBIRD_USER`/`FIREBIRD_PASS`.
- The app's `src/main/resources/application.properties` supports this via property
  expansion (`quarkus.datasource.weather.jdbc.url=jdbc:firebirdsql://${firebird.host}:${firebird.port}/${firebird.database}...`),
  so the existing chart is not technically broken.
- However, the actual current runtime config — root `docker-compose.yml` and the
  production stack `~/Source/postbotix/docker-compose.yml` (used as the reference
  for "current runtime configuration" since `~/Source/postix_env` does not exist) —
  sets the datasource directly via `QUARKUS_DATASOURCE_WEATHER_JDBC_URL`,
  `QUARKUS_DATASOURCE_WEATHER_USERNAME`, `QUARKUS_DATASOURCE_WEATHER_PASSWORD`,
  bypassing the host/port/database indirection entirely.
- `image.repository` in values.yaml is `weatherlog` (bare), not matching the real
  image `wolfgangreder/at.or.reder.weatherlog` used everywhere else (docker-compose,
  `application.properties` `quarkus.container-image.*` settings, `docker-push.sh`).
- Chart/image tag (`0.1.0-SNAPSHOT`) is stale vs. current `gradle.properties`
  (`projectVersion=0.2.0-SNAPSHOT`).
- `ingress.hosts[0].host` is a placeholder (`weatherlog.local`), not the real
  target hostname.

Separately, weatherlog has no `kubernetes/` directory with plain-kubectl example
manifests, unlike the sibling project `~/Source/firebirdsql-monitoring`, which has
`kubernetes/{README.md,configmap.yaml,secret.yaml,deployment.yaml,helm-values-example.yaml}`
alongside its Helm chart. That project's git history (commits `2a0f83b`, `a423dd2`,
`4a62a98`) shows the example files were deliberately scrubbed of real hostnames/
credentials and use `<replace-with-...>` placeholders throughout, with only the
Helm-values example carrying slightly more "fill this in" guidance text.

## Goal

1. Correct `helm/weatherlog/` so it reflects the actual env vars the app reads in
   production (`QUARKUS_DATASOURCE_WEATHER_JDBC_URL/USERNAME/PASSWORD`), the real
   image repository, current version tag, and the real ingress hostname
   `weather.local.reder.or.at`.
2. Add a `kubernetes/` directory to the weatherlog project with the same file set
   and conventions as `firebirdsql-monitoring/kubernetes/`, using placeholders for
   all secrets/hostnames except the ingress hostname (which is not sensitive).
3. Add a short doc pointer so the deployment docs are discoverable.

## Design

### 1. Helm chart fixes (`helm/weatherlog/`)

**`Chart.yaml`**
- `appVersion`: `"0.1.0-SNAPSHOT"` → `"0.2.0-SNAPSHOT"` (matches `gradle.properties`,
  mirrors firebirdsql-monitoring's convention of appVersion == image tag)

**`values.yaml`**
- `image.repository`: `weatherlog` → `wolfgangreder/at.or.reder.weatherlog`
- `image.tag`: `"0.1.0-SNAPSHOT"` → `"0.2.0-SNAPSHOT"`
- `ingress.hosts[0].host`: `weatherlog.local` → `weather.local.reder.or.at`
- `firebird` block replaced:
  ```yaml
  firebird:
    jdbcUrl: "jdbc:firebirdsql://<replace-with-host>:<replace-with-port>//<replace-with-db-path>?charSet=UTF-8"
    user: "sysdba"
    # password: set via --set firebird.password=... or external Secret
    password: "CHANGEME"
  ```
  (drops `host`/`port`/`database` fields entirely)

**`templates/configmap.yaml`**
- Keys become:
  ```yaml
  data:
    QUARKUS_DATASOURCE_WEATHER_JDBC_URL: {{ .Values.firebird.jdbcUrl | quote }}
    QUARKUS_DATASOURCE_WEATHER_USERNAME: {{ .Values.firebird.user | quote }}
  ```
  (was `FIREBIRD_HOST`/`FIREBIRD_PORT`/`FIREBIRD_DATABASE`/`FIREBIRD_USER`)

**`templates/secret.yaml`**
- Key becomes `QUARKUS_DATASOURCE_WEATHER_PASSWORD` (was `FIREBIRD_PASS`)

**`templates/deployment.yaml`**
- `env` block updated to reference the new configmap/secret keys:
  ```yaml
  env:
    - name: QUARKUS_DATASOURCE_WEATHER_JDBC_URL
      valueFrom:
        configMapKeyRef:
          name: {{ include "weatherlog.fullname" . }}
          key: QUARKUS_DATASOURCE_WEATHER_JDBC_URL
    - name: QUARKUS_DATASOURCE_WEATHER_USERNAME
      valueFrom:
        configMapKeyRef:
          name: {{ include "weatherlog.fullname" . }}
          key: QUARKUS_DATASOURCE_WEATHER_USERNAME
    - name: QUARKUS_DATASOURCE_WEATHER_PASSWORD
      valueFrom:
        secretKeyRef:
          name: {{ include "weatherlog.fullname" . }}
          key: QUARKUS_DATASOURCE_WEATHER_PASSWORD
  ```

**No changes**: `templates/ingress.yaml`, `templates/service.yaml`,
`templates/serviceaccount.yaml`, `templates/_helpers.tpl` — already generic and
correct. `prometheus.path` (`/q/metrics`) and health probe paths
(`/q/health/live`, `/q/health/ready`) in `values.yaml` are already correct
(confirmed against `application.properties` and the `quarkus-smallrye-health`
dependency in `build.gradle`) — no changes needed there.

### 2. New `kubernetes/` directory (plain kubectl examples)

Mirrors `firebirdsql-monitoring/kubernetes/` file-for-file:

**`kubernetes/configmap.yaml`**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: weatherlog
  namespace: <replace-with-namespace>
data:
  QUARKUS_DATASOURCE_WEATHER_JDBC_URL: "jdbc:firebirdsql://<replace-with-host>:<replace-with-port>//<replace-with-db-path>?charSet=UTF-8"
  QUARKUS_DATASOURCE_WEATHER_USERNAME: "<replace-with-db-user>"
```

**`kubernetes/secret.yaml`**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: weatherlog
  namespace: <replace-with-namespace>
type: Opaque
data:
  # echo -n "yourpassword" | base64
  QUARKUS_DATASOURCE_WEATHER_PASSWORD: <replace-with-base64-encoded-password>
```

**`kubernetes/deployment.yaml`** — Deployment + Service in one file (same layout
as the sibling project):
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: weatherlog
  namespace: <replace-with-namespace>
spec:
  replicas: 1
  selector:
    matchLabels:
      app: weatherlog
  template:
    metadata:
      labels:
        app: weatherlog
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: /q/metrics
        prometheus.io/port: "8080"
    spec:
      containers:
        - name: weatherlog
          image: wolfgangreder/at.or.reder.weatherlog:<replace-with-tag>
          ports:
            - name: http
              containerPort: 8080
          envFrom:
            - configMapRef:
                name: weatherlog
            - secretRef:
                name: weatherlog
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /q/health/ready
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 3
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: weatherlog
  namespace: <replace-with-namespace>
spec:
  selector:
    app: weatherlog
  type: ClusterIP
  ports:
    - port: 8080
      targetPort: http
      name: http
```

**`kubernetes/helm-values-example.yaml`**
```yaml
# Example Helm values override
# Copy this file, fill in your values, then:
#   helm upgrade --install weatherlog helm/weatherlog \
#     -f my-values.yaml --namespace <your-namespace> --create-namespace

image:
  tag: "<replace-with-tag>"   # see https://hub.docker.com/r/wolfgangreder/at.or.reder.weatherlog/tags

firebird:
  jdbcUrl: "jdbc:firebirdsql://<replace-with-host>:<replace-with-port>//<replace-with-db-path>?charSet=UTF-8"
  user: "<replace-with-db-user>"
  password: "<replace-with-password>"    # use --set firebird.password=... to avoid committing secrets

ingress:
  enabled: true
  className: "<replace-with-ingressclass>"   # e.g. nginx, traefik
  hosts:
    - host: "weather.local.reder.or.at"
      paths:
        - path: /
          pathType: Prefix
```
(Ingress hostname is filled in with the real target since it is not sensitive;
everything else stays a placeholder, matching firebirdsql-monitoring's
placeholder-only convention for secrets/connection details.)

**`kubernetes/README.md`** — same structure as the sibling project's:
plain-kubectl section (apply order: secret → configmap → deployment, base64
encoding instructions for the password), Helm section (copy example values,
`helm upgrade --install`/`helm uninstall` commands), and an "Available values"
table referencing `helm/weatherlog/values.yaml`.

### 3. Doc pointer

`weatherlog/AGENTS.md` has no existing Kubernetes/Helm section (it predates this
work and is otherwise stale re: Liberty/Jakarta EE — that content is **not**
touched by this change, out of scope). Append a new section at the end:

```markdown
## Kubernetes / Helm

See [`kubernetes/README.md`](kubernetes/README.md) — includes plain kubectl
manifests, Helm chart, and example config files.
```

## Amendment (2026-09-02, same day)

Follow-up request: automatix_env also needed a per-app manifest for weatherlog
that reuses the *same* `firebird-config`/`firebird-secret` Kubernetes resources
already used by `firebirdsql-monitoring` in that cluster (DRY credentials,
single source of truth for the DB connection), rather than weatherlog owning
its own duplicate ConfigMap/Secret with independently-set values.

`firebirdsql-monitoring`'s chart had already evolved (chart `version` 0.1.0 →
0.2.0 in `automatix_env/helm/firebirdsql-monitoring`) to stop creating its own
ConfigMap/Secret and instead reference pre-existing ones via
`externalSecret.name` / `externalConfig.name` values. This amendment applies
the identical pattern to weatherlog's chart, as a breaking change (chart
`version` 0.1.0 → 0.2.0, `appVersion` unchanged):

- Removed `templates/configmap.yaml` and `templates/secret.yaml` — chart no
  longer owns these resources.
- `values.yaml`: replaced `firebird.{jdbcUrl,user,password}` with
  `externalSecret.name` (default `firebird-secret`) and `externalConfig.name`
  (default `firebird-config`).
- `templates/deployment.yaml`: env vars now reference the external
  ConfigMap/Secret directly (`database_url` key for the JDBC URL, `username`/
  `password` keys for credentials — matching the key names already used by
  `firebird-config`/`firebird-secret` in automatix_env).
- `kubernetes/{configmap.yaml,secret.yaml,deployment.yaml,helm-values-example.yaml,README.md}`
  updated to match: the standalone examples now also use the `firebird-config`/
  `firebird-secret` resource names (not `weatherlog`-prefixed), so they double
  as drop-in examples compatible with the same convention used elsewhere in
  this deployment ecosystem.

Separately, in `automatix_env`: vendor the updated chart to `helm/weatherlog`
(mirroring `helm/firebirdsql-monitoring`), add `weatherlog-config.yaml` (values
override: `externalSecret.name: firebird-secret`, `externalConfig.name:
firebird-config`, `ingress.hosts[0].host: weather.local.reder.or.at`), and
`helm template`-render the result to `weatherlog.yaml`, following the exact
same generate-and-commit workflow already documented in `automatix_env`'s root
`README.md` for `firebird-monitoring.yaml`.

## Out of scope

- Rewriting the stale Liberty/Jakarta EE content in `AGENTS.md` (pre-existing
  issue, unrelated to this change).
- Fixing `~/Source/automatix_env/firebird-service.yaml` (found empty/0 bytes
  during research) or any other issue in the automatix_env or postbotix repos.
- Any change to `docker-compose.yml` or the Docker Compose stack — this task is
  Kubernetes/Helm only.
- TLS configuration for the ingress (left as the existing `tls: []` default;
  not specified by the request).
