#!/bin/bash
# Stop V24 demo server
# Usage: bash server-stop.sh

PID_FILE="/tmp/claude-server.pid"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        kill "$PID" 2>/dev/null
        # Wait for process to exit (up to 10s)
        for i in $(seq 1 10); do
            kill -0 "$PID" 2>/dev/null || break
            sleep 1
        done
        if kill -0 "$PID" 2>/dev/null; then
            kill -9 "$PID" 2>/dev/null
        fi
        echo "Stopped server (PID: $PID)"
    else
        echo "PID $PID not running (stale pid file)"
    fi
    rm -f "$PID_FILE"
else
    echo "No PID file found, trying to find process..."
    pkill -f "spring-boot:run.*enhanced-rich-text-editor-demo" 2>/dev/null \
        && echo "Killed spring-boot process" \
        || echo "No process found"
fi

# Also kill any remaining java processes on port 8080
PID_ON_PORT=$(ss -tlpn 2>/dev/null | grep ":8080 " | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1)
if [ -n "$PID_ON_PORT" ]; then
    kill "$PID_ON_PORT" 2>/dev/null || true
    echo "Killed leftover process on port 8080 (PID: $PID_ON_PORT)"
fi
