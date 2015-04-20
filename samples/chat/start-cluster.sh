#!/bin/sh

echo 'Starting the chat cluster'

mkdir -p log

./start-backend.sh > ./log/backend1.log &
./start-backend.sh > ./log/backend2.log &
./start-frontend.sh > ./log/frontend.log &
sleep 10
# Check for exo-open
command -v exo-open > /dev/null 2>&1
if [ $? -eq 0 ]
then
    exo-open http://localhost:8080/ &
else
    echo 'You will find the chat interface at http://localhost:8080'
fi

echo 'You will find logs in the log directory'
