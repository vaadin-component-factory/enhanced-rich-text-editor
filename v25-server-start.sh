#!/bin/bash
# Start V25 demo server (port 8082)
# Usage: bash v25-server-start.sh [port]
exec bash "$(dirname "$0")/enhanced-rich-text-editor-demo-25/server-start.sh" "$@"
