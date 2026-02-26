# Enhanced Rich Text Editor -- Contributing Guide

This guide explains how to contribute to the Enhanced Rich Text Editor (ERTE) project. It covers the git workflow, code style, architecture constraints, and testing requirements.

**Before contributing:** Read [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for setup and build instructions. For end-user docs, see [User Guide](../user/USER_GUIDE.md).

---

## Table of Contents

- [Getting Started](#getting-started)
- [Git Workflow](#git-workflow)
- [Code Style](#code-style)
- [Architecture Constraints](#architecture-constraints)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)

---

## Getting Started

1. **Fork the repository** (if external contributor) or clone directly (if team member)
2. **Set up your environment:**
   ```bash
   git clone https://github.com/your-org/enhanced-rich-text-editor.git
   cd enhanced-rich-text-editor
   ```
3. **Build the project:**
   ```bash
   bash v25-build.sh
   ```
4. **Start the demo:**
   ```bash
   bash v25-server-start.sh
   ```
5. **Run tests to verify your environment:**
   ```bash
   bash v25-build-it.sh
   bash v25-it-server-start.sh
   cd enhanced-rich-text-editor-it
   npx playwright test tests/erte/
   bash v25-it-server-stop.sh
   ```

See [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for detailed setup steps.

---

## Git Workflow

ERTE uses a simple branching model:

### Branch Strategy

| Branch | Purpose | Merge Target |
|--------|---------|--------------|
| `v25` | Active development | (main working branch) |
| `feature/*` | New features | `v25` |
| `fix/*` | Bug fixes | `v25` |

### Golden Rule

All development happens on the `v25` branch. Create feature and fix branches from `v25` and submit pull requests back to `v25`.

### Creating a Feature Branch

```bash
# Update v25 with latest changes
git checkout v25
git pull origin v25

# Create your feature branch
git checkout -b feature/your-feature-name

# Make changes, commit locally
git add .
git commit -m "Add your feature"

# Push to remote
git push origin feature/your-feature-name
```

### Commit Messages

Use clear, concise commit messages in **English**:

```
Add tabstop blot for Parchment 3

- Implement Embed blot with Quill 2 lifecycle
- Add CSS for tab alignment
- Add tests for tab positioning

Fixes #123
```

**Format:**
- **Title:** One line, imperative mood, under 50 characters
- **Body:** Detailed explanation, wrapped at 72 characters
- **References:** Link related issues with `Fixes #123` or `Relates to #456`

---

## Code Style

### Java Code

- **Language:** English (all identifiers, comments, documentation)
- **Format:** Standard Java conventions (4-space indentation)
- **License header:** Every Java file must include the CVALv3 license header (see below)
- **Javadoc:** Document all public methods, classes, and enums
- **Imports:** Organized and unused imports removed

**License header template** (required on all Java files):

```java
/*-
 * #%L
 * Enhanced Rich Text Editor V25
 * %%
 * Copyright (C) 2019 - 2025 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file license.html distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */
package com.example;
```

**Example Java method:**

```java
/**
 * Sets the tabstops for this editor.
 *
 * @param tabStops list of tabstops ordered by position (left-to-right)
 * @throws IllegalArgumentException if tabStops is null
 */
public void setTabStops(List<TabStop> tabStops) {
    Objects.requireNonNull(tabStops, "tabStops cannot be null");
    this.tabStops = new ArrayList<>(tabStops);
    updateRuler();
}
```

### JavaScript Code

- **Language:** English (all identifiers, comments)
- **Framework:** ES2020+ class syntax (Lit patterns)
- **NO Polymer patterns:** This project uses Lit, not Polymer
- **DOM manipulation:** Use `createElement()` / `appendChild()`, never `innerHTML` with dynamic content
- **Inline comments:** Add comments for complex logic (e.g., Quill integration, sanitization)

**Example JavaScript method:**

```javascript
/**
 * Registers custom keyboard bindings for tabstops.
 * Overrides default Tab key behavior to insert tab blots.
 *
 * @private
 */
_registerTabBinding() {
  // Clear default Tab binding and add custom handler
  this._editor.keyboard.bindings['Tab'] = [
    {
      key: 'Tab',
      handler: () => this._insertTab(),
    }
  ];
}

_insertTab() {
  const index = this._editor.getSelection().index;
  this._editor.insertEmbed(index, 'tab', {}, Quill.sources.USER);
  this._editor.setSelection(index + 1, 0, Quill.sources.USER);
  return true;
}
```

### TypeScript Test Code

- **Language:** English
- **Format:** 2-space indentation (Playwright convention)
- **Structure:** `test.describe()` blocks, clear test names
- **Assertions:** Use Playwright `expect()` for all assertions

**Example test:**

```typescript
test.describe('Tabstop Alignment', () => {
  test('Tab aligns to left tabstop', async ({ page }) => {
    await page.goto(TABSTOPS_URL);
    await waitForEditor(page);

    const editor = getEditor(page);
    await editor.click();
    await page.keyboard.press('Tab');

    const tabs = await getTabs(page).all();
    expect(tabs).toHaveLength(1);
  });
});
```

---

## Architecture Constraints

**Hard requirements enforced in code review. Violating them breaks updatability or introduces security issues.**

### 1. Never Copy RTE 2 Source Code

Extend via inheritance, not copying:

```java
// CORRECT: extend via inheritance
public class EnhancedRichTextEditor extends RichTextEditor {
    @Override
    protected void setPresentationValue(String value) {
        // Extend, don't copy
    }
}

// WRONG: copying RTE 2 classes breaks with RTE updates
public class MyEditor extends RichTextEditor {
    // ... RTE 2 source code copied here ...
}
```

### 2. Keep All ERTE Logic in com.vaadin.componentfactory

All ERTE classes live in the `com.vaadin.componentfactory` package:

```java
// CORRECT
package com.vaadin.componentfactory;
public class EnhancedRichTextEditor extends RichTextEditor { }
public class TabStopHelper { }

// WRONG: don't split ERTE across packages
package com.vaadin.flow.component.richtexteditor;
public class ErteHelper { }
```

### 3. Java Value Sync Timing

Never set content in `ready()` — it gets overwritten by Java value sync. Set via `setPresentationValue()`:

```java
// WRONG: content gets overwritten
setValue("<p>Content</p>"); // in ready(), attached, etc.

// CORRECT: react to value in setPresentationValue
@Override
protected void setPresentationValue(String value) {
    super.setPresentationValue(value);
    // Value is already set, safe to react here
}
```

### 4. Embed Guard Nodes (Quill 2)

Never set `contenteditable="false"` on Embed outer domNode — Quill 2 guard nodes inside must stay editable. See [EXTENDING.md — Embed Blot Gotchas](EXTENDING.md#embed-blot-gotchas) for details and code examples.

See [CLAUDE.md](../../CLAUDE.md) for the complete list of confirmed patterns and architecture decisions.

---

## Test Architecture

ERTE tests consist of Java test views (the backend) and Playwright specs (the test scripts).

### Test Commands

Run tests from the `enhanced-rich-text-editor-it/` directory:

| Command | Purpose |
|---------|---------|
| `npx playwright test` | Run all tests (prototype + ERTE) |
| `npx playwright test tests/erte/` | Run only ERTE feature tests |
| `npx playwright test tests/erte/tabstops.spec.ts` | Run one specific spec file |
| `npx playwright test --ui` | Interactive UI mode (debug with browser) |
| `npx playwright test --headed` | Run with visible browser (not headless) |

### Test Results Baseline

| Category | Count | Notes |
|----------|-------|-------|
| Prototype tests | 75 | In demo module, all passing |
| ERTE feature tests | 306 | See TEST_INVENTORY.md for breakdown |
| **Total** | **381** | Some tests marked `.fixme` for known Quill 2 limitations |

### Test View Pattern

Java test views live in `enhanced-rich-text-editor-it/src/main/java/com/vaadin/componentfactory/`:

| View | Route | Purpose |
|------|-------|---------|
| `ErteShellTestView` | `/erte-test/shell` | Shell basics, Lit lifecycle |
| `ErteTabStopTestView` | `/erte-test/tabstops` | Tabstops, rulers, soft-break |
| `ErteReadonlyTestView` | `/erte-test/readonly` | Readonly sections |
| `ErtePlaceholderTestView` | `/erte-test/placeholders` | Placeholders |
| `ErteToolbarTestView` | `/erte-test/toolbar` | Toolbar slots & visibility |
| `ErteToolbarDialogTestView` | `/erte-test/toolbar-dialog` | Toolbar dialog helper |
| `ErteToolbarPopoverTestView` | `/erte-test/toolbar-popover` | Toolbar popover helper |
| `ErteToolbarSelectPopupTestView` | `/erte-test/toolbar-select-popup` | Toolbar context menu helper |
| `ErteExtendOptionsTestView` | `/erte-test/extend-options` | extendQuill/extendEditor hooks |
| `ErteReplaceIconTestView` | `/erte-test/replace-icons` | Replace toolbar icons |
| `ErteTablesTestView` | `/erte-test/tables` | Table operations |
| `ErteFeatureTestView` | `/erte-test/features` | Misc features (NBSP, i18n, align) |

Each test view provides: `#test-editor`, `#delta-output`, `#html-output`, `#event-log`, `#test-ready`.

### Key Test Helpers

From `enhanced-rich-text-editor-it/tests/erte/helpers.ts`:

| Helper | Purpose |
|--------|---------|
| `waitForEditor(page)` | Wait for editor to be fully initialized |
| `getEditor(page)` | Get editor locator |
| `getTabs(page)`, `getSoftBreaks(page)` | Get embed locators |
| `getDelta(page)` | Get Delta JSON from `#delta-output` |
| `getDeltaFromEditor(page)` | Get Delta directly from Quill |
| `getRuler(page)`, `getRulerMarkers(page)` | Access tabstop ruler |

### Debugging Tips

**Shadow DOM:** Playwright locators pierce shadow DOM, but `page.evaluate()` does not — use `el.shadowRoot.querySelector()`.

**Ready indicator:** `#test-ready` has `display:none` — use `state: 'attached'` (not `visible`).

**Delta timing:** After typing, use `getDelta()` (from `#delta-output`). After server-side updates, use `getDeltaFromEditor()` (reads directly from Quill).

**Server errors:** `bash v25-it-server-logs.sh -errors`

---

## Testing Requirements

Every feature change must include tests.

### Test Views

Create a new test view in `enhanced-rich-text-editor-it/src/main/java/com/vaadin/componentfactory/` following the pattern:

```java
@Route("erte-test/my-feature")
public class ErteMyFeatureTestView extends VerticalLayout {
    public ErteMyFeatureTestView() {
        setSizeFull();
        setPadding(true);

        var editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        // ... configure editor for feature ...

        var deltaOutput = new Pre();
        deltaOutput.setId("delta-output");

        var eventLog = new Div();
        eventLog.setId("event-log");

        // JavaScript to update outputs
        editor.getElement().executeJs(
            "const el = this; "
            + "const out = document.getElementById('delta-output'); "
            + "if (el._editor) el._editor.on('text-change', () => { "
            + "  out.textContent = JSON.stringify(el._editor.getContents()); "
            + "});"
        );

        add(editor, deltaOutput, eventLog);
    }
}
```

**Guidelines:**
- Always provide `#test-editor`, `#delta-output`, and `#event-log` elements
- Use client-side JavaScript to update outputs (not Java listeners)
- Set `#test-ready[data-ready="true"]` when initialization is complete (see existing test views)

### Playwright Tests

Create specs in `enhanced-rich-text-editor-it/tests/erte/` following the pattern:

```typescript
import { test, expect } from '@playwright/test';
import { waitForEditor, getEditor, getDelta, ERTE_TEST_BASE } from './helpers';

const MY_FEATURE_URL = `${ERTE_TEST_BASE}/my-feature`;

test.describe('ERTE My Feature', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(MY_FEATURE_URL);
    await waitForEditor(page);
  });

  test('Feature works as expected', async ({ page }) => {
    const editor = getEditor(page);
    await editor.click();
    await page.keyboard.type('test');

    const delta = await getDelta(page);
    expect(delta.ops).toHaveLength(1);
    expect(delta.ops[0].insert).toBe('test');
  });
});
```

**Guidelines:**
- Use shared helpers from `tests/erte/helpers.ts`
- Wait for editor with `waitForEditor(page)` in `beforeEach`
- Simulate **real user interactions** (`click()`, `type()`, `press()`) — never programmatic shortcuts
- Test both visual appearance and underlying data (delta)

**All tests must pass before submitting a PR.** If a test is deferred or known to fail, mark it with `.skip` and document why in the test comment. See [Pull Request Process](#pull-request-process) step 3 for the exact commands.

---

## Pull Request Process

1. **Create a feature branch** from `v25`:
   ```bash
   git checkout -b feature/my-feature
   ```

2. **Implement the feature** with tests:
   - Add Java code in `enhanced-rich-text-editor-v25/src/main/java/`
   - Add JavaScript in `enhanced-rich-text-editor-v25/src/main/resources/META-INF/resources/`
   - Add test view in `enhanced-rich-text-editor-it/src/main/java/com/vaadin/componentfactory/`
   - Add Playwright spec in `enhanced-rich-text-editor-it/tests/erte/`

3. **Run the full test suite** locally:
   ```bash
   bash v25-build-it.sh
   bash v25-it-server-start.sh
   cd enhanced-rich-text-editor-it && npx playwright test tests/erte/
   bash v25-it-server-stop.sh
   ```

4. **Push and create PR** targeting `v25`:
   ```bash
   git push origin feature/my-feature
   # Create PR on GitHub targeting v25 (not master)
   ```

5. **PR requirements:**
   - ✅ All Playwright tests pass
   - ✅ Code follows style guide (Java license header, English, consistent naming)
   - ✅ Architecture constraints followed (no RTE 2 copies, single foreign class, etc.)
   - ✅ Tests cover the feature (unit + integration)
   - ✅ Documentation updated ([USER_GUIDE.md](../user/USER_GUIDE.md) or [API_REFERENCE.md](../user/API_REFERENCE.md) if applicable)

6. **Code review** — Team reviews your code for style, architecture, and testing

7. **Merge to `v25`** — Never merge to `master`

---

## Useful References

- **[DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)** — Build, test, and demo setup
- **[USER_GUIDE.md](../user/USER_GUIDE.md)** — Feature documentation
- **[API_REFERENCE.md](../user/API_REFERENCE.md)** — Complete Java API
- **[CLAUDE.md](../../CLAUDE.md)** — Architecture decisions, confirmed patterns, Quill/Parchment migration notes
