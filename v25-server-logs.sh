#!/bin/bash
# Print V25 demo server logs
# Usage: bash v25-server-logs.sh [-f|-state|-errors]
exec bash "$(dirname "$0")/enhanced-rich-text-editor-demo-25/print-server-logs.sh" "$@"
