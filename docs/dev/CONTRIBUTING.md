# Contributing to ERTE

Contributions require: [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for setup, [User Guide](../../enhanced-rich-text-editor-v25/docs/USER_GUIDE.md) for end-user features.

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

```bash
git clone https://github.com/your-org/enhanced-rich-text-editor.git
cd enhanced-rich-text-editor
bash v25-build.sh
bash v25-server-start.sh
```

Verify with: `bash v25-build-it.sh && bash v25-it-server-start.sh && cd enhanced-rich-text-editor-it && npx playwright test tests/erte/`

See [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for details.

---

## Git Workflow

All development on `v25` branch. Create `feature/*` and `fix/*` branches from `v25`, PR back to `v25`. **Never merge to `master`.**

### Commit Messages

Use clear, concise English messages:

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

Hard requirements. Violating breaks updatability or introduces security issues.

1. **Never copy RTE 2 source** — Extend via inheritance, not copying
2. **Keep ERTE logic in `com.vaadin.componentfactory`** — All classes in one package
3. **Never set content in `ready()`** — It gets overwritten by Java value sync. Use `setPresentationValue()`
4. **Embed guard nodes (Quill 2)** — Never set `contenteditable="false"` on outer domNode. Guard nodes inside must stay editable. See [EXTENDING.md](EXTENDING.md#embed-blot-gotchas)

See [CLAUDE.md](../../CLAUDE.md) for confirmed patterns.

---

## Test Architecture

Java test views (backend) + Playwright specs (tests).

### Test Commands

From `enhanced-rich-text-editor-it/`:

| Command | Purpose |
|---------|---------|
| `npx playwright test tests/erte/` | Run ERTE tests |
| `npx playwright test tests/erte/tabstops.spec.ts` | Run one spec |
| `npx playwright test --ui` | Interactive debug |

### Baseline

306 ERTE tests + 75 prototype tests = 381 total. See TEST_INVENTORY.md.

### Test Views

Java test views in `enhanced-rich-text-editor-it/src/main/java/com/vaadin/componentfactory/`. Each provides: `#test-editor`, `#delta-output`, `#html-output`, `#event-log`, `#test-ready`.

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

- **Shadow DOM:** Playwright pierces it, but `page.evaluate()` doesn't — use `el.shadowRoot.querySelector()`
- **Ready indicator:** `#test-ready` has `display:none` — use `state: 'attached'`
- **Delta:** Use `getDelta()` from element, or `getDeltaFromEditor()` from Quill
- **Server errors:** `bash v25-it-server-logs.sh -errors`

---

## Testing Requirements

Every feature needs tests. Create test view + Playwright spec.

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

Requirements: real user interactions (`click()`, `type()`, `press()`), not programmatic shortcuts. All tests must pass before PR.

---

## Pull Request Process

1. Create branch from `v25`: `git checkout -b feature/my-feature`
2. Implement feature + tests (Java, JS, test view, Playwright spec)
3. Run full suite: `bash v25-build-it.sh && bash v25-it-server-start.sh && cd enhanced-rich-text-editor-it && npx playwright test tests/erte/`
4. Push and PR to `v25` (never `master`)

**PR Checklist:**
- ✅ All tests pass
- ✅ Code style (Java license header, English, consistent naming)
- ✅ Architecture constraints (no RTE 2 copies, single foreign class)
- ✅ Tests cover feature
- ✅ Documentation updated ([USER_GUIDE.md](../../enhanced-rich-text-editor-v25/docs/USER_GUIDE.md) or [API_REFERENCE.md](../../enhanced-rich-text-editor-v25/docs/API_REFERENCE.md))

---

**See also:** [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md), [USER_GUIDE.md](../../enhanced-rich-text-editor-v25/docs/USER_GUIDE.md), [API_REFERENCE.md](../../enhanced-rich-text-editor-v25/docs/API_REFERENCE.md), [CLAUDE.md](../../CLAUDE.md)
