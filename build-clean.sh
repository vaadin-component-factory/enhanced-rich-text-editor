#!/bin/bash
# Build V25 modules with full Vaadin frontend cleanup
# Usage: bash build-clean.sh [-q]
#   -q    Quiet mode
QUIET=""
[ "$1" = "-q" ] && QUIET="-q"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"
echo "=== ERTE V25 Build (clean frontend) ==="
echo "--- Cleaning Vaadin frontend caches ---"
for MOD in enhanced-rich-text-editor enhanced-rich-text-editor-tables enhanced-rich-text-editor-demo; do
    MOD_DIR="$ROOT_DIR/$MOD"
    rm -rf "$MOD_DIR/node_modules" "$MOD_DIR/frontend/generated" "$MOD_DIR/target/frontend" "$MOD_DIR/target/dev-bundle" 2>/dev/null
    echo "  Cleaned $MOD"
done
echo "--- vaadin:clean-frontend + clean install -DskipTests ---"
mvn com.vaadin:vaadin-maven-plugin:clean-frontend clean install -DskipTests $QUIET \
    -pl enhanced-rich-text-editor,enhanced-rich-text-editor-tables,enhanced-rich-text-editor-demo \
    -am
