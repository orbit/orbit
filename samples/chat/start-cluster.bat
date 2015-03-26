@echo off

echo Starting the chat cluster

start start-backend.bat
start start-backend.bat
start start-frontend.bat
start http://localhost:8080/