#!/bin/bash
# Build V25 modules including IT module (mvn clean install -DskipTests)
# Usage: bash build-it.sh [-q]
#   -q    Quiet mode
QUIET=""
[ "$1" = "-q" ] && QUIET="-q"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"
echo "=== ERTE V25 Build (with IT) ==="
mvn clean install -DskipTests $QUIET \
    -pl enhanced-rich-text-editor,enhanced-rich-text-editor-tables,enhanced-rich-text-editor-it \
    -am
