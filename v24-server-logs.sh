#!/bin/bash
# Print V24 demo server logs
# Usage: bash v24-server-logs.sh [-f|-state|-errors]
exec bash "$(dirname "$0")/enhanced-rich-text-editor-demo/print-server-logs.sh" "$@"
