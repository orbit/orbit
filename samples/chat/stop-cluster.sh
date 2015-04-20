#!/bin/sh

echo "Killing all java processes on the current TTY"
myTTY=`tty | cut -d/ -f3`
ps -o pid,tty,command | awk -v myTTY="$myTTY" '$3 ~ /java/ && $2 ~ myTTY {print $1}'| xargs kill
echo "Goodbye."

