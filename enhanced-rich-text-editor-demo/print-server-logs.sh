#!/bin/bash
# Print server logs

if [ ! -f /tmp/claude-server.log ]; then
    echo "No log file found at /tmp/claude-server.log"
    exit 1
fi

# Default: last 500 lines, or pass -f for follow mode
if [ "$1" = "-f" ]; then
    tail -f /tmp/claude-server.log
elif [ "$1" = "-state" ]; then
    grep -i "state\|ready\|processing\|question" /tmp/claude-server.log | tail -50
else
    tail -500 /tmp/claude-server.log
fi
