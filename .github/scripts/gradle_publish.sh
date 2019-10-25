#!/bin/sh

./gradlew publish --exclude-task test -Porbit.version="$ORBIT_VERSION" -PremotePublish=true -PinMemoryKey=true -Ppublish.url="$MAVEN_URL" -Ppublish.url="$MAVEN_URL" -Ppublish.username="$MAVEN_USERNAME" -Ppublish.password="$MAVEN_PASSWORD"