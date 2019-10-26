FROM azul/zulu-openjdk-alpine:11

ADD src/orbit-application/build/libs/orbit-application-fat-2-SNAPSHOT.jar /opt/orbit/libs/orbit-application-fat.jar
ADD docker/server/entrypoint.sh /opt/orbit/

RUN ["chmod", "+x", "/opt/orbit/entrypoint.sh"]

CMD ["/opt/orbit/entrypoint.sh"]