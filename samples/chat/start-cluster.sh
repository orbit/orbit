#!/bin/sh

echo 'Starting the chat cluster'

mkdir -p log

./start-backend.sh > ./log/backend1.log &
./start-backend.sh > ./log/backend2.log &
./start-frontend.sh > ./log/frontend.log &
sleep 10
exo-open http://localhost:8080/ &

echo 'You will find logs in the log directory'
