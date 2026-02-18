#!/bin/bash
# Build V25 with full frontend cleanup
# Usage: bash v25-build-clean.sh [-q] [-a]
#   -q    Quiet mode
#   -a    Build ALL modules (V24 + V25)
exec bash "$(dirname "$0")/enhanced-rich-text-editor-demo-25/build-clean-frontend.sh" "$@"
