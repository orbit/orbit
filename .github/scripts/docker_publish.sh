#!/bin/sh

docker login "$DOCKER_BASE_URL" -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"

cp "./src/orbit-application/build/libs/orbit-application-fat-${ORBIT_VERSION}.jar" ./src/orbit-application/build/libs/orbit-application-fat.jar
docker build -t "${DOCKER_REPO}/orbit:${ORBIT_VERSION}" -f docker/server/Dockerfile .
docker push "${DOCKER_REPO}/orbit:${ORBIT_VERSION}"
