# ERTE V25 Migration - Phase 0: Use Case Analysis & UI Test Suite

## Context

Before migrating ERTE from Vaadin 24/Quill 1 to Vaadin 25/Quill 2, we need:
1. A **complete feature inventory** (Step 0) cross-referencing every ERTE 1 feature with RTE 2
2. A **comprehensive UI test suite** covering all ERTE-specific features as regression baseline
3. **Migration of the 75 prototype tabstop tests** to work against the real ERTE 1 component

---

## Part 1: Use Case Analysis (Step 0)

### Deliverable
`migration_v25/USE_CASE_ANALYSIS.md`

### Structure per Feature

| Field | Content |
|-------|---------|
| Feature Name | Human-readable name |
| ERTE 1 Behavior | What the user sees/does |
| Key JS Methods | Functions/properties in vcf-enhanced-rich-text-editor.js |
| Key Java API | Public methods on EnhancedRichTextEditor.java |
| Custom Blots | Which blots (TabBlot, SoftBreakBlot, etc.) |
| Events | Java events fired |
| RTE 2 Support | NATIVE / PARTIAL / NONE |
| Migration Path | Extend / Override / Custom / Inherit |
| Dependencies | Which other features this depends on |
| Security Notes | XSS vectors to fix (from SECURITY.md) |

### Complete Feature List (20 features)
1. Tabstops (L/R/M alignment, width calc engine)
2. Rulers (horizontal ruler, click-to-add)
3. Soft-Break (Shift+Enter + tab copying)
4. Readonly Sections (inline contenteditable=false + delete protection)
5. Placeholders (embed + dialog + combo box + events + alt appearance + tags)
6. Non-Breaking Space (Shift+Space)
7. Whitespace Indicators (symbols + legend + toggle)
8. Toolbar Slot System (24 named slots)
9. Toolbar Button Visibility (show/hide individual buttons)
10. Custom Keyboard Shortcuts (addStandardButtonBinding)
11. HTML Sanitization (Jsoup + ERTE class whitelist)
12. I18n (32 translatable strings)
13. Theme Variants (Lumo + custom ERTE styles)
14. Programmatic Text Insertion (addText API)
15. Value Change Mode (inherited from RTE 2 -- NATIVE)
16. extendOptions Hook (pre-Quill-init callback for tables)
17. List Indentation Buttons (indent/outdent)
18. Align Justify (justify button)
19. Replace Toolbar Button Icons (slot-based icon replacement)
20. Arrow Navigation (tab-aware cursor movement)

### Sources
- `feature_comparison.md`, `SPIKE_RESULTS.md`, `SECURITY.md`, `quill_v1_to_v2_api_diff.md`
- V24 source code (JS + Java)

---

## Part 2: UI Test Suite

### Critical Infrastructure Decisions (from QA/UI review)

#### Shadow DOM Handling
ERTE is a Vaadin web component with open shadow DOM. Playwright CSS locators pierce shadow DOM automatically in Chromium. **Always use scoped locators:**
```typescript
const erte = page.locator('#test-editor');
const editor = erte.locator('.ql-editor');
const tabs = erte.locator('.ql-tab');
```

**Important**: `page.evaluate()` does NOT pierce shadow DOM. Use `document.getElementById('test-editor')._editor` for Quill access.

#### `waitForEditor()` Implementation
Must check Quill readiness, not just DOM presence:
```typescript
async function waitForEditor(page, id = 'test-editor') {
  await page.locator(`#${id}`).waitFor({ state: 'visible', timeout: 60000 });
  await page.waitForFunction(
    (id) => { const el = document.getElementById(id); return el?._editor?.root != null; },
    id, { timeout: 60000 }
  );
  await page.locator(`#${id}`).locator('.ql-editor[contenteditable="true"]').waitFor({ timeout: 10000 });
}
```

#### Dialog Testing
Vaadin dialogs render in `document.body`, NOT inside component shadow DOM:
```typescript
const dialog = page.locator('vaadin-confirm-dialog-overlay');  // body-level
const comboDropdown = page.locator('vaadin-combo-box-overlay'); // separate overlay
```

#### Delta Output Freshness
All test views MUST set `editor.setValueChangeMode(ValueChangeMode.EAGER)` or delta output will be stale.

#### Ready Indicator
Each test view adds a `data-ready` attribute after all Java-side init is complete. Tests wait for this before starting.

#### No `waitForTimeout()`
Replace all `page.waitForTimeout(200)` with assertion-based waits:
```typescript
await expect(erte.locator('.ql-tab')).toHaveCount(4, { timeout: 5000 });
```

---

### Test Views (new Java views in demo module)

| View Class | Route | Purpose |
|------------|-------|---------|
| `ErteTabStopTestView` | `/erte-test/tabstops` | Tabstops + rulers + soft-break + whitespace |
| `ErtePlaceholderTestView` | `/erte-test/placeholders` | Placeholders + dialog + events |
| `ErteReadonlyTestView` | `/erte-test/readonly` | Readonly sections + whole-editor readonly |
| `ErteToolbarTestView` | `/erte-test/toolbar` | Custom slots, visibility, shortcuts, icons, helpers |
| `ErteFeatureTestView` | `/erte-test/features` | NBSP, addText, align justify, indent, i18n, sanitizer |

Each view provides:
- Single `EnhancedRichTextEditor` (id: `test-editor`)
- `ValueChangeMode.EAGER`
- Delta output (`<code id="delta-output">`) updated on every text-change (client-side listener, not just Java event)
- HTML output (`<div id="html-output">`)
- Event log (`<div id="event-log">`) with timestamped entries
- Ready indicator (`<div id="test-ready" data-ready="true">`)
- Feature-specific controls

**Placeholder test view special**: Needs toggles for "auto-confirm inserts" and "auto-confirm removes" because `PlaceholderBeforeInsertEvent` uses a cancel/confirm pattern -- the default action is ALWAYS prevented, Java listener must call `event.insert()`.

### Test File Structure
```
enhanced-rich-text-editor-demo/tests/
  tab-stop-prototype.spec.ts        # existing 75 tests (keep, prototype route)
  erte/
    helpers.ts                       # shared helpers
    tabstops.spec.ts                 # ~78 tests (migrated + whitespace + Java API)
    placeholders.spec.ts             # ~32 tests
    readonly.spec.ts                 # ~17 tests
    toolbar.spec.ts                  # ~23 tests
    features.spec.ts                 # ~25 tests
```

---

### Test Plan by Feature

#### 2a. Tabstops (~78 tests, migrated from prototype)
**View**: `ErteTabStopTestView` | **Route**: `/erte-test/tabstops`

**Migration changes from prototype**:
- URL: `/tab-stop` -> `/erte-test/tabstops`
- Scoped locators: `page.locator('#test-editor').locator('.ql-editor')`
- Ruler: Use ERTE ruler selector (determine during implementation)
- Delta output: `#delta-output` (client-side updated)
- Whitespace toggle: Java checkbox in test view
- `waitForTimeout()` -> assertion-based waits
- `Shift+Enter` shorthand: `page.keyboard.press('Shift+Enter')`

**22 categories** (identical to prototype):
Hard-Break (2), Soft-Break (6), Auto Wrap (2), Combined (3), Tab Alignment L/C/R (5), Overflow+Soft-Break (3), Ruler Manipulation (4), Edge Cases (5), Mixed Breaks (4), Stress (2), Undo/Redo (4), Selection (3), Backspace/Delete (3), Tab After Soft-Break (2), Formatted Text (2), Cursor Nav (3), Multiple Paragraphs (2), Overflow Limit (4), Browser Resize (1), Focus/Blur (1), Boundary (1), Empty Line (1), Whitespace Indicators (12)

**Additional tests** (not in prototype):
- `setTabStops()` from Java -- programmatic configuration
- `getTabStops()` returns state after ruler manipulation
- Bidirectional sync: change in UI, read from Java

#### 2b. Placeholders (~32 tests)
**View**: `ErtePlaceholderTestView` | **Route**: `/erte-test/placeholders`

**View setup**: Editor with 3 pre-configured placeholders, auto-confirm toggles, event log.

| # | Test | Validates |
|---|------|-----------|
| 1 | Placeholder button opens dialog | Dialog renders in body |
| 2 | Combo box populated with configured placeholders | setPlaceholders() reflected |
| 3 | Combo box filters/searches placeholder names | Filter UX |
| 4 | Insert placeholder from dialog (OK) | PlaceholderBlot in Delta |
| 5 | Cancel dialog without selection | No change to content |
| 6 | Insert multiple placeholders | Multiple embeds, correct positions |
| 7 | Remove placeholder via dialog (Remove button) | Embed removed |
| 8 | Placeholder displays correct text | Visual text matches config |
| 9 | Placeholder tags (start/end) | Custom wrapping: `{{Name}}` |
| 10 | Tags change after placeholders exist | Re-render with new tags |
| 11 | Alt appearance toggle | Format switches, button state |
| 12 | Alt appearance pattern matching | Regex-based format selection |
| 13 | Placeholder format (bold/italic) | format JsonObject applied |
| 14 | Placeholder altFormat | altFormat applied in alt mode |
| 15 | Placeholder format with link | Wraps in `<a>` tag |
| 16 | Keyboard insert (Ctrl+P / Meta+P on Mac) | Shortcut opens dialog |
| 17 | Type over selected placeholder | Placeholder deleted, char inserted |
| 18 | Copy-paste placeholder | Clipboard round-trip |
| 19 | Undo placeholder insert | History integration |
| 20 | Undo placeholder remove | History integration |
| 21 | PlaceholderButtonClickedEvent | Java event fires with cursor position |
| 22 | PlaceholderBeforeInsertEvent (confirm) | event.insert() inserts |
| 23 | PlaceholderBeforeInsertEvent (cancel) | No insert when not confirmed |
| 24 | PlaceholderInsertedEvent | Java event after insert |
| 25 | PlaceholderBeforeRemoveEvent (confirm) | event.remove() removes |
| 26 | PlaceholderBeforeRemoveEvent (cancel) | No removal, content restored |
| 27 | PlaceholderRemovedEvent | Java event after remove |
| 28 | PlaceholderSelectedEvent | Java event on selection |
| 29 | PlaceholderLeaveEvent | Java event when leaving |
| 30 | PlaceholderAppearanceChangedEvent | event with altAppearance + label |
| 31 | Batch insert multiple placeholders | Event carries all placeholders |
| 32 | Delete selection with multiple placeholders | BeforeRemove carries all |

#### 2c. Readonly Sections (~17 tests)
**View**: `ErteReadonlyTestView` | **Route**: `/erte-test/readonly`

| # | Test | Validates |
|---|------|-----------|
| 1 | Readonly blot renders | span.ql-readonly, contenteditable=false |
| 2 | Cannot edit inside readonly section | Typing has no effect |
| 3 | Cannot delete readonly section (Backspace) | Delta reverted |
| 4 | Cannot delete readonly section (Delete) | Delta reverted |
| 5 | Select-all + Delete prevented | Readonly sections survive |
| 6 | Cut operation on readonly prevented | Content not removed |
| 7 | Can select and copy readonly text | Selection works, copy allowed |
| 8 | Toggle readonly via toolbar button | Format applied/removed |
| 9 | Cursor navigation around readonly | Arrow keys skip or stop at boundary |
| 10 | Click inside readonly | Cursor at boundary, not inside |
| 11 | Readonly survives undo/redo | History preserves readonly |
| 12 | Whole-editor readonly mode | Toolbar hidden, non-editable |
| 13 | Whole-editor readonly toggle back | Toolbar reappears |
| 14 | Readonly + formatted text | Bold/italic preserved inside |
| 15 | Readonly + copy-paste | Pasted content behavior |
| 16 | Load Delta with readonly sections | setValue with readonly blots |
| 17 | Readonly in Delta output | Correct serialization |

#### 2d. Toolbar System (~23 tests)
**View**: `ErteToolbarTestView` | **Route**: `/erte-test/toolbar`

| # | Test | Validates |
|---|------|-----------|
| 1 | All 24 slots present in DOM | Slot elements exist |
| 2 | Add component to START slot | Before all groups |
| 3 | Add component to END slot | After all groups |
| 4 | Add component to BEFORE_GROUP_* | Correct position |
| 5 | Add component to AFTER_GROUP_* | Correct position |
| 6 | Add component to GROUP_CUSTOM | Legacy slot |
| 7 | addToolbarComponentsAtIndex ordering | Index-based position |
| 8 | Multiple components in same slot | All render in order |
| 9 | Remove component from slot by ID | DOM removed |
| 10 | Remove component by reference | DOM removed |
| 11 | getToolbarComponent by ID | Returns correct component |
| 12 | Hide standard toolbar buttons | display:none verified |
| 13 | Hide ERTE-specific buttons (WHITESPACE, READONLY, PLACEHOLDER) | ERTE buttons hidden |
| 14 | Show hidden buttons again | Buttons reappear |
| 15 | Custom keyboard shortcut fires | Key combo triggers toolbar action |
| 16 | Replace standard button icon | New icon visible |
| 17 | ToolbarSwitch toggle state | Active/inactive cycling |
| 18 | ToolbarDialog opens/closes with switch | Sync state |
| 19 | ToolbarPopover opens/closes with switch | Popover at switch position |
| 20 | ToolbarSelectPopup opens/closes | Context menu sync |
| 21 | Custom button click fires Java event | Server event |
| 22 | Toolbar keyboard navigation (arrow keys) | Focus cycling |
| 23 | Toolbar survives re-render (i18n change) | Custom components persist |

#### 2e. Remaining Features (~25 tests)
**View**: `ErteFeatureTestView` | **Route**: `/erte-test/features`

| # | Test | Validates |
|---|------|-----------|
| 1 | Non-breaking space (Shift+Space) | Nbsp blot in Delta |
| 2 | Multiple consecutive NBSPs | All preserved |
| 3 | NBSP + copy-paste round-trip | Preserved |
| 4 | addText at cursor position | Text inserted at cursor |
| 5 | addText at specific position | Text at position N |
| 6 | addText when readonly | Rejected or works (document behavior) |
| 7 | addText when disabled | Rejected |
| 8 | getTextLength | Correct length |
| 9 | Align justify button | Justify format applied |
| 10 | Indent button | List indentation |
| 11 | Outdent button | List outdentation |
| 12 | I18n: German labels | Tooltips updated |
| 13 | No rulers mode (setNoRulers) | Rulers hidden |
| 14 | setNoRulers(false) re-shows rulers | Rulers visible again |
| 15 | Disabled state | Editor non-interactive, toolbar disabled |
| 16 | Disabled vs readonly difference | Disabled prevents copy, readonly allows |
| 17 | Toggle disabled -> enabled | Full functionality restored |
| 18 | Value round-trip: tabs in Delta | setValue/getValue preserves tabs |
| 19 | Value round-trip: readonly in Delta | Preserves readonly blots |
| 20 | Value round-trip: placeholders in Delta | Preserves placeholder blots |
| 21 | TabConverter: old format auto-conversion | Legacy Delta converted |
| 22 | Sanitizer preserves ERTE classes | ql-tab, ql-readonly, ql-placeholder in HTML |
| 23 | Sanitizer strips XSS payloads | Script injection blocked |
| 24 | Sanitizer + data-placeholder attribute | Verify if preserved or stripped |
| 25 | focus() method | Programmatic focus works |

#### 2f. Visual Regression Screenshots (~6 tests, across spec files)
Targeted screenshots for features where DOM assertions are insufficient:

| # | Test | In File |
|---|------|---------|
| 1 | Tab alignment L/C/R visual | tabstops.spec.ts |
| 2 | Whitespace indicators visible (arrows, pilcrow) | tabstops.spec.ts |
| 3 | Ruler with tabstop markers | tabstops.spec.ts |
| 4 | Readonly section gray background | readonly.spec.ts |
| 5 | Placeholder normal vs alt appearance | placeholders.spec.ts |
| 6 | Toolbar with custom components | toolbar.spec.ts |

Config addition for playwright.config.ts:
```typescript
expect: { toHaveScreenshot: { maxDiffPixelRatio: 0.01, animations: 'disabled' } }
```

---

## Implementation Order

### Step 1: Use Case Analysis Document
1. Create `migration_v25/USE_CASE_ANALYSIS.md`
2. Fill in all 20 features from source code + migration docs
3. Cross-reference with feature_comparison.md (extend, don't duplicate)

### Step 2: Test Infrastructure
1. Create `tests/erte/helpers.ts` (waitForEditor, getDelta, getEventLog, scoped locators)
2. Create `ErteTabStopTestView.java` at `/erte-test/tabstops`
3. Update `playwright.config.ts` (add screenshot config, test patterns)
4. Uncomment `@Route("tab-stop")` in TabStopPrototypeView
5. Build & verify existing 75 prototype tests still pass

### Step 3: Migrate Prototype Tests
1. Copy prototype tests -> `tests/erte/tabstops.spec.ts`
2. Rewrite: URL, scoped locators, assertion-based waits, ruler selectors
3. Add tabstop Java API tests + screenshot tests
4. Verify all pass against ERTE 1

### Step 4: Write New Feature Tests (can partially parallelize)
1. `ErtePlaceholderTestView` + `placeholders.spec.ts` (32 tests)
2. `ErteReadonlyTestView` + `readonly.spec.ts` (17 tests)
3. `ErteToolbarTestView` + `toolbar.spec.ts` (23 tests)
4. `ErteFeatureTestView` + `features.spec.ts` (25 tests)

### Step 5: Verification
- Build: `mvn clean package -DskipTests`
- Start: `bash v24-server-start.sh`
- Prototype: `npx playwright test tests/tab-stop-prototype.spec.ts` (75 pass)
- ERTE suite: `npx playwright test tests/erte/` (~178 pass)
- All: `npx playwright test` (all pass)
- Stop: `bash v24-server-stop.sh`

---

## Test Count Summary

| Spec File | Tests | Status |
|-----------|-------|--------|
| tab-stop-prototype.spec.ts | 75 | existing (keep) |
| erte/tabstops.spec.ts | ~78 | migrated + new |
| erte/placeholders.spec.ts | ~32 | new |
| erte/readonly.spec.ts | ~17 | new |
| erte/toolbar.spec.ts | ~23 | new |
| erte/features.spec.ts | ~25 | new |
| Visual screenshots | ~6 | new (across files) |
| **Total** | **~256** | |

---

## Files to Create/Modify

### New Files
- `migration_v25/USE_CASE_ANALYSIS.md`
- `enhanced-rich-text-editor-demo/src/main/java/.../test/ErteTabStopTestView.java`
- `enhanced-rich-text-editor-demo/src/main/java/.../test/ErtePlaceholderTestView.java`
- `enhanced-rich-text-editor-demo/src/main/java/.../test/ErteReadonlyTestView.java`
- `enhanced-rich-text-editor-demo/src/main/java/.../test/ErteToolbarTestView.java`
- `enhanced-rich-text-editor-demo/src/main/java/.../test/ErteFeatureTestView.java`
- `enhanced-rich-text-editor-demo/tests/erte/helpers.ts`
- `enhanced-rich-text-editor-demo/tests/erte/tabstops.spec.ts`
- `enhanced-rich-text-editor-demo/tests/erte/placeholders.spec.ts`
- `enhanced-rich-text-editor-demo/tests/erte/readonly.spec.ts`
- `enhanced-rich-text-editor-demo/tests/erte/toolbar.spec.ts`
- `enhanced-rich-text-editor-demo/tests/erte/features.spec.ts`

### Modify
- `TabStopPrototypeView.java` (uncomment @Route)
- `playwright.config.ts` (screenshot config)
