# Enhanced Rich Text Editor -- Developer Guide

This guide covers building, testing, and developing the Enhanced Rich Text Editor (ERTE) v6.x from source.

**Audience:** Developers contributing to ERTE or building extensions. For end-user feature documentation, see [User Guide](../user/USER_GUIDE.md). For API reference, see [API Reference](../user/API_REFERENCE.md).

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Repository Structure](#repository-structure)
- [Building from Source](#building-from-source)
- [Running the Demo](#running-the-demo)
- [Running Tests](#running-tests)

---

## Prerequisites

Before building or testing ERTE, ensure your environment has the required tools:

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21+ | Required for compilation and running the demo |
| Maven | 3.9+ | Used for building all modules |
| Node.js | 20+ | Required for Playwright tests |

**Verify your setup:**

```bash
java -version          # Should show Java 21 or higher
mvn -version          # Should show Maven 3.9+
node -version         # Should show Node.js 20+
npm -version          # Node package manager
```

> **Note:** The project targets Vaadin 25.0.x and Spring Boot 4.x. See `/workspace/enhanced-rich-text-editor-demo/pom.xml` for exact versions.

---

## Repository Structure

This is a multi-module Maven project with the following layout:

| Module | Purpose | Status |
|--------|---------|--------|
| **enhanced-rich-text-editor-v25/** | Core ERTE addon (Java + JavaScript) | Active |
| **enhanced-rich-text-editor-tables-v25/** | Tables addon for ERTE | Active |
| **enhanced-rich-text-editor-demo/** | Demo application (sample views, prototype tests) | Active |
| **enhanced-rich-text-editor-it/** | Integration tests (test views + Playwright specs) | Active |
| enhanced-rich-text-editor/ | Legacy V24 core (reference only, excluded from build) | Archived |
| enhanced-rich-text-editor-tables/ | Legacy V24 tables (reference only, excluded from build) | Archived |

**Git workflow:** Development happens on the `v25` branch. The `master` branch tracks V24 production — never merge to master.

---

## Building from Source

The project provides convenient shell scripts for building. Always use these instead of running Maven manually.

### Standard Build

```bash
bash v25-build.sh
```

Runs `mvn clean install -DskipTests` for all V25 modules. This:
- Cleans previous artifacts
- Compiles Java source and tests
- Packages the addon JARs
- Installs to the local Maven repository

**Quiet mode** (no verbose output):

```bash
bash v25-build.sh -q
```

### Clean Build (Wiping Frontend Cache)

```bash
bash v25-build-clean.sh
```

Same as `v25-build.sh`, but also runs `vaadin:clean-frontend` to wipe the development bundle. Use this if:
- JavaScript changes aren't appearing in the demo
- You've changed dependencies
- You're troubleshooting build issues

**Time to build:** Standard build ~1-2 minutes. Clean build ~3-4 minutes (rebuilds frontend assets).

### Build Workflow

Always build before starting the server:

```bash
bash v25-build.sh      # Build once
bash v25-server-start.sh  # Then start server
# Make changes, repeat build and server restart as needed
```

---

## Running the Demo

The demo application (`enhanced-rich-text-editor-demo/`) is a Spring Boot app that serves:
- A home page with navigation links
- Test views for each ERTE feature (at `/erte-test/*`)
- Playwright test targets

### Starting the Server

```bash
bash v25-server-start.sh
```

Starts the demo on port 8080. The script:
- Kills any existing server on the port
- Starts Spring Boot in the background
- Waits up to 90 seconds for the server to be ready
- Prints the URL: `http://localhost:8080/`

**Custom port:**

```bash
bash v25-server-start.sh 9090
```

### Checking Server Status

```bash
bash v25-server-status.sh
```

Shows:
- Running process ID (or "stale" if process died)
- Port availability
- HTTP response code

**Wait mode** (polls until ready):

```bash
bash v25-server-status.sh --wait --timeout 60
```

Useful in scripts that need to wait for startup.

### Viewing Server Logs

```bash
bash v25-server-logs.sh
```

Shows the last 50 lines of the server log. Options:

| Command | Output |
|---------|--------|
| `bash v25-server-logs.sh` | Last 50 lines |
| `bash v25-server-logs.sh -f` | Stream logs (follow mode, Ctrl+C to exit) |
| `bash v25-server-logs.sh -state` | Last 50 lines (alias for default) |
| `bash v25-server-logs.sh -errors` | Only error/exception lines (last 30) |

**Quick troubleshooting:**

```bash
bash v25-server-logs.sh -errors  # Find what went wrong
```

### Stopping the Server

```bash
bash v25-server-stop.sh
```

Stops the running server and removes the PID file.

> **Important:** Always stop the server when done. The server runs inside a container and is not useful to the user — leaving it running wastes resources.

---

## Running Tests

ERTE tests live in the `enhanced-rich-text-editor-it/` module and run against a dedicated IT server on port 8081.

### Quick Start

```bash
# Build IT module
bash v25-build-it.sh

# Start IT server (port 8081)
bash v25-it-server-start.sh

# Run ERTE tests
cd enhanced-rich-text-editor-it
npx playwright test tests/erte/

# Stop IT server when done
bash v25-it-server-stop.sh
```

Current baseline: **306 ERTE tests** (+ 75 prototype tests in demo module = 381 total). See [TEST_INVENTORY.md](../../enhanced-rich-text-editor-it/tests/TEST_INVENTORY.md) for the full listing.

For test architecture details, debugging tips, and advanced commands, see [CONTRIBUTING.md](CONTRIBUTING.md#testing-requirements).

---

## Next Steps

- **Contributing:** See [CONTRIBUTING.md](CONTRIBUTING.md) for code style, test architecture, and PR process
- **Extending ERTE:** See [EXTENDING.md](EXTENDING.md) for custom blots, toolbar components, and keyboard shortcuts
- **Reference:** [User Guide](../user/USER_GUIDE.md) for feature documentation, [API Reference](../user/API_REFERENCE.md) for the complete Java API
