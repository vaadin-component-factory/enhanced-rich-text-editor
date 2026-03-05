# Enhanced Rich Text Editor — Demo Application

Demo application for the Enhanced Rich Text Editor (ERTE) V25. Provides interactive
playground views for exploring ERTE features, comparing with stock RTE 2, and
loading sample content.

> **Looking for tests?** ERTE integration tests (Playwright) live in the
> [`enhanced-rich-text-editor-it/`](../enhanced-rich-text-editor-it/) module.

## Quick Start

```bash
# From the repository root:

# 1. Build all modules
bash build.sh

# 2. Start the demo server on port 8080
bash server-start.sh

# 3. Browse to http://localhost:8080

# 4. Stop the server when done
bash server-stop.sh
```

## Available Views

| Route | View Class | Description |
|-------|-----------|-------------|
| `/` | `ErtePlaygroundView` | Full-featured ERTE demo with tables, toolbar customization, dark/light mode |
| `/erte-samples` | `ErteSamplesView` | Pre-built sample content showcasing ERTE features |
| `/rte-playground` | `RtePlaygroundView` | Stock Vaadin RTE 2 (unmodified) for side-by-side comparison |

### Prototype

| Route | View Class | Description |
|-------|-----------|-------------|
| `/tab-stop` | *(frontend-only)* | Quill 2 tabstop prototype (pure JS, no ERTE) |

## Prototype Tests

The demo module includes a Playwright test for the tabstop prototype (75 tests).
This is a standalone Quill 2 prototype, separate from the ERTE component tests.

```bash
# Build and start demo server
bash build.sh
bash server-start.sh

# Run prototype tests
cd enhanced-rich-text-editor-demo
npx playwright test tests/tab-stop-prototype.spec.ts

# Stop server when done
cd ..
bash server-stop.sh
```

See [prototype_tests.md](prototype_tests.md) for details on the prototype test suite.

## Server Management Scripts

All scripts are in the repository root.

| Script | Description |
|--------|-------------|
| `build.sh [-q]` | Build V25 modules (`mvn clean install -DskipTests`) |
| `build-clean.sh [-q]` | Full clean build (wipes Vaadin dev bundle) |
| `server-start.sh [port]` | Start demo server (default port 8080) |
| `server-stop.sh` | Stop the demo server |
| `server-status.sh` | Check if the server is running |
| `server-logs.sh [-f\|-errors]` | View server logs |

## Project Structure

```
enhanced-rich-text-editor-demo/
  src/
    main/
      java/com/vaadin/componentfactory/
        Application.java              # Spring Boot entry point
        DemoLayout.java               # App layout with navigation
        ErtePlaygroundView.java       # Main ERTE demo
        ErteSamplesView.java          # Sample content showcase
        RtePlaygroundView.java        # Stock RTE 2 comparison
      frontend/
        src/
          tab-stop-prototype.js       # Quill 2 tabstop prototype
          sampleEditorExtensionConnector.js  # Extension example
  tests/
    tab-stop-prototype.spec.ts        # Prototype tests (75)
  playwright.config.ts                # Playwright configuration
```

## Tech Stack

- Java 21, Vaadin 25.0.x, Spring Boot 4.x
- Playwright for prototype testing
