# Design: weatherlog service in ~/Docker/docker-compose.yml

Date: 2026-07-29

## Goal

Add a `weatherlog` service to the shared `/home/wolfi/Docker/docker-compose.yml` stack,
connecting to the Firebird database on `automatix.local.reder.or.at` with credentials
stored in Docker external secret files.

## Service

- **Image:** `wolfgangreder/at.or.reder.weatherlog:0.0.4`
- **Container name:** `weatherlog`
- **Port:** `9080:9080`
- **DB URL** (non-secret, set via environment): `jdbc:firebirdsql://automatix.local.reder.or.at:3050//var/lib/firebird/data/frodo.firebird`
- **Restart policy:** `no`

## Secret handling

Liberty reads all XML files in `/config/configDropins/overrides/` at startup and merges
them into server configuration. Each secret file is a minimal Liberty XML snippet that
declares one variable, mounted via Docker Compose `secrets:`.

| Secret name       | Host file                                          | Container target                                          | Variable set        |
|-------------------|----------------------------------------------------|-----------------------------------------------------------|---------------------|
| `firebird_user`   | `/home/wolfi/Docker/secrets/firebird-user.xml`     | `/config/configDropins/overrides/firebird-user.xml`       | `firebird.user`     |
| `firebird_pass`   | `/home/wolfi/Docker/secrets/firebird-pass.xml`     | `/config/configDropins/overrides/firebird-pass.xml`       | `firebird.pass`     |

Both files owned `1001:0`, mode `0440` — matches Liberty container user.

### Secret file format

```xml
<server><variable name="firebird.user" value="sysdba"/></server>
```

```xml
<server><variable name="firebird.pass" value="CHANGE_ME"/></server>
```

The host files are not committed to version control.

## Changes

1. Create `/home/wolfi/Docker/secrets/` directory.
2. Create template secret files (placeholder values — operator fills in real password).
3. Append `secrets:` top-level block and `weatherlog` service to `~/Docker/docker-compose.yml`.
