#!/bin/bash
# Start V25 demo server on port 8082
# Usage: bash v25-server-start.sh [port]
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PORT=${1:-8082}
PID_FILE="/tmp/claude-server-v25.pid"
LOG_FILE="/tmp/claude-server-v25.log"

# Stop any existing server first
if [ -f "$PID_FILE" ]; then
    kill "$(cat "$PID_FILE")" 2>/dev/null || true
    rm -f "$PID_FILE"
fi
PID_ON_PORT=$(ss -tlpn 2>/dev/null | grep ":${PORT} " | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1 || true)
if [ -n "${PID_ON_PORT:-}" ]; then
    kill "$PID_ON_PORT" 2>/dev/null || true
fi
sleep 2

cd "$ROOT_DIR"
mvn -pl enhanced-rich-text-editor-demo spring-boot:run \
    -Dspring-boot.run.arguments="--server.port=${PORT}" \
    > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"
echo "Server starting on port $PORT (PID: $(cat "$PID_FILE"))"

echo "Waiting for server to be ready..."
for i in $(seq 1 90); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/" 2>/dev/null || true)
    if [ "$STATUS" = "200" ] || [ "$STATUS" = "302" ]; then
        echo "Server ready after ${i}s! http://localhost:${PORT}/"
        exit 0
    fi
    if ! kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "ERROR: Server process died. Check logs: bash v25-server-logs.sh"
        tail -20 "$LOG_FILE"
        exit 1
    fi
    sleep 1
done

echo "WARNING: Server not ready after 90s (last HTTP $STATUS). Check logs:"
echo "  bash v25-server-logs.sh"
