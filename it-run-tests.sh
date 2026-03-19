#!/bin/bash
# Run ERTE Playwright E2E tests
# Usage: bash run-e2e-tests.sh [filter]
#   filter: optional grep pattern to run specific tests, e.g. "tabstops" or "toolbar"
# Requires: IT server running on port 8081 (bash it-server-start.sh)
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
FILTER="${1:-}"

echo "=== ERTE V25 E2E Tests ==="

# Check IT server is running
IT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8081/" 2>/dev/null || true)
if [ "$IT_STATUS" != "200" ] && [ "$IT_STATUS" != "302" ]; then
    echo "ERROR: IT server not running on port 8081 (HTTP $IT_STATUS)"
    echo "Start it first: bash it-server-start.sh"
    exit 1
fi

cd "$ROOT_DIR/enhanced-rich-text-editor-it"

if [ -n "$FILTER" ]; then
    echo "Running tests matching: $FILTER"
    npx playwright test tests/erte/ --grep "$FILTER"
else
    echo "Running all ERTE tests"
    npx playwright test tests/erte/
fi
