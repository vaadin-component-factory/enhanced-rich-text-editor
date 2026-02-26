# Enhanced Rich Text Editor -- Demo Application

Demo and test application for the Enhanced Rich Text Editor (ERTE) V25. Provides a
full-featured demo view and individual test views for each ERTE feature, plus a
Playwright test suite.

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

### Demo

| Route | View Class | Description |
|-------|-----------|-------------|
| `/` | `V25DemoView` | Main demo with all features, toolbar customization, dark/light mode |

### Test Views

All test views are accessible under `/erte-test/*` with a shared side navigation
(`ErteTestLayout`).

| Route | View Class | Features Tested |
|-------|-----------|-----------------|
| `/erte-test/toolbar` | `ErteToolbarTestView` | Slots, visibility, shortcuts |
| `/erte-test/readonly` | `ErteReadonlyTestView` | Readonly sections, protection |
| `/erte-test/tabstops` | `ErteTabStopTestView` | Tabstops, rulers, soft-break, whitespace |
| `/erte-test/placeholders` | `ErtePlaceholderTestView` | Placeholder dialog, events, formatting |
| `/erte-test/extend-options` | `ErteExtendOptionsTestView` | extendQuill, extendEditor hooks |
| `/erte-test/features` | `ErteFeatureTestView` | NBSP, addText, align, indent, i18n, sanitizer, focus |
| `/erte-test/replace-icons` | `ErteReplaceIconTestView` | Toolbar icon replacement |
| `/erte-test/toolbar-popover` | `ErteToolbarPopoverTestView` | ToolbarPopover component |
| `/erte-test/toolbar-select-popup` | `ErteToolbarSelectPopupTestView` | ToolbarSelectPopup component |
| `/erte-test/toolbar-dialog` | `ErteToolbarDialogTestView` | ToolbarDialog component |

### Spike/Prototype

| Route | View Class | Description |
|-------|-----------|-------------|
| `/tab-stop` | *(frontend-only)* | Quill 2 tabstop prototype (pure JS, no ERTE) |
| `/erte-spike/aura-proxy` | `ErteAuraSpikeView` | Aura theme proxy spike |

## Running Tests

The demo includes a Playwright test suite. The server must be running before executing tests.

```bash
# Build and start server
bash build.sh
bash server-start.sh

# Run all tests (287 total: 75 prototype + 212 ERTE)
cd enhanced-rich-text-editor-demo
npx playwright test

# Run only ERTE tests
npx playwright test tests/erte/

# Run a specific spec file
npx playwright test tests/erte/tabstops.spec.ts

# Run with Playwright UI for debugging
npx playwright test --ui

# Stop server when done
cd ..
bash server-stop.sh
```

### Test Suite Overview

| Spec File | Tests | Features |
|-----------|-------|----------|
| `erte/tabstops.spec.ts` | 78 | Tabstops, rulers, soft-break, whitespace indicators |
| `erte/placeholders.spec.ts` | 32 | Placeholder dialog, events, appearance, keyboard |
| `erte/toolbar.spec.ts` | 28 | Slot system, visibility, shortcuts, keyboard nav |
| `erte/features.spec.ts` | 36 | NBSP, addText, align, indent, i18n, sanitizer, focus |
| `erte/readonly.spec.ts` | 18 | Readonly sections, protection, whole-editor readonly |
| `erte/replace-icons.spec.ts` | 10 | Toolbar icon replacement |
| `erte/erte-shell.spec.ts` | 6 | Basic shell rendering, value sync |
| `erte/extend-options.spec.ts` | 4 | Extension hooks |
| `tab-stop-prototype.spec.ts` | 75 | Tabstop prototype (standalone Quill 2) |

See [TEST_INVENTORY.md](tests/TEST_INVENTORY.md) for the full test listing.

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
        V25DemoView.java          # Main demo
        ErteTestLayout.java       # Side nav layout for test views
        Erte*TestView.java        # Individual test views
      frontend/
        src/
          tab-stop-prototype.js   # Quill 2 tabstop prototype
  tests/
    erte/                         # ERTE Playwright tests
      helpers.ts                  # Shared test utilities
      *.spec.ts                   # Test specs by feature
    tab-stop-prototype.spec.ts    # Prototype tests
  playwright.config.ts            # Playwright configuration
```

## Tech Stack

- Java 21, Vaadin 25.0.5, Spring Boot 4.0.2
- Playwright for end-to-end testing
