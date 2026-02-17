#!/bin/bash
# Stop spike server
# Usage: bash server-stop.sh

PID_FILE="/tmp/claude-spike-server.pid"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    kill $PID 2>/dev/null
    # Also kill child processes (the actual Java process)
    pkill -P $PID 2>/dev/null
    echo "Stopped spike server (PID: $PID)"
    rm "$PID_FILE"
else
    echo "No PID file found. Trying port-based cleanup..."
    # Try to find and kill by port 8081
    PID_ON_PORT=$(ss -tlpn 2>/dev/null | grep ":8081 " | sed -n 's/.*pid=\([0-9]*\).*/\1/p')
    if [ -n "$PID_ON_PORT" ]; then
        kill $PID_ON_PORT 2>/dev/null
        echo "Killed process on port 8081 (PID: $PID_ON_PORT)"
    else
        echo "No spike server found running"
    fi
fi
