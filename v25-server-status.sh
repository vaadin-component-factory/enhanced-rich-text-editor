#!/bin/bash
# Check V25 demo server status
# Usage: bash v25-server-status.sh [port]
PID_FILE="/tmp/claude-server-v25.pid"
PORT=${1:-8082}

echo "=== ERTE V25 Demo Server Status ==="

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 $PID 2>/dev/null; then
        echo "PID: $PID (running)"
    else
        echo "PID: $PID (stale)"
    fi
else
    echo "PID: no pid file"
fi

PID_ON_PORT=$(ss -tlpn 2>/dev/null | grep ":${PORT} " | sed -n 's/.*pid=\([0-9]*\).*/\1/p')
if [ -n "$PID_ON_PORT" ]; then
    echo "Port $PORT: in use (PID: $PID_ON_PORT)"
else
    echo "Port $PORT: free"
fi

STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/" 2>/dev/null)
echo "HTTP: ${STATUS:-not reachable}"
