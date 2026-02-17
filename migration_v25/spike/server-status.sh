#!/bin/bash
# Check spike server status
# Usage: bash server-status.sh [port]
# Default port: 8081

PORT=${1:-8081}
PID_FILE="/tmp/claude-spike-server.pid"

echo "=== Spike Server Status ==="

# Check PID file
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 $PID 2>/dev/null; then
        echo "PID: $PID (running)"
    else
        echo "PID: $PID (stale - process not running)"
    fi
else
    echo "PID: no pid file"
fi

# Check port
PID_ON_PORT=$(ss -tlpn 2>/dev/null | grep ":${PORT} " | sed -n 's/.*pid=\([0-9]*\).*/\1/p')
if [ -n "$PID_ON_PORT" ]; then
    echo "Port $PORT: in use (PID: $PID_ON_PORT)"
else
    echo "Port $PORT: free"
fi

# HTTP check
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/" 2>/dev/null)
if [ "$STATUS" = "200" ]; then
    echo "HTTP: OK (200)"
elif [ "$STATUS" = "000" ]; then
    echo "HTTP: not reachable"
else
    echo "HTTP: $STATUS"
fi

# Also check demo server (port 8080)
DEMO_PID_ON_PORT=$(ss -tlpn 2>/dev/null | grep ":8080 " | sed -n 's/.*pid=\([0-9]*\).*/\1/p')
if [ -n "$DEMO_PID_ON_PORT" ]; then
    echo "Demo server (8080): running (PID: $DEMO_PID_ON_PORT)"
fi
