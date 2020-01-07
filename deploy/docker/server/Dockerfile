FROM azul/zulu-openjdk-alpine:11

ARG GRPC_HEALTH_PROBE_VERSION=v0.3.1

RUN wget -qO/bin/grpc_health_probe https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/$GRPC_HEALTH_PROBE_VERSION/grpc_health_probe-linux-amd64 && \
    chmod +x /bin/grpc_health_probe

ADD src/orbit-application/build/libs/orbit-application-fat.jar /opt/orbit/libs/orbit-application-fat.jar
ADD deploy/docker/server/entrypoint.sh /opt/orbit/

RUN ["chmod", "+x", "/opt/orbit/entrypoint.sh"]

CMD ["/opt/orbit/entrypoint.sh"]