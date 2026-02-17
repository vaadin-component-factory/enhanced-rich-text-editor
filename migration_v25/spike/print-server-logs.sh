#!/bin/bash
# Print spike server logs
# Usage: bash print-server-logs.sh [-f|-tail N|-state|-errors]

LOG_FILE="/tmp/claude-spike-server.log"

if [ ! -f "$LOG_FILE" ]; then
    echo "No log file found at $LOG_FILE"
    exit 1
fi

case "$1" in
    -f)       tail -f "$LOG_FILE" ;;
    -tail)    tail -${2:-100} "$LOG_FILE" ;;
    -state)   grep -i "state\|ready\|started\|failed" "$LOG_FILE" | tail -50 ;;
    -errors)  grep -i "error\|exception\|failed" "$LOG_FILE" | tail -50 ;;
    *)        tail -500 "$LOG_FILE" ;;
esac
