# Developer Guide

Everything you need to build, test, and run ERTE from source. Whether you're fixing a bug, adding a feature, or just exploring — this gets you up and running. For end-user features, see the [User Guide](../BASE_USER_GUIDE.md).

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

The project is a multi-module Maven build. Each module has its own job, and you'll rarely need to touch more than one or two at a time:

| Module | Purpose |
|--------|---------|
| **enhanced-rich-text-editor/** | Core ERTE addon (Java + JavaScript) |
| **enhanced-rich-text-editor-tables/** | Tables extension for ERTE |
| **enhanced-rich-text-editor-demo/** | Demo application with sample views and prototype tests (75) |
| **enhanced-rich-text-editor-it/** | Integration tests (dedicated test views + Playwright specs) |

All development happens on the `v25` branch.

---

## Building from Source

Use the root scripts rather than running Maven directly — they handle module ordering and skip tests automatically, so you don't have to remember the right flags.

```bash
bash build.sh             # Standard build (clean install, skip tests)
bash build.sh -q          # Quiet mode (less output)
bash build-clean.sh       # Clean frontend cache + rebuild
```

If your JS changes aren't showing up, `build-clean.sh` usually fixes it — it wipes Vaadin's dev bundle cache and forces a fresh frontend build. Always rebuild before starting the server after code changes.

---

## Running the Demo

The demo is a Spring Boot application that showcases all ERTE features with interactive examples. Runs on port 8080 by default.

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

**Important:** Always stop the server when you're done — it runs in a container and continues consuming resources until explicitly stopped.

---

## Running Tests

The integration tests use Playwright and run against a dedicated IT server (port 8081) with purpose-built test views. The workflow is straightforward:

```bash
bash build-it.sh                          # Build IT module
bash it-server-start.sh                   # Start IT server (port 8081)
cd enhanced-rich-text-editor-it
npx playwright test tests/erte/           # Run all ERTE tests
bash it-server-stop.sh                    # Stop IT server when done
```

See [TEST_INVENTORY.md](../../enhanced-rich-text-editor-it/tests/TEST_INVENTORY.md) for the current test suite overview. For test architecture details, debugging tips, and advanced commands, see [CONTRIBUTING.md](CONTRIBUTING.md#testing-requirements).

---

**Where to go from here:** [CONTRIBUTING.md](CONTRIBUTING.md) for code style and PR process, [EXTENDING.md](EXTENDING.md) for adding your own blots and toolbar components, [User Guide](../BASE_USER_GUIDE.md) for the full feature reference.
