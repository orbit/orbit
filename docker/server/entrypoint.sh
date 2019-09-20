#!/usr/bin/env sh

JAVA_HOME=/usr/local/jdk-11
PATH=$JAVA_HOME/bin:"$PATH"

# Start Marketplace
cd /opt/orbit
java -jar ./libs/orbit-application-all-2.0.0-SNAPSHOT.jar