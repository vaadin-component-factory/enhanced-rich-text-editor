#!/bin/bash
# Check V25 demo server status
# Usage: bash v25-server-status.sh [port]
exec bash "$(dirname "$0")/enhanced-rich-text-editor-demo-25/server-status.sh" "$@"
