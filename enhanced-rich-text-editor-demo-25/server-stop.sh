#!/bin/bash
# Stop ERTE V25 demo server
# Usage: bash server-stop.sh

PID_FILE="/tmp/claude-demo25-server.pid"
DEFAULT_PORT=8082

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    kill $PID 2>/dev/null
    # Also kill child processes (the actual Java process)
    pkill -P $PID 2>/dev/null
    echo "Stopped ERTE V25 demo server (PID: $PID)"
    rm "$PID_FILE"
else
    echo "No PID file found. Trying port-based cleanup..."
    PID_ON_PORT=$(ss -tlpn 2>/dev/null | grep ":${DEFAULT_PORT} " | sed -n 's/.*pid=\([0-9]*\).*/\1/p')
    if [ -n "$PID_ON_PORT" ]; then
        kill $PID_ON_PORT 2>/dev/null
        echo "Killed process on port $DEFAULT_PORT (PID: $PID_ON_PORT)"
    else
        echo "No ERTE V25 demo server found running"
    fi
fi
