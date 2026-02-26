# Developer Guide

Building, testing, and developing ERTE v6.x from source. For features, see [User Guide](../../enhanced-rich-text-editor/docs/USER_GUIDE.md). For API, see [API Reference](../../enhanced-rich-text-editor/docs/API_REFERENCE.md).

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Repository Structure](#repository-structure)
- [Building from Source](#building-from-source)
- [Running the Demo](#running-the-demo)
- [Running Tests](#running-tests)

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21+ | Required for compilation and running the demo |
| Maven | 3.9+ | Used for building all modules |
| Node.js | 20+ | Required for Playwright tests |

**Verify:** `java -version` (21+), `mvn -version` (3.9+), `node -version` (20+)

Targets Vaadin 25.0.x and Spring Boot 4.x. See `enhanced-rich-text-editor-demo/pom.xml` for exact versions.

---

## Repository Structure

Multi-module Maven project:

| Module | Purpose |
|--------|---------|
| **enhanced-rich-text-editor/** | Core ERTE addon (Java + JavaScript) |
| **enhanced-rich-text-editor-tables/** | Tables addon for ERTE |
| **enhanced-rich-text-editor-demo/** | Demo application (sample views, prototype tests) |
| **enhanced-rich-text-editor-it/** | Integration tests (test views + Playwright specs) |

**Git workflow:** All development happens on the `v25` branch.

---

## Building from Source

Always use root scripts instead of Maven:

**Standard build:**
```bash
bash build.sh          # mvn clean install -DskipTests
bash build.sh -q       # Quiet mode
```

**Clean build (wipe frontend cache):**
```bash
bash build-clean.sh    # Also runs vaadin:clean-frontend
```

Use clean build if JS changes aren't showing, dependencies changed, or troubleshooting build issues.

**Time:** Standard ~1-2 min, clean ~3-4 min.

**Workflow:** Always build before starting server. Build again after code changes.

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

**See also:** [CONTRIBUTING.md](CONTRIBUTING.md) for code style and PR process, [EXTENDING.md](EXTENDING.md) for custom blots and extensions, [User Guide](../../enhanced-rich-text-editor/docs/USER_GUIDE.md) for features, [API Reference](../../enhanced-rich-text-editor/docs/API_REFERENCE.md) for Java API.
