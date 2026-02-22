# Enhanced Rich Text Editor -- Developer Guide

This guide covers building, testing, and developing the Enhanced Rich Text Editor (ERTE) v6.x from source.

**Audience:** Developers contributing to ERTE or building extensions. For end-user feature documentation, see [User Guide](USER_GUIDE.md). For API reference, see [API Reference](API_REFERENCE.md).

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Repository Structure](#repository-structure)
- [Building from Source](#building-from-source)
- [Running the Demo](#running-the-demo)
- [Running Tests](#running-tests)
- [Test Architecture](#test-architecture)
- [Key Patterns and Debugging Tips](#key-patterns-and-debugging-tips)

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
| **enhanced-rich-text-editor-demo/** | Demo application with test views and Playwright specs | Active |
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

The ERTE test suite uses Playwright for end-to-end testing. Tests require a running server.

### Quick Start

```bash
# Terminal 1: Build and start server
bash v25-build.sh
bash v25-server-start.sh

# Terminal 2: Run tests
cd enhanced-rich-text-editor-demo
npx playwright test tests/erte/

# When done:
bash v25-server-stop.sh
```

### Test Commands

| Command | Purpose |
|---------|---------|
| `npx playwright test` | Run all tests (prototype + ERTE) |
| `npx playwright test tests/erte/` | Run only ERTE feature tests |
| `npx playwright test tests/erte/tabstops.spec.ts` | Run one specific spec file |
| `npx playwright test --ui` | Interactive UI mode (debug with browser) |
| `npx playwright test --headed` | Run with visible browser (not headless) |

### Test Results

Current baseline (as of latest build):

| Category | Count | Notes |
|----------|-------|-------|
| Prototype tests | 75 | All passing |
| ERTE feature tests | ~180 | See TEST_INVENTORY.md for exact counts |
| **Total** | **~255** | Some tests marked `.fixme` for known Quill 2 limitations |

Skipped/fixme tests document known Quill 2/Parchment 3 limitations, not ERTE bugs. See [TEST_INVENTORY.md](../enhanced-rich-text-editor-demo/tests/TEST_INVENTORY.md) for the full listing.

### Debugging Tests with Playwright UI

```bash
cd enhanced-rich-text-editor-demo
npx playwright test --ui
```

Opens an interactive browser where you can:
- Step through tests
- Inspect DOM and shadow DOM
- Re-run individual tests
- Watch logs in real-time

Useful for understanding test failures.

### Running Tests in Headed Mode

```bash
npx playwright test --headed
```

Runs with a visible browser window instead of headless. Helps see what the test is actually doing.

---

## Test Architecture

ERTE tests consist of two parts: **Java test views** (the backend) and **Playwright specs** (the test scripts).

### Test View Pattern

Each feature has a corresponding Java test view in `enhanced-rich-text-editor-demo/src/main/java/com/vaadin/componentfactory/`:

| View | Feature | URL Route |
|------|---------|-----------|
| `ErteTabStopTestView` | Tabstops, rulers, soft-break | `/erte-test/tabstops` |
| `ErteReadonlyTestView` | Readonly sections | `/erte-test/readonly` |
| `ErtePlaceholderTestView` | Placeholders | `/erte-test/placeholder` |
| `ErteToolbarTestView` | Toolbar slots & visibility | `/erte-test/toolbar` |
| `ErteToolbarPopoverTestView` | Toolbar popover helper | `/erte-test/toolbar-popover` |
| `ErteToolbarDialogTestView` | Toolbar dialog helper | `/erte-test/toolbar-dialog` |
| `ErteToolbarSelectPopupTestView` | Toolbar select popup helper | `/erte-test/toolbar-select-popup` |
| `ErteExtendOptionsTestView` | Extension hooks | `/erte-test/extend-options` |
| `ErteFeatureTestView` | Misc features (NBSP, i18n, align) | `/erte-test/features` |
| `ErteReplaceIconTestView` | Toolbar icon replacement | `/erte-test/replace-icon` |

**Test view anatomy:**

Each test view provides:
- An editor with ID `test-editor`
- A delta output element (`#delta-output`) — updated on every text-change
- An HTML output element (`#html-output`) — updated on every text-change
- An event log element (`#event-log`) — for feature-specific events
- A ready indicator (`#test-ready`) — signaled when the view is fully initialized

**Example structure** (from `ErteTabStopTestView`):

```java
var editor = new EnhancedRichTextEditor();
editor.setId("test-editor");
// ... configure editor ...

var deltaOutput = new Pre();
deltaOutput.setId("delta-output");

var htmlOutput = new Pre();
htmlOutput.setId("html-output");

var eventLog = new Div();
eventLog.setId("event-log");

// JavaScript listener: update outputs on every text-change
editor.getElement().executeJs(
    "const el = this;"
    + "const deltaOut = document.getElementById('delta-output');"
    + "const htmlOut = document.getElementById('html-output');"
    + "if (el._editor) {"
    + "  el._editor.on('text-change', () => { "
    + "    deltaOut.textContent = JSON.stringify(el._editor.getContents()); "
    + "    htmlOut.textContent = el.getHtmlValue(); "
    + "  });"
    + "}"
);

add(editor, deltaOutput, htmlOutput, eventLog);
```

### Playwright Spec Pattern

Test specs live in `enhanced-rich-text-editor-demo/tests/erte/` and use shared helpers from `helpers.ts`.

**Example test** (from `tabstops.spec.ts`):

```typescript
import { test, expect } from '@playwright/test';
import { waitForEditor, getEditor, getTabs, getDelta, ERTE_TEST_BASE } from './helpers';

const TABSTOPS_URL = `${ERTE_TEST_BASE}/tabstops`;

test.describe('ERTE Tabstops', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(TABSTOPS_URL);
    await waitForEditor(page);
  });

  test('Tab aligns to first tabstop', async ({ page }) => {
    const editor = getEditor(page);
    await editor.click();
    await page.keyboard.press('Tab');
    await page.keyboard.type('Text');

    const tabs = await getTabs(page).all();
    expect(tabs).toHaveLength(1);
  });
});
```

### Key Test Helpers

From `tests/erte/helpers.ts`:

| Helper | Purpose |
|--------|---------|
| `waitForEditor(page)` | Wait for editor to be fully initialized (Quill ready, contenteditable, Java ready indicator) |
| `getErte(page)`, `getEditor(page)` | Get component/editor locators |
| `getTabs(page)`, `getSoftBreaks(page)` | Get embed locators |
| `getDelta(page)` | Get Delta JSON from `#delta-output` |
| `getDeltaFromEditor(page)` | Get Delta directly from Quill (for silent updates) |
| `getEventLog(page)`, `clearEventLog(page)` | Manage feature event logs |
| `focusEditor(page)`, `typeInEditor(page, text)` | Simulate user input |
| `getRuler(page)`, `getRulerMarkers(page)` | Access tabstop ruler UI |
| `enableShowWhitespace(page)`, `isShowWhitespaceActive(page)` | Control whitespace display |

---

## Key Patterns and Debugging Tips

### Shadow DOM Traversal

The ERTE editor lives in Vaadin's shadow DOM. Playwright locators pierce shadow DOM automatically:

```typescript
// This works (Playwright auto-pierces shadow DOM)
await page.locator('#test-editor').locator('.ql-tab').click();
```

But `page.evaluate()` and `page.waitForFunction()` do NOT pierce shadow DOM:

```typescript
// This FAILS (can't see shadow DOM)
await page.evaluate(() => {
  const tab = document.querySelector('.ql-tab'); // null!
});

// This WORKS (use el.shadowRoot.querySelector)
await page.evaluate((elId) => {
  const el = document.getElementById(elId);
  const tab = el.shadowRoot.querySelector('.ql-tab'); // correct
}, 'test-editor');
```

### Ready Indicator

The Java test view sets `#test-ready[data-ready="true"]` when initialization is complete. This element has `display: none`, so you must wait with `state: 'attached'` (not `visible`):

```typescript
// WRONG: element is hidden, will timeout
await page.locator('#test-ready').waitFor({ state: 'visible' });

// CORRECT: check it's in the DOM
await page.locator('#test-ready[data-ready="true"]').waitFor({ state: 'attached' });
```

### Vaadin Component Interactions

Some Vaadin components have quirks in Playwright:

**Combo-box:** `fill()` doesn't trigger filtering. Use `pressSequentially()`:

```typescript
const combo = page.locator('vaadin-combo-box');
// WRONG: doesn't filter
await combo.fill('search text');

// CORRECT: pressSequentially triggers input events
await combo.pressSequentially('search text');
await combo.locator('vaadin-combo-box-overlay').getByRole('option').first().click();
```

**Confirm dialog:** Cancel button is hidden by default. Use Escape key:

```typescript
// Open dialog
await page.locator('vaadin-confirm-dialog').waitFor();

// Press Escape to cancel (OK button: click directly)
await page.keyboard.press('Escape');
```

### Delta Output Timing

The `#delta-output` element is updated by a client-side Quill `text-change` listener. This means:
- **Immediate typing:** Delta updates appear instantly
- **Server-side updates with SOURCE.SILENT:** Delta doesn't update (because `text-change` doesn't fire)
- **Solution:** Use `getDeltaFromEditor()` for server-side updates, which reads directly from Quill

```typescript
// After typing:
const delta = await getDelta(page); // Gets #delta-output

// After server-side setValue with SOURCE.SILENT:
const delta = await getDeltaFromEditor(page); // Gets directly from _editor
```

### Finding Server Errors

If a test fails mysteriously, check server logs for exceptions:

```bash
bash v25-server-logs.sh -errors
```

This shows only error/exception lines, making it easier to spot Java problems.

### Test View URL Routes

All test views are registered under `/erte-test/`. The route name comes from the `@Route` annotation:

```java
@Route("erte-test/tabstops")
public class ErteTabStopTestView extends VerticalLayout { }
```

Access it at: `http://localhost:8080/erte-test/tabstops`

---

## Next Steps

- **Contributing:** See [CONTRIBUTING.md](CONTRIBUTING.md) for code style, architecture constraints, and PR process
- **Testing new features:** Add a new test view in `enhanced-rich-text-editor-demo/src/main/java/com/vaadin/componentfactory/` and corresponding spec in `tests/erte/`
- **Reference:** [User Guide](USER_GUIDE.md) for feature documentation, [API Reference](API_REFERENCE.md) for the complete Java API
