# Developer Guide

Building, testing, and developing ERTE v6.x from source. For features, see [User Guide](../BASE_USER_GUIDE.md).

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Repository Structure](#repository-structure)
- [Building from Source](#building-from-source)
- [Running the Demo](#running-the-demo)
- [Running Tests](#running-tests)

---

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | 20+ (for Playwright tests) |
| Maven/Java | Dictated by Vaadin 25 project setup |

---

## Repository Structure

| Module | Purpose |
|--------|---------|
| **enhanced-rich-text-editor/** | Core ERTE (Java + JS) |
| **enhanced-rich-text-editor-tables/** | Tables addon |
| **enhanced-rich-text-editor-demo/** | Demo app + prototype tests (75) |
| **enhanced-rich-text-editor-it/** | Integration tests (test views + Playwright specs) |

**Development:** All work on `v25` branch.

---

## Building from Source

Always use root scripts:

```bash
bash build.sh             # mvn clean install -DskipTests
bash build.sh -q          # Quiet mode
bash build-clean.sh       # Clean frontend cache (for JS changes)
```

Use clean build if JS changes aren't visible or for troubleshooting. **Time:** ~1-2 min (standard), ~3-4 min (clean).

---

## Running the Demo

Spring Boot app on port 8080 with sample views, navigation, and prototype tests (75 tests).

**Start server:**
```bash
bash server-start.sh         # Port 8080
bash server-start.sh 9090    # Custom port
```

**Check status:**
```bash
bash server-status.sh
bash server-status.sh --wait --timeout 60
```

**View logs:**
```bash
bash server-logs.sh          # Last 50 lines
bash server-logs.sh -f       # Follow mode
bash server-logs.sh -errors  # Errors only
```

**Stop server:**
```bash
bash server-stop.sh
```

**Important:** Always stop server when done — runs in container, wastes resources if left running.

---

## Running Tests

ERTE tests in `enhanced-rich-text-editor-it/`, IT server on port 8081.

```bash
bash build-it.sh                          # Build IT module
bash it-server-start.sh                   # Start IT server
cd enhanced-rich-text-editor-it
npx playwright test tests/erte/               # Run tests
bash it-server-stop.sh                    # Stop IT server
```

See [TEST_INVENTORY.md](../../enhanced-rich-text-editor-it/tests/TEST_INVENTORY.md) for current test counts.

For architecture, debugging, and advanced commands, see [CONTRIBUTING.md](CONTRIBUTING.md#testing-requirements).

---

---

**See also:** [CONTRIBUTING.md](CONTRIBUTING.md) for code style and PR process, [EXTENDING.md](EXTENDING.md) for custom blots and extensions, [User Guide](../BASE_USER_GUIDE.md) for features.
