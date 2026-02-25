# ERTE Test Suite Inventory

> **V25 Migration Status (Phase 5 QA — tables fixme complete):** 296 pass, 10 skip, 0 fail across 306 tests.
> Implemented: Shell (7), Toolbar (26), Readonly (18), Tabstops (67 pass, 14 skip), Placeholders (30 pass, 2 skip), extendOptions (4), Whitespace (7 pass, 2 skip), Sanitizer (11), I18n (2), Replace Icons (10), Features (32), Tables (82 pass, 0 skip).
> Skipped tests document known component bugs and Quill 2/Parchment 3 limitations, not ERTE core bugs.

Total: 381 tests (75 prototype + 306 ERTE including Tables)
V25 status: 296 passed, 10 skipped, 0 failed

---

## Tabstops (78 tests) — `erte/tabstops.spec.ts`

### Hard-Break (Enter)
- Tab in new paragraph aligns to first tabstop
- Hard-Break does NOT copy tabs from previous line

### Soft-Break (Shift+Enter)
- Copies tabs up to cursor position - cursor after 1st of 3 tabs
- Cursor at line start copies no tabs
- Cursor after all tabs copies all tabs
- Multiple soft-breaks: each new line gets same tab count
- Soft-break preserves original line content
- Soft-break in middle of visual line after previous soft-break

### Automatic Wrap
- Tab after auto-wrap uses fixed width (not tabstop)
- Tabs before wrap still align to tabstops

### Combined Scenarios
- Hard-break after soft-break creates new paragraph
- Tabs align correctly after soft-break
- Soft-break after tabs and text preserves text

### Tab Alignment
- First tab aligns to L (left) tabstop
- Second tab aligns to C (center) tabstop
- Third tab aligns to R (right) tabstop
- Fourth tab (beyond tabstops) uses fixed width
- Fifth and sixth tabs also use fixed width

### Overflow Tabs with Soft-Break
- Soft-break with 5 tabs copies only 3 tabs (tabstop limit)
- Soft-break after 4th tab copies only 3 tabs (tabstop limit)
- Multiple soft-breaks with overflow tabs respect tabstop limit

### Ruler and Tabstop Manipulation
- Adding a new tabstop by clicking ruler
- Cycling tabstop alignment: L -> C -> R -> remove
- Removing middle tabstop affects tab widths
- Tab width updates when tabstop alignment changes

### Edge Cases
- Soft-break with only tabs (no text)
- Hard-break immediately after soft-break
- Soft-break at empty line
- Many tabs (10+) with soft-break copies only tabstop count
- Alternating tabs and text with soft-break

### Mixed Break Types
- Soft-break -> Hard-break -> Soft-break sequence
- Narrow viewport: auto-wrap + soft-break interaction
- Delete tab after soft-break
- Copy-paste content with tabs preserves structure

### Undo/Redo
- Multiple undo restores state before soft-break
- Undo removes last typed character
- Undo single tab insertion
- Ruler changes are outside Quill undo history

### Selection Operations
- Select and delete text spanning soft-break
- Select text spanning soft-break and type to replace
- Double-click selects word adjacent to tab

### Backspace/Delete at Boundaries
- Backspace at start of visual line deletes soft-break
- Delete key removes character or element at cursor
- Backspace deletes tab character

### Tab After Soft-Break
- Tab can be inserted on new paragraph
- Additional tab can be inserted on new visual line after soft-break

### Formatted Text with Tabs
- Bold text preserved after soft-break
- Tab between formatted spans works correctly

### Cursor Navigation
- Arrow keys navigate correctly across tabs
- ArrowDown/ArrowUp navigates between lines containing tabs
- ArrowUp on first line jumps to line start
- ArrowDown on last line jumps to line end
- Cursor can be placed after the last tab in a line
- Cursor is visible (non-zero height) at every tab position
- Tab right edges align with ruler markers within 2px
- End key moves to end of visual line
- Shift+Arrow creates selection and delete removes selected content

### Multiple Paragraphs with Soft-Breaks
- Soft-breaks in different paragraphs are independent
- Soft-break does not affect other paragraphs

### Overflow Tab Limit
- Soft-break at overflow tab position copies only tabstop count tabs
- Soft-break after 4th tab (overflow) copies only 3 tabs
- Soft-break with cursor at 2nd tab copies 2 tabs (within tabstop limit)
- Soft-break after removing tabstops respects new limit

### Browser Resize
- Tab widths recalculate after viewport resize

### Focus/Blur
- Tabs remain visible after blur and refocus

### Tab at Tabstop Boundary
- Consecutive tabs have increasing end positions

### Empty Visual Line
- Soft-break on already empty visual line after previous soft-break

### Stress Tests
- Rapid soft-breaks (10x)
- All tabstops removed - tabs use fixed width

### Whitespace Indicators
- Show Whitespace toolbar button is present and not active by default
- Show Whitespace can be toggled and controls the show-whitespace class
- Tab indicator visible when Show Whitespace enabled
- Soft-break indicator visible when Show Whitespace enabled
- Paragraph indicator visible at end of paragraphs
- Disabling Show Whitespace removes indicators
- Indicators visible for all whitespace types simultaneously
- ~~Auto-wrap indicator shown for wrapped tabs~~ *(fixme — auto-wrap indicator disabled)*
- ~~Auto-wrap class removed when tab not on wrapped line~~ *(fixme — auto-wrap indicator disabled)*

### ERTE Integration
- Java setTabStops reflects in UI
- Tabstops bidirectional sync - UI changes reflected
- Screenshot: Tab alignment L/C/R

---

## Placeholders (32 tests) — `erte/placeholders.spec.ts`

- Placeholder button opens dialog
- Combo box populated with configured placeholders
- Combo box filters/searches
- Insert placeholder from dialog (OK)
- Cancel dialog without selection
- Insert multiple placeholders
- Remove placeholder via dialog (Remove button)
- Placeholder displays correct text
- Placeholder tags (start/end)
- Tags change after placeholders exist
- Alt appearance toggle
- Alt appearance pattern matching
- Placeholder format (bold/italic)
- Placeholder altFormat
- Placeholder format with link
- Keyboard insert (Ctrl+P)
- Delete selected placeholder via keyboard
- ~~Copy-paste placeholder~~ *(fixme: embed doesn't survive clipboard roundtrip)*
- Undo placeholder insert
- ~~Undo placeholder remove~~ *(fixme: Quill history can't restore embeds)*
- PlaceholderButtonClickedEvent
- PlaceholderBeforeInsertEvent (confirm)
- PlaceholderBeforeInsertEvent (cancel)
- PlaceholderInsertedEvent
- PlaceholderBeforeRemoveEvent (confirm)
- PlaceholderBeforeRemoveEvent (cancel)
- PlaceholderRemovedEvent
- PlaceholderSelectedEvent
- PlaceholderLeaveEvent
- PlaceholderAppearanceChangedEvent
- Batch insert multiple placeholders
- Delete selection with multiple placeholders

---

## Readonly Sections (18 tests) — `erte/readonly.spec.ts`

- Readonly blot renders as span.ql-readonly with contenteditable false
- Cannot edit inside readonly section
- Cannot delete readonly section with Backspace
- Cannot delete readonly section with Delete key
- Select-all + Delete does not remove readonly sections
- Cut operation on readonly text is prevented
- Can select and copy readonly text without removing it
- Toggle readonly via toolbar button applies and removes format
- Cursor navigation with arrow keys around readonly boundary
- Click inside readonly places cursor at boundary, not inside
- ~~Readonly sections survive undo and redo~~ *(fixme: Quill history removes readonly attributes)*
- Whole-editor readonly mode hides toolbar and prevents editing
- Whole-editor readonly toggle back restores toolbar and editing
- Bold and italic formatting preserved inside readonly sections
- Pasting content does not affect readonly sections
- Load Delta with readonly sections via setValue button
- Readonly sections correctly serialized in delta output
- Screenshot: Readonly section gray background

---

## Toolbar (28 tests) — `erte/toolbar.spec.ts`

- All 25 slots present in DOM
- Component in START slot renders
- Component in END slot renders
- Component in BEFORE_GROUP_HISTORY renders
- Component in AFTER_GROUP_EMPHASIS renders
- Component in GROUP_CUSTOM renders
- addToolbarComponentsAtIndex ordering - multiple components in same slot
- Multiple components in same slot - both exist in GROUP_CUSTOM
- Remove component from slot by reference
- getToolbarComponent by ID - slot button click triggers Java event
- Hide standard toolbar buttons
- Hide ERTE-specific buttons
- Show hidden buttons again
- Group auto-hides when all its buttons are hidden
- Custom keyboard shortcut fires - Shift+F9 applies align center
- Keyboard shortcut focuses toolbar - Shift+F10
- Custom shortcut toggles format - Ctrl+Shift+B toggles bold
- ToolbarSwitch toggle state - click activates
- ToolbarSwitch second click toggles back to inactive
- Custom button click fires Java event - START slot
- Custom group button click fires event
- Toolbar keyboard navigation - arrow keys move focus
- Toolbar survives re-render after i18n change - custom components still exist
- I18n labels updated - German tooltips applied
- All standard toolbar button parts exist
- Screenshot: Toolbar with custom components

---

## extendOptions Hooks (4 tests) — `erte/extend-options.spec.ts`

- extendEditor hook fires and sets flag on editor root
- extendQuill hook registers highlight format
- highlight format survives delta roundtrip
- V24 extendOptions deprecation warning

---

## Features (36 tests) — `erte/features.spec.ts`

### Non-Breaking Space (Shift+Space)
- Shift+Space inserts a non-breaking space
- Multiple consecutive NBSPs are all preserved
- NBSP survives copy-paste round-trip

### NBSP Whitespace Indicators
- NBSP shows middle dot when whitespace indicators enabled
- NBSP indicator disappears when whitespace indicators disabled
- Multiple NBSPs show individual indicators

### addText API
- addText at cursor position inserts text
- addText at position 0 inserts text at start
- addText when readonly documents behavior
- addText when disabled does not modify content

### getTextLength
- getTextLength returns correct character count

### Toolbar Buttons
- Align Justify button applies justify alignment
- Indent button increases list indentation
- Outdent button decreases list indentation

### I18n
- German I18n labels are applied to toolbar buttons
- German I18n labels are applied to ERTE-specific buttons

### No Rulers Mode
- setNoRulers hides the ruler
- setNoRulers(false) re-shows the ruler

### Disabled State
- Disabled state makes editor non-interactive with toolbar disabled
- Disabled vs readonly: toolbar state differs
- Toggle disabled back to enabled restores full functionality

### Value Round-Trip
- Load Delta with tab blot and verify round-trip
- Load Delta with readonly blot and verify it renders
- Load Delta with placeholder blot and verify it renders

### Sanitizer
- Sanitizer preserves ERTE-specific classes in HTML output
- Sanitizer strips XSS payloads from HTML output
- Sanitizer handles data-placeholder attribute in HTML output
- Strips url() from style attribute
- Strips expression() from style attribute
- Preserves safe color style through round-trip
- Preserves safe background-color style through round-trip
- Strips data:text/html from img src
- Preserves data:image/png in img src
- Strips unknown CSS properties
- background: url() shorthand is blocked

### focus() Method
- focus() method gives focus to the editor

---

## Replace Icons (10 tests) — `erte/replace-icons.spec.ts`

Phase 3.3g — `replaceStandardToolbarButtonIcon()` enum-based API.

| # | Test | Status |
|---|------|--------|
| 1 | Pre-configured icons render on load | PASS |
| 2 | Default icons are removed when custom icon present | PASS |
| 3 | Custom icon has correct attributes | PASS |
| 4 | Null icon clears slot and restores default | PASS |
| 5 | Runtime icon replacement works | PASS |
| 6 | Icons survive toolbar re-render (i18n change) | PASS |
| 7 | Multiple icon replacements coexist | PASS |
| 8 | Icon replacement does not affect button functionality | PASS |
| 9 | ERTE-specific button icon replacement (justify) | PASS |
| 10 | Enum API provides compile-time safety | PASS |

**Total:** 10 tests, all passing

---

## Tables (82 tests) — `erte/tables.spec.ts`

Phase 4 — Tables addon for ERTE. Full CRUD operations, cell selection, keyboard navigation, templates, and events.

### Table Structure (Initial State) — 6 tests
- Pre-loaded table has 6 rows and 5 columns
- Cells contain numbered text 1-30
- All cells have table_id, row_id, cell_id attributes
- All cells share same table_id
- Cells in same row share row_id
- Table has colgroup with 5 col elements

### Table Creation — 4 tests
- Insert table via toolbar popover (3x3)
- New table cells have unique IDs
- Table with template class at creation
- Add Table popover fields accept valid values

### Row Operations — 5 tests
- Add row below current cell
- Add row above current cell
- Remove current row
- Row IDs preserved after add
- Content preserved in adjacent rows after removal

### Column Operations — 5 tests
- Add column after current cell
- Add column before current cell
- Remove current column
- Colgroup updates after column add
- Colgroup updates after column remove

### Cell Merge and Split — 6 tests
- Merge 2x1 horizontal
- Merge 1x2 vertical
- Merge 2x2 rectangle
- Split merged cell
- Merged cell content combines sources
- Merge menu disabled without multi-cell selection

### Cell Selection — 8 tests
- Ctrl+Click selects single cell
- Ctrl+Drag selects rectangle
- Selection highlight visible
- Escape clears selection
- Click outside table clears selection
- Ctrl key shows cell cursor
- Click in cell shows focused-cell indicator
- Selection fires TableSelected with cellSelection=true

### Keyboard Navigation — 8 tests
- Tab moves to next cell
- Tab at end of row wraps to next row
- Shift+Tab moves to previous cell
- Shift+Tab at row start wraps to previous row
- Tab at last cell exits table
- Backspace at cell start doesn't cross boundary
- Delete at cell end doesn't cross boundary
- Ctrl+A selects cell text only

### Toolbar Controls — 6 tests
- Add Table button enabled when not in table
- Add Table button disabled when in table
- Modify Table enabled when in table
- Modify Table disabled when not in table
- Style Templates enabled when in table
- Style Templates disabled when not in table

### Template System — 5 tests
- Pre-loaded table has template1 class
- Template CSS applied (background colors)
- Read Templates returns JSON with 3 templates
- Template class in delta td attribute
- Template class preserved in HTML output

### Template Dialog — 4 tests
- Style Templates button opens template dialog
- Template ComboBox lists all templates
- Change template for current table
- Close dialog with close button

### Events — 5 tests
- TableSelected fires on click into table
- TableSelected fires on leaving table
- CellChanged fires with row/col indices
- CellChanged includes old coordinates
- TableSelected reports template class

### Undo/Redo — 5 tests
- Undo add row
- Undo remove table
- Redo after undo
- Undo merge
- Multiple undo steps

### Value Round-Trip — 5 tests
- Delta has 30 td ops for 6x5 table
- Each td op has valid metadata format
- HTML output contains table structure
- Load delta restores table
- Template class survives delta round-trip

### Border Toggle — 2 tests
- Border toggle hides table borders
- Border toggle restores table borders

### Edge Cases — 8 tests
- Type text inside cell
- Bold text inside cell
- Remove table clears table content
- Table survives editor readonly mode
- Table cells not editable in readonly mode
- Delete last row removes entire table
- Single-cell table operations
- Empty cell remains functional

**Total:** 82 tests — 82 pass, 0 fixme, 0 fail

---

## TabStop Prototype (75 tests) — `tab-stop-prototype.spec.ts`

Identical test set to Tabstops above (minus 3 ERTE Integration tests), running against the prototype view at `/tab-stop`. These are the original tests; the tabstops.spec.ts tests are the migrated version targeting the real ERTE component at `/erte-test/tabstops`.
