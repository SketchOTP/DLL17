# Reproducible local deployment of the identity authority.
#
# A local deployment artifact, not a production topology. There is no TLS, no
# reverse proxy and no secret management here, and `OPERATIONS.md` requires all
# three in front of the service. A container that looked production-shaped would
# invite being used as one.
#
# Build from the repository root:
#   docker build -f services/identity-authority/operations/Dockerfile -t dll17-authority .

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew :services:identity-authority:installDist --no-daemon

FROM eclipse-temurin:17-jre
# Non-root, and the only writable path is the store volume. The service needs
# nothing else: no temp files, no cache, and no configuration it discovers on
# its own.
RUN useradd --system --uid 10014 --create-home --home-dir /var/lib/dll17 authority \
    && mkdir -p /var/lib/dll17/authority \
    && chown -R authority:authority /var/lib/dll17
COPY --from=build /src/services/identity-authority/build/install/identity-authority /opt/identity-authority
USER authority
VOLUME ["/var/lib/dll17/authority"]

ENV DLL17_AUTHORITY_STORE=/var/lib/dll17/authority
ENV DLL17_AUTHORITY_PORT=8080
# Inside a container, loopback is unreachable from outside it. This is the one
# place binding wider is correct, and it is correct only because the container's
# network is expected to sit behind the proxy `OPERATIONS.md` requires.
ENV DLL17_AUTHORITY_BIND=0.0.0.0
EXPOSE 8080

# Liveness only. Readiness is `/readyz`, and it belongs to the load balancer
# rather than the supervisor: a process that cannot write its store must stay up
# long enough for an operator to ask it why.
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD ["/bin/bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/${DLL17_AUTHORITY_PORT} && printf 'GET /healthz HTTP/1.0\\r\\n\\r\\n' >&3 && head -n 1 <&3 | grep -q ' 200 '"]

ENTRYPOINT ["/opt/identity-authority/bin/identity-authority"]
