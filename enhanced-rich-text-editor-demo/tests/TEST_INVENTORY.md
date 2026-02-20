# ERTE Test Suite Inventory

> **V25 Migration Status (Phase 3.3d complete):** 185 tests passing, 11 fixme.
> Implemented: Shell (6), Toolbar (27), Readonly (18), Tabstops (67 pass, 14 fixme), Placeholders (30 pass, 2 fixme), extendOptions (4), Whitespace (7 pass, 2 fixme), Sanitizer (11), I18n (2).
> Features.spec.ts: 28 pass, 5 fail (addText/getTextLength=3.3e, align justify=3.3f, focus=3.3e).

Total: 268 tests (75 prototype + 193 ERTE)
V25 status: 185 passed, 11 skipped across all feature specs

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

## Toolbar (29 tests) — `erte/toolbar.spec.ts`

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
- ~~Replace standard button icon~~ *(fixme: Phase 3.3g)*
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

## Features (33 tests) — `erte/features.spec.ts`

### Non-Breaking Space (Shift+Space)
- Shift+Space inserts a non-breaking space
- Multiple consecutive NBSPs are all preserved
- NBSP survives copy-paste round-trip

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

## TabStop Prototype (75 tests) — `tab-stop-prototype.spec.ts`

Identical test set to Tabstops above (minus 3 ERTE Integration tests), running against the prototype view at `/tab-stop`. These are the original tests; the tabstops.spec.ts tests are the migrated version targeting the real ERTE component at `/erte-test/tabstops`.
