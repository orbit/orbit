#!/usr/bin/env sh

# Start Orbit
cd /opt/orbit
java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 ./libs/orbit-application-fat.jar