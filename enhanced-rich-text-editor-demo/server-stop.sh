#!/bin/bash
# Stop test server on port 8081

if [ -f /tmp/claude-server.pid ]; then
    PID=$(cat /tmp/claude-server.pid)
    kill $PID 2>/dev/null
    echo "Stopped server (PID: $PID)"
    rm /tmp/claude-server.pid
else
    echo "No PID file found, trying to find process..."
    pkill -f "spring-boot:run" 2>/dev/null && echo "Killed spring-boot process" || echo "No process found"
fi
