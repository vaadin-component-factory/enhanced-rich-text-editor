# Contributing to ERTE

Welcome, and thanks for contributing! This guide covers the workflow, code style, and testing expectations. Before diving in, make sure your dev environment is ready — [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) has the setup steps.

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

Clone, build, and run the demo to see ERTE in action:

```bash
git clone https://github.com/your-org/enhanced-rich-text-editor.git
cd enhanced-rich-text-editor
bash build.sh
bash server-start.sh
```

To make sure everything works, run the test suite:

```bash
bash build-it.sh && bash it-server-start.sh
cd enhanced-rich-text-editor-it && npx playwright test tests/erte/
```

See [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for more details on the build and test workflow.

---

## Git Workflow

All development happens on the `v25` branch. Create `feature/*` and `fix/*` branches from `v25`, and PR back to `v25`. **Never merge to `master`** — that's the V24 production branch.

### Commit Messages

Keep them clear and concise, in English:

```
Add tabstop blot for Parchment 3

- Implement Embed blot with Quill 2 lifecycle
- Add CSS for tab alignment
- Add tests for tab positioning

Fixes #123
```

**Format:** One-line title (50 chars, imperative), detailed body (72-char wrap), link issues with `Fixes #123` or `Relates to #456`.

---

## Code Style

### Java Code

- **Language:** English (identifiers, comments, docs)
- **Format:** Standard Java conventions (4-space indentation)
- **License header:** CVALv3 required on all Java files:

**License header:**

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


### JavaScript Code

- **Language:** English (identifiers, comments)
- **Framework:** ES2020+ class syntax (Lit patterns)
- **DOM:** Use `createElement()` / `appendChild()`, never `innerHTML` with dynamic content
- **Comments:** Add for complex logic (Quill integration, sanitization)

### TypeScript Test Code

- **Language:** English
- **Format:** 2-space indentation (Playwright)
- **Structure:** `test.describe()` blocks, `expect()` assertions

---

## Architecture Constraints

These are hard rules — violating them compromises updatability or introduces security issues:

1. **Never copy RTE 2 source** — ERTE extends at runtime via inheritance. This is what keeps us updatable when Vaadin ships new RTE versions.
2. **Keep ERTE logic in `com.vaadin.componentfactory`** — Only `RteExtensionBase` lives in the foreign package (it's a minimal bridge). Everything else stays together.
3. **Never set content in `ready()`** — Java value sync overwrites it. Always use `setPresentationValue()`.
4. **Embed guard nodes (Quill 2)** — Never set `contenteditable="false"` on the outer domNode — Quill 2's invisible guard nodes inside must stay editable for cursor placement. See [EXTENDING.md](EXTENDING.md#embed-blot-patterns) for details.

---

## Test Architecture

Every feature has two parts: a Java test view (the backend setup) and a Playwright spec (the actual tests). The test views live in the IT module and provide a controlled environment for each feature.

### Test Commands

Run from within `enhanced-rich-text-editor-it/`:

| Command | Purpose |
|---------|---------|
| `npx playwright test tests/erte/` | Run ERTE tests |
| `npx playwright test tests/erte/tabstops.spec.ts` | Run one spec |
| `npx playwright test --ui` | Interactive debug |

### Baseline

See TEST_INVENTORY.md for current test counts.

### Test Views

Each test view follows a consistent pattern: it provides `#test-editor`, `#delta-output`, `#html-output`, `#event-log`, and a `#test-ready` indicator. This makes it easy to write tests that work the same way across all features.

| View | Route | Purpose |
|------|-------|---------|
| `ErteShellTestView` | `/erte-test/shell` | Shell basics, Lit lifecycle |
| `ErteTabStopTestView` | `/erte-test/tabstops` | Tabstops, rulers, soft-break |
| `ErteReadonlyTestView` | `/erte-test/readonly` | Readonly sections |
| `ErtePlaceholderTestView` | `/erte-test/placeholders` | Placeholders |
| `ErteToolbarTestView` | `/erte-test/toolbar` | Toolbar slots, visibility |
| `ErteFeatureTestView` | `/erte-test/features` | NBSP, i18n, align |
| `ErteTablesTestView` | `/erte-test/tables` | Tables |

### Test Helpers (from `helpers.ts`)

| Helper | Purpose |
|--------|---------|
| `waitForEditor(page)` | Wait for editor init |
| `getEditor(page)` | Get editor locator |
| `getDelta(page)`, `getDeltaFromEditor(page)` | Get Delta |
| `getTabs(page)`, `getSoftBreaks(page)` | Get embeds |
| `getRuler(page)`, `getRulerMarkers(page)` | Access ruler |

### Debugging Tips

Common pitfalls:

- **Shadow DOM:** Playwright locators pierce it automatically, but `page.evaluate()` does NOT — you'll need `el.shadowRoot.querySelector()` inside evaluate calls
- **Ready indicator:** `#test-ready` has `display:none` — use `state: 'attached'`, not the default `visible`
- **Delta:** Use `getDelta()` to read from the output element, or `getDeltaFromEditor()` to read directly from the Quill instance
- **Server errors:** `bash it-server-logs.sh -errors` shows you just the errors without scrolling through the full log

---

## Testing Requirements

Every feature needs tests — no exceptions. The pattern: create a test view (Java) and a Playwright spec (TypeScript).

### Test View Example

```java
@Route("erte-test/my-feature")
public class ErteMyFeatureTestView extends VerticalLayout {
    public ErteMyFeatureTestView() {
        var editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        var deltaOutput = new Pre();
        deltaOutput.setId("delta-output");
        var eventLog = new Div();
        eventLog.setId("event-log");
        // ... setup ...
        add(editor, deltaOutput, eventLog);
    }
}
```

Requirements: `#test-editor`, `#delta-output`, `#event-log`, client-side JS for outputs, `#test-ready` flag.

### Playwright Test Example

```typescript
test.describe('ERTE My Feature', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(MY_FEATURE_URL);
    await waitForEditor(page);
  });

  test('works', async ({ page }) => {
    await getEditor(page).click();
    await page.keyboard.type('test');
    const delta = await getDelta(page);
    expect(delta.ops[0].insert).toBe('test');
  });
});
```

**Important:** Always use real user interactions in tests — `click()`, `type()`, `press()` — never programmatic shortcuts like `setSelection()` or `element.value = ...`. Programmatic APIs bypass the browser's event chain and don't reflect what actual users experience. All tests must pass before merging.

---

## Pull Request Process

1. Create a branch from `v25`: `git checkout -b feature/my-feature`
2. Implement your feature with tests (Java + JS + test view + Playwright spec)
3. Run the full test suite to make sure nothing broke
4. Push and open a PR to `v25` (never `master`)

**Before submitting, double-check:**
- All tests pass (not just your new ones — the whole suite)
- Code style is consistent (Java license headers, English identifiers, standard formatting)
- Architecture constraints are respected (no RTE 2 source copies, minimal foreign-package classes)
- Your feature is covered by tests
- Documentation is updated if the feature is user-facing ([USER_GUIDE.md](../BASE_USER_GUIDE.md))

---

**See also:** [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for build and run instructions, [EXTENDING.md](EXTENDING.md) for adding custom blots and toolbar components, [User Guide](../BASE_USER_GUIDE.md) for the full feature reference.
