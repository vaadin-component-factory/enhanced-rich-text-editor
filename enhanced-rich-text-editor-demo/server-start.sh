#!/bin/bash
# Start test server on port 8080

# Stop any existing server first (by PID file or by port)
if [ -f /tmp/claude-server.pid ]; then
    kill $(cat /tmp/claude-server.pid) 2>/dev/null
fi
# Also kill anything on port 8080
PID_ON_PORT=$(ss -tlpn | grep 8080 | sed -n 's/.*pid=\([0-9]*\).*/\1/p')
if [ -n "$PID_ON_PORT" ]; then
    kill $PID_ON_PORT 2>/dev/null
fi
sleep 2

# Start server
SERVER_PORT=8080 ./mvnw spring-boot:run > /tmp/claude-server.log 2>&1 &
echo $! > /tmp/claude-server.pid
echo "Server starting on port 8080 (PID: $(cat /tmp/claude-server.pid))"
echo "Waiting 25 seconds for startup..."
sleep 25

# Check status
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/)
if [ "$STATUS" = "200" ]; then
    echo "Server ready! http://localhost:8080/"
else
    echo "Server may not be ready (HTTP $STATUS). Check logs with ./print-server-logs.sh"
fi
