#!/bin/bash
# Clean install all V25 modules (core + tables + demo)
# Usage: bash build-install.sh [options]
#   -q    Quiet mode (less Maven output)
#   -f    Also run vaadin:clean-frontend before build
#
# Builds from reactor root to resolve inter-module SNAPSHOT dependencies.
# Uses 'install' (not 'package') so spring-boot:run can resolve SNAPSHOTs.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
QUIET=""
CLEAN_FE=""

while getopts "qf" opt; do
    case $opt in
        q) QUIET="-q" ;;
        f) CLEAN_FE=true ;;
    esac
done

echo "=== ERTE V25 Clean Install ==="
echo "Root: $ROOT_DIR"

# Optional: clean Vaadin frontend caches
if [ "$CLEAN_FE" = true ]; then
    echo "--- Cleaning Vaadin frontend caches ---"
    for MOD in enhanced-rich-text-editor-25 enhanced-rich-text-editor-tables-25 enhanced-rich-text-editor-demo-25; do
        MOD_DIR="$ROOT_DIR/$MOD"
        rm -rf "$MOD_DIR/node_modules" "$MOD_DIR/frontend/generated" "$MOD_DIR/target/frontend" "$MOD_DIR/target/dev-bundle" 2>/dev/null
        echo "  Cleaned $MOD"
    done
fi

# Build all V25 modules via reactor
cd "$ROOT_DIR"
echo "--- mvn clean install -DskipTests (V25 modules) ---"
mvn clean install -DskipTests $QUIET \
    -pl enhanced-rich-text-editor-25,enhanced-rich-text-editor-tables-25,enhanced-rich-text-editor-demo-25 \
    -am

RC=$?
if [ $RC -eq 0 ]; then
    echo "=== BUILD SUCCESS ==="
else
    echo "=== BUILD FAILED (exit code: $RC) ==="
fi
exit $RC
