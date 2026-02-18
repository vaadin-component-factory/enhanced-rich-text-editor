#!/bin/bash
# Stop V24 demo server
PID_FILE="/tmp/claude-server.pid"
PORT=${1:-8080}

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill "$PID" 2>/dev/null; then
        echo "Stopped server (PID: $PID)"
    else
        echo "Server already stopped (PID: $PID)"
    fi
    rm -f "$PID_FILE"
else
    echo "No PID file found"
fi

PID_ON_PORT=$(ss -tlpn 2>/dev/null | grep ":${PORT} " | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1 || true)
if [ -n "${PID_ON_PORT:-}" ]; then
    kill "$PID_ON_PORT" 2>/dev/null || true
    echo "Killed process on port $PORT (PID: $PID_ON_PORT)"
fi
