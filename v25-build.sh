#!/bin/bash
# Build all V25 modules (clean install)
# Usage: bash v25-build.sh [-f] [-q]
#   -f    Also clean Vaadin frontend caches
#   -q    Quiet mode
exec bash "$(dirname "$0")/enhanced-rich-text-editor-demo-25/build-install.sh" "$@"
