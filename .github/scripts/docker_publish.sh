#!/bin/sh

docker login "$DOCKER_BASE_URL" -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"

docker build -t "${DOCKER_REPO}/orbit:${ORBIT_VERSION}" -f docker/server/Dockerfile .
docker push "${DOCKER_REPO}/orbit:${ORBIT_VERSION}"
