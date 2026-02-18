#!/bin/bash
# Start V24 demo server (port 8080)
# Usage: bash v24-server-start.sh [port]
exec bash "$(dirname "$0")/enhanced-rich-text-editor-demo/server-start.sh" "$@"
