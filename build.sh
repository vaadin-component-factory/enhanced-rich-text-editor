#!/bin/bash
# Build V25 modules (mvn clean install -DskipTests)
# Usage: bash build.sh [-q]
#   -q    Quiet mode
QUIET=""
[ "$1" = "-q" ] && QUIET="-q"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"
echo "=== ERTE V25 Build ==="
mvn clean install -DskipTests $QUIET \
    -pl enhanced-rich-text-editor,enhanced-rich-text-editor-tables,enhanced-rich-text-editor-demo \
    -am
