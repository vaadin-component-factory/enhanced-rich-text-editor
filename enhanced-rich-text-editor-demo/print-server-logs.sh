#!/bin/bash
# Print server logs
# Usage: bash print-server-logs.sh [-f|-state|-errors]

LOG_FILE="/tmp/claude-server.log"

if [ ! -f "$LOG_FILE" ]; then
    echo "No log file found at $LOG_FILE"
    exit 1
fi

case "${1:---tail}" in
    -f)      tail -f "$LOG_FILE" ;;
    -state)  grep -iE "state|ready|started|listening|processing" "$LOG_FILE" | tail -50 ;;
    -errors) grep -iE "error|exception|fail" "$LOG_FILE" | tail -50 ;;
    *)       tail -500 "$LOG_FILE" ;;
esac
