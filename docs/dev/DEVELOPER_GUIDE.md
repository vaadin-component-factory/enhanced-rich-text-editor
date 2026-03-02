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

The project is a multi-module Maven build. Each module has a distinct role:

| Module | Purpose |
|--------|---------|
| **enhanced-rich-text-editor/** | Core ERTE addon (Java + JavaScript) |
| **enhanced-rich-text-editor-tables/** | Tables extension for ERTE |
| **enhanced-rich-text-editor-demo/** | Demo application with sample views and prototype tests (75) |
| **enhanced-rich-text-editor-it/** | Integration tests (dedicated test views + Playwright specs) |

All development happens on the `v25` branch.

---

## Building from Source

Always use the root scripts instead of running Maven directly. They handle module ordering and skip tests automatically.

```bash
bash build.sh             # mvn clean install -DskipTests
bash build.sh -q          # Quiet mode
bash build-clean.sh       # Clean frontend cache (for JS changes)
```

Use `build-clean.sh` if your JS changes aren't showing up, or when troubleshooting build issues. Always rebuild before starting the server after code changes.

---

## Running the Demo

The demo is a Spring Boot application that showcases all ERTE features with interactive examples. It runs on port 8080 by default.

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

The integration tests run against a dedicated IT server (port 8081) with purpose-built test views. The tests use Playwright.

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

**See also:** [CONTRIBUTING.md](CONTRIBUTING.md) for code style and PR process, [EXTENDING.md](EXTENDING.md) for custom blots and extensions, [User Guide](../BASE_USER_GUIDE.md) for features.
