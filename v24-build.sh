#!/bin/bash
# Build V24 modules (mvn clean install -DskipTests)
# Usage: bash v24-build.sh [-q]
#   -q    Quiet mode
QUIET=""
[ "$1" = "-q" ] && QUIET="-q"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"
echo "=== ERTE V24 Build ==="
mvn clean install -DskipTests $QUIET \
    -pl enhanced-rich-text-editor,enhanced-rich-text-editor-tables,enhanced-rich-text-editor-demo \
    -am
