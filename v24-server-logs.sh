#!/bin/bash
# Print V24 demo server logs
# Usage: bash v24-server-logs.sh [-f|-state|-errors]
LOG_FILE="/tmp/claude-server.log"

if [ ! -f "$LOG_FILE" ]; then
    echo "No log file found at $LOG_FILE"
    exit 1
fi

case "${1:-}" in
    -f)      tail -f "$LOG_FILE" ;;
    -state)  tail -50 "$LOG_FILE" ;;
    -errors) grep -i "error\|exception\|fatal" "$LOG_FILE" | tail -30 ;;
    *)       tail -50 "$LOG_FILE" ;;
esac
