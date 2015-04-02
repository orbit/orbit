#!/bin/sh

echo 'Starting a chat backend server'

cd chat-actors
mvn exec:java 2>&1
