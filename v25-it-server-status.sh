#!/bin/bash
# Check V25 IT server status
# Usage: bash v25-it-server-status.sh [options] [port]
#   -w, --wait    Wait for server to be ready (HTTP 200)
#   -t, --timeout Timeout in seconds for wait mode (default: 60)
PID_FILE="/tmp/claude-it-server.pid"

# Parse arguments
WAIT_MODE=false
TIMEOUT=60
PORT=8081

while [[ $# -gt 0 ]]; do
    case $1 in
        -w|--wait)
            WAIT_MODE=true
            shift
            ;;
        -t|--timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        [0-9]*)
            PORT="$1"
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Wait mode: poll until server is ready
if [ "$WAIT_MODE" = true ]; then
    echo "Waiting for IT server on port $PORT (timeout: ${TIMEOUT}s)..."
    for i in $(seq 1 $TIMEOUT); do
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/" 2>/dev/null)
        if [ "$STATUS" = "200" ]; then
            echo "IT server ready (HTTP 200)"
            exit 0
        fi
        sleep 1
    done
    echo "Timeout: IT server not ready after ${TIMEOUT}s"
    exit 1
fi

# Normal status check
echo "=== ERTE V25 IT Server Status ==="

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
