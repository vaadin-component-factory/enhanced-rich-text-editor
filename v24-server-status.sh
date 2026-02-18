#!/bin/bash
# Check V24 demo server status + all ports
PID_FILE="/tmp/claude-server.pid"
PORT=${1:-8080}

echo "=== ERTE V24 Demo Server Status ==="

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
