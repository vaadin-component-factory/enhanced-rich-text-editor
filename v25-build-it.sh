#!/bin/bash
# Build V25 modules including IT module (mvn clean install -DskipTests)
# Usage: bash v25-build-it.sh [-q]
#   -q    Quiet mode
QUIET=""
[ "$1" = "-q" ] && QUIET="-q"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"
echo "=== ERTE V25 Build (with IT) ==="
mvn clean install -DskipTests $QUIET \
    -pl enhanced-rich-text-editor-v25,enhanced-rich-text-editor-tables-v25,enhanced-rich-text-editor-it \
    -am
