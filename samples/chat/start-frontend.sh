#!/bin/sh

echo 'Starting a chat frontend server'

cd chat-frontend
mvn exec:java 2>&1

