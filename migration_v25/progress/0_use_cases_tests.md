# ERTE V25 Migration — Progress

## Phase 0: Use Case Analysis & UI Test Suite — COMPLETE

### Deliverables
- `migration_v25/USE_CASE_ANALYSIS.md` — 20 features analyzed with migration paths
- 5 Java test views (`ErteTabStopTestView`, `ErtePlaceholderTestView`, `ErteReadonlyTestView`, `ErteToolbarTestView`, `ErteFeatureTestView`)
- 5 Playwright spec files in `tests/erte/` with shared `helpers.ts`
- `tests/TEST_INVENTORY.md` — full test listing

### Bug Fixes Applied (V24 component)

#### NBSP (Shift+Space) — FIXED
- **Root cause:** Nbsp blot was `Inline` without `className` → Quill threw TypeError on format lookup
- **Fix 1 (blots.js):** Changed `Nbsp` from `Inline` to `Embed`, added `Nbsp.className = 'ql-nbsp'`
- **Fix 2 (editor.js):** Shift+Space keyboard binding was appended via `addBinding()` — Quill's default space handler fired first. Changed to prepend at start of `keyboard.bindings[32]` array. Used `function()` (not arrow) so Quill 1 binds `this.quill` correctly.

#### Readonly Protection (Backspace/Delete/Select-all) — FIXED
- **Root cause:** Readonly detection in `text-change` handler checked `v.insert.readonly` (embed format), but readonly is an attribute format `v.attributes.readonly`
- **Fix (editor.js):** Changed to `v.attributes && v.attributes.readonly`

#### Placeholder Dialog — FIXED
- **Root cause 1:** `@EventData("event.preventDefault()")` in `PlaceholderButtonClickedEvent` Java class cancels the DOM event. When a `PlaceholderButtonClickedListener` is registered, the JS dialog never opens.
- **Fix (ErtePlaceholderTestView.java):** Added `editor.getElement().executeJs("this._placeholderEditing = true")` after logging to re-open the dialog.
- **Root cause 2:** `getPlaceholderBlots` locator `#test-editor .ql-placeholder` matched a template element in the Shadow DOM outside `.ql-editor`, causing false count=2.
- **Fix (placeholders.spec.ts):** Changed locator to `.ql-editor .ql-placeholder` to only match blots inside the editor content area.
- **Note:** Standard demo view works correctly — it has no `PlaceholderButtonClickedListener`, so `preventDefault()` never fires.

#### Show Whitespace — ADAPTED
- **Change:** Removed `#whitespace-toggle` Checkbox from `ErteTabStopTestView.java` — tests now use the built-in toolbar button `[part~="toolbar-button-whitespace"]`
- **Fix (helpers.ts):** `enableShowWhitespace`/`disableShowWhitespace` click toolbar button instead of checkbox. New `isShowWhitespaceActive` helper checks `ql-active` class.
- **Fix (tabstops.spec.ts):** "Show Whitespace checkbox" test → "Show Whitespace toolbar button". 3 prototype-only "Tab Debug" tests removed (feature doesn't exist in ERTE).

### Test Results (current)
| Suite | Passed | Failed | Skipped | Total |
|-------|--------|--------|---------|-------|
| tabstops.spec.ts | 73 | 0 | 1 | 75 |
| placeholders.spec.ts | 21 | 11 | 0 | 32 |
| readonly.spec.ts | 17 | 0 | 1 | 18 |
| toolbar.spec.ts | 24 | 0 | 0 | 24 |
| features.spec.ts | 25 | 0 | 0 | 25 |
| **ERTE Total** | **160** | **11** | **2** | **174** |
| tab-stop-prototype.spec.ts | 74 | 0 | 1 (flaky) | 75 |

**Changes from previous:** +2 passed (whitespace button test + previously-skipped whitespace checkbox test now uses toolbar), -3 tests removed (Tab Debug prototype-only), -3 skipped (2 Tab Debug removed, 1 whitespace checkbox converted to toolbar button test).

### Remaining Failures (11 placeholder tests)
| Test | Error | Likely cause |
|------|-------|--------------|
| 3 - Combo box filters | toHaveCount | Locator or timing |
| 5 - Cancel dialog | Timeout 1.5m | Dialog doesn't close after cancel? |
| 12 - Alt appearance pattern | toContainText | Assertion/format mismatch |
| 13 - Placeholder format (bold/italic) | toContainText | Assertion/format mismatch |
| 14 - Placeholder altFormat | toContainText | Assertion/format mismatch |
| 17 - Type over selected | toHaveCount | Placeholder replace timing |
| 18 - Copy-paste placeholder | toHaveCount | Clipboard/paste behavior |
| 20 - Undo placeholder remove | toContainText | Undo state mismatch |
| 21 - PlaceholderButtonClickedEvent | Timeout 1.5m | Event flow / cancel issue |
| 28 - PlaceholderSelectedEvent | toHaveCount | Selection event timing |
| 31 - Batch insert multiple | toContainText | Multi-insert flow |

### Skipped Tests (2)
| Tests | Reason | Fixable? |
|-------|--------|----------|
| 1 TabConverter | Needs old-format delta test setup | Yes, test view change |
| 1 Readonly (Undo) | Quill history removes readonly attributes | Medium effort |

## Phase 1-4: Not started
