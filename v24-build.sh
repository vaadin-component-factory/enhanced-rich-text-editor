#!/bin/bash
# Build V24 modules (standalone, not part of root reactor)
# Usage: bash v24-build.sh [-q]
#   -q    Quiet mode
QUIET=""
[ "$1" = "-q" ] && QUIET="-q"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"
echo "=== ERTE V24 Build ==="
mvn -f enhanced-rich-text-editor/pom.xml clean install -DskipTests $QUIET && \
mvn -f enhanced-rich-text-editor-tables/pom.xml clean install -DskipTests $QUIET
