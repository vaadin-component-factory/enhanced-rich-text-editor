# Enhanced Rich Text Editor -- Contributing Guide

This guide explains how to contribute to the Enhanced Rich Text Editor (ERTE) project. It covers the git workflow, code style, architecture constraints, and testing requirements.

**Before contributing:** Read [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for setup and build instructions.

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
   cd enhanced-rich-text-editor-demo
   npx playwright test tests/erte/
   ```

See [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for detailed setup steps.

---

## Git Workflow

ERTE uses a simple branching model:

### Branch Strategy

| Branch | Purpose | Merge Target |
|--------|---------|--------------|
| `v25` | Active development (V25 migration) | (main working branch) |
| `feature/*` | New features | `v25` |
| `fix/*` | Bug fixes | `v25` |
| `master` | V24 production (ARCHIVED — do not use) | (closed) |

### Golden Rule

**NEVER merge to `master`.** The `master` branch tracks V24 production and is archived. All development happens on `v25` and feature/fix branches.

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

These are **hard requirements** enforced in code review. Violating them breaks updatability or introduces security issues.

### 1. Never Copy RTE 2 Source Code

ERTE extends Vaadin's RTE 2 at runtime. Do NOT copy RTE 2 classes into ERTE.

```java
// WRONG: copying RTE 2 code
public class ErteRichTextEditor extends RichTextEditor {
    // ... copy of RTE 2's toolbar HTML here ...
}

// CORRECT: extend via inheritance and override methods
public class EnhancedRichTextEditor extends RteExtensionBase {
    @Override
    protected void runBeforeClientResponse(ExecutableFunction function) {
        // Extend, don't copy
    }
}
```

**Why:** Copied code breaks with RTE updates. Inheritance allows updates to flow automatically.

### 2. Single Foreign Package Class

Only ONE class may live in a foreign package (Vaadin's RTE 2 package). This is `RteExtensionBase`:

```java
// CORRECT: single bridge class
package com.vaadin.flow.component.richtexteditor;
public class RteExtensionBase extends RichTextEditor { }

// CORRECT: all ERTE logic here
package com.vaadin.componentfactory;
public class EnhancedRichTextEditor extends RteExtensionBase { }

// WRONG: don't put multiple ERTE classes in the foreign package
package com.vaadin.flow.component.richtexteditor;
public class ErteHelper { } // Don't do this!
```

**Why:** Minimizes tight coupling to Vaadin internals.

### 3. Global Quill Registration

Custom blots must be registered globally BEFORE element creation:

```javascript
// CORRECT: register before instantiation
Quill.register(TabBlot);
Quill.register(PlaceholderBlot);

// Then create editor
const editor = new Quill(...);

// WRONG: registering after creation
const editor = new Quill(...);
Quill.register(TabBlot); // Won't work!
```

**Why:** Quill caches blot registrations at editor init time.

### 4. DOM Manipulation Rules

- **DO use:** `createElement()`, `appendChild()`, `removeChild()`
- **DO NOT use:** `innerHTML` with dynamic content (XSS risk)
- **DO NOT use:** Direct `domNode` property access (use `Quill.find()`)

```javascript
// CORRECT
const span = document.createElement('span');
span.textContent = userInput; // Safe: textContent auto-escapes
span.classList.add('ql-placeholder');
container.appendChild(span);

// WRONG: XSS vulnerability
const span = document.createElement('span');
span.innerHTML = userInput; // Dangerous!
container.appendChild(span);
```

### 5. Keyboard Key Names (No Numeric Codes)

Use string key names (`'Tab'`, `'Enter'`, etc.), never numeric keyCodes:

```javascript
// CORRECT
const binding = {
  key: 'Tab',
  handler: () => insertTab(),
};

// WRONG: numeric code creates binding under wrong key
const binding = {
  key: 9, // Silent failure — Tab bindings ignored!
  handler: () => insertTab(),
};
```

### 6. Parchment 3 API (No Parchment.create)

In Quill 2, use `scroll.create()` instead of `Parchment.create()` (removed in Parchment 3):

```javascript
// CORRECT: Quill 2 / Parchment 3
const node = this.scroll.create('blot-name', value);

// WRONG: Parchment 2 API (removed in Parchment 3)
const node = Parchment.create('blot-name', value);
```

### 7. Java Value Sync Timing

Never set editor content in `ready()` — it gets overwritten by Java value sync:

```java
// WRONG: content gets overwritten
@Override
protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    setValue("<p>Content</p>"); // Overwritten by Java sync!
}

// CORRECT: set value via normal component binding
@Override
protected void setPresentationValue(String value) {
    super.setPresentationValue(value);
    // Value is already set, safe to react to it
}
```

### 8. Embed contenteditable (Quill 2 Guard Nodes)

Do NOT set `contenteditable="false"` on Embed blot outer `domNode`. Quill 2 places guard nodes (zero-width text) inside, which must remain editable:

```javascript
// WRONG: breaks cursor placement adjacent to embed
class MyBlot extends Quill.import('blots/embed') {
  static create() {
    const node = super.create();
    node.contentEditable = 'false'; // Breaks guard nodes!
    return node;
  }
}

// CORRECT: inner contentNode already has contenteditable="false"
class MyBlot extends Quill.import('blots/embed') {
  static create() {
    const node = super.create();
    // Don't set contenteditable on outer node
    const content = document.createElement('span');
    content.contentEditable = 'false'; // Safe on inner element
    node.appendChild(content);
    return node;
  }
}
```

---

## Testing Requirements

Every feature change must include tests.

### Test Views

Create a new test view in `enhanced-rich-text-editor-demo/src/main/java/com/vaadin/componentfactory/` following the pattern:

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

Create specs in `enhanced-rich-text-editor-demo/tests/erte/` following the pattern:

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

### Running Tests Before PR

```bash
# Build
bash v25-build.sh

# Start server
bash v25-server-start.sh

# Run tests
cd enhanced-rich-text-editor-demo
npx playwright test tests/erte/

# Stop server
bash v25-server-stop.sh
```

**All tests must pass before submitting a PR.** If a test is deferred or known to fail, mark it with `.skip` and document why in the test comment.

---

## Pull Request Process

1. **Create a feature branch** from `v25`:
   ```bash
   git checkout -b feature/my-feature
   ```

2. **Implement the feature** with tests:
   - Add Java code in `enhanced-rich-text-editor-v25/src/main/java/`
   - Add JavaScript in `enhanced-rich-text-editor-v25/src/main/resources/META-INF/resources/`
   - Add test view in `enhanced-rich-text-editor-demo/src/main/java/com/vaadin/componentfactory/`
   - Add Playwright spec in `enhanced-rich-text-editor-demo/tests/erte/`

3. **Run the full test suite** locally:
   ```bash
   bash v25-build.sh
   bash v25-server-start.sh
   cd enhanced-rich-text-editor-demo && npx playwright test tests/erte/
   bash v25-server-stop.sh
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
   - ✅ Documentation updated ([USER_GUIDE.md](USER_GUIDE.md) or [API_REFERENCE.md](API_REFERENCE.md) if applicable)

6. **Code review** — Team reviews your code for style, architecture, and testing

7. **Merge to `v25`** — Never merge to `master`

---

## Useful References

- **[DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)** — Build, test, and demo setup
- **[USER_GUIDE.md](USER_GUIDE.md)** — Feature documentation
- **[API_REFERENCE.md](API_REFERENCE.md)** — Complete Java API
- **[CLAUDE.md](../CLAUDE.md)** — Architecture decisions, confirmed patterns, Quill/Parchment migration notes
