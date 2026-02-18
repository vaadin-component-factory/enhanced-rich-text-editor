#!/bin/bash
# Clean install with full Vaadin frontend cleanup
# Usage: bash build-clean-frontend.sh [options]
#   -q    Quiet mode (less Maven output)
#   -a    Also build V24 modules (full reactor)
#
# Steps:
# 1. Remove node_modules, generated frontend, dev-bundle from all V25 modules
# 2. Run vaadin:clean-frontend on each V25 module
# 3. Clean install all V25 modules
#
# Use this when:
# - Frontend JS changes aren't picked up
# - Vite/dev-bundle issues after dependency changes
# - After switching branches with different frontend deps

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
QUIET=""
BUILD_ALL=""

while getopts "qa" opt; do
    case $opt in
        q) QUIET="-q" ;;
        a) BUILD_ALL=true ;;
    esac
done

V25_MODULES=(
    enhanced-rich-text-editor-25
    enhanced-rich-text-editor-tables-25
    enhanced-rich-text-editor-demo-25
)

echo "=== ERTE V25 Clean Frontend + Install ==="
echo "Root: $ROOT_DIR"

# Step 1: Remove cached frontend artifacts
echo ""
echo "--- Step 1: Remove cached frontend artifacts ---"
for MOD in "${V25_MODULES[@]}"; do
    MOD_DIR="$ROOT_DIR/$MOD"
    if [ -d "$MOD_DIR" ]; then
        rm -rf "$MOD_DIR/node_modules" 2>/dev/null && echo "  $MOD: node_modules removed"
        rm -rf "$MOD_DIR/frontend/generated" 2>/dev/null
        rm -rf "$MOD_DIR/target/frontend" 2>/dev/null
        rm -rf "$MOD_DIR/target/dev-bundle" 2>/dev/null
        rm -rf "$MOD_DIR/target/classes/META-INF/VAADIN" 2>/dev/null
        echo "  $MOD: target frontend caches removed"
    fi
done

# Step 2: Run vaadin:clean-frontend on demo module (which pulls in all deps)
echo ""
echo "--- Step 2: vaadin:clean-frontend ---"
cd "$ROOT_DIR"
mvn vaadin:clean-frontend $QUIET \
    -pl enhanced-rich-text-editor-demo-25 2>/dev/null
echo "  vaadin:clean-frontend done"

# Step 3: Clean install
echo ""
echo "--- Step 3: mvn clean install -DskipTests ---"
if [ "$BUILD_ALL" = true ]; then
    echo "  Building ALL modules (full reactor)..."
    mvn clean install -DskipTests $QUIET
else
    echo "  Building V25 modules only..."
    mvn clean install -DskipTests $QUIET \
        -pl enhanced-rich-text-editor-25,enhanced-rich-text-editor-tables-25,enhanced-rich-text-editor-demo-25 \
        -am
fi

RC=$?
if [ $RC -eq 0 ]; then
    echo ""
    echo "=== BUILD SUCCESS ==="
    echo "To start the V25 demo: bash $SCRIPT_DIR/server-start.sh"
else
    echo ""
    echo "=== BUILD FAILED (exit code: $RC) ==="
fi
exit $RC
