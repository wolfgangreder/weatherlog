
FROM icr.io/appcafe/open-liberty:kernel-slim-java21-openj9-ubi-minimal

ARG VERSION=0.0.1
ARG REVISION=SNAPSHOT

LABEL \
  org.opencontainers.image.authors="Wolfgang Reder" \
  org.opencontainers.image.version="$VERSION" \
  org.opencontainers.image.revision="$REVISION" \
  name="weatherlog" \
  version="$VERSION-$REVISION"

EXPOSE 9080
EXPOSE 7777

COPY --chown=1001:0 /src/main/liberty/config /config

RUN features.sh

COPY --chown=1001:0 /target/*.war /config/apps

RUN configure.sh

COPY --chown=1001:0 /target/liberty/wlp/usr/shared/resources /opt/ol/wlp/usr/shared/resources
