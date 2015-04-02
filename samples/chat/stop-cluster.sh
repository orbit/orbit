#!/bin/sh

echo "Killing all java processes on the current TTY"
ps --no-headers -o pid,fname | grep java | cut -d' ' -f1 | xargs kill
echo "Goodbye."

