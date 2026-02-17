#!/bin/bash
# Start ERTE V25 demo server on port 8082
# Usage: bash server-start.sh [port]
# Default port: 8082

PORT=${1:-8082}
PID_FILE="/tmp/claude-demo25-server.pid"
LOG_FILE="/tmp/claude-demo25-server.log"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Stop any existing server first
if [ -f "$PID_FILE" ]; then
    kill $(cat "$PID_FILE") 2>/dev/null
    rm "$PID_FILE"
fi
# Also kill anything on our port
PID_ON_PORT=$(ss -tlpn 2>/dev/null | grep ":${PORT} " | sed -n 's/.*pid=\([0-9]*\).*/\1/p')
if [ -n "$PID_ON_PORT" ]; then
    kill $PID_ON_PORT 2>/dev/null
fi
sleep 2

# Start server from demo-25 directory
cd "$SCRIPT_DIR"
SERVER_PORT=$PORT mvn spring-boot:run > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"
echo "ERTE V25 demo server starting on port $PORT (PID: $(cat "$PID_FILE"))"
echo "Log: $LOG_FILE"
echo "Waiting for startup..."

# Poll for readiness (max 120 seconds)
for i in $(seq 1 60); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/" 2>/dev/null)
    if [ "$STATUS" = "200" ]; then
        echo "Server ready after ~$((i*2))s! http://localhost:${PORT}/"
        exit 0
    fi
    sleep 2
done

echo "Server may not be ready after 120s. Check: bash print-server-logs.sh"
exit 1
