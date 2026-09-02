# Kubernetes Deployment

weatherlog does **not** create its own Secret/ConfigMap for the Firebird
connection — it references a pre-existing Secret and ConfigMap in the same
namespace (so it can share credentials/connection info with other apps, e.g.
`firebirdsql-monitoring`, talking to the same database).

## Plain kubectl

```bash
# 1. fill in placeholders in secret.yaml and configmap.yaml
#    (skip if firebird-secret / firebird-config already exist in the namespace,
#    e.g. because firebirdsql-monitoring is already deployed there)
kubectl apply -f kubernetes/secret.yaml
kubectl apply -f kubernetes/configmap.yaml
kubectl apply -f kubernetes/deployment.yaml
```

Metrics endpoint: `http://<service>:8080/q/metrics`
Health: `http://<service>:8080/q/health/live`

### secret.yaml

Encode username/password before inserting:

```bash
echo -n "yourusername" | base64
echo -n "yourpassword" | base64
```

Paste results into `secret.yaml` → `username` / `password`.

### configmap.yaml

| Key | Description |
|---|---|
| `database_url` | Full Jaybird JDBC URL, e.g. `jdbc:firebirdsql://host:3050//path/to/db.firebird?charSet=UTF-8` |

---

## Helm

```bash
# copy and edit the example values
cp kubernetes/helm-values-example.yaml my-values.yaml
# edit my-values.yaml — fill in image tag, externalSecret/externalConfig names

# create the Secret + ConfigMap first (see above), unless they already exist
kubectl apply -f kubernetes/secret.yaml -f kubernetes/configmap.yaml

# install
helm upgrade --install weatherlog helm/weatherlog \
  -f my-values.yaml \
  --namespace weatherlog --create-namespace

# uninstall
helm uninstall weatherlog --namespace weatherlog
```

### Available values

| Key | Default | Description |
|---|---|---|
| `image.repository` | `wolfgangreder/at.or.reder.weatherlog` | Container image |
| `image.tag` | `0.2.0-SNAPSHOT` | Container image tag |
| `externalSecret.name` | `firebird-secret` | Pre-existing Secret with `username`/`password` keys |
| `externalConfig.name` | `firebird-config` | Pre-existing ConfigMap with `database_url` key |
| `ingress.enabled` | `false` | Create Ingress resource |
| `ingress.className` | `""` | IngressClass (e.g. `nginx`, `traefik`) |
| `ingress.hosts[0].host` | `weather.local.reder.or.at` | Ingress hostname |
| `prometheus.scrape` | `true` | Add `prometheus.io/scrape` pod annotation |

Full list: [`helm/weatherlog/values.yaml`](../helm/weatherlog/values.yaml)
