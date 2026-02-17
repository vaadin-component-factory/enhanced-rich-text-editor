# ERTE 1 vs RTE 2 (Vaadin 25) Feature Comparison Matrix

## Context

- **ERTE 1**: Enhanced Rich Text Editor, built on Quill 1.3.6, Polymer web component, Vaadin 24.x
- **RTE 2**: Vaadin 25 built-in Rich Text Editor (`<vaadin-rich-text-editor>`), uses **Quill 2.0.3** (vendored at `packages/rich-text-editor/vendor/vaadin-quill.js`). Lit-based web component (migrated from Polymer).

## Status Legend

| Symbol | Meaning |
|--------|---------|
| NATIVE | Available natively in RTE 2 / Quill |
| PARTIAL | Partially available, needs extension |
| CUSTOM | Must be fully custom-migrated |
| NEW-IN-RTE2 | New in RTE 2, not in ERTE 1 |
| DROP? | Candidate for removal (discuss with stakeholders) |

---

## Comparison Matrix

### 1. Tab Stops with Alignment (LEFT / RIGHT / MIDDLE)

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | Custom `TabBlot` (Embed), iterative width calculation engine (~200 lines), `_updateTabWidths()` with alignment logic, `TabStop.java` with `Direction` enum (LEFT/RIGHT/MIDDLE), `tabStops` property on web component, `_tabStopsArray` for runtime lookup |
| **RTE 2 (Vaadin 25)** | Not available. No tab stop concept at all. |
| **Quill 1/2** | Neither Quill 1 nor Quill 2 has native tab stop support. |
| **Migration status** | **CUSTOM** -- Full migration required. Must register TabBlot as custom Embed, port the iterative width calculation engine, and wire up the tabStops property. The Parchment API in Quill 2 (when eventually upgraded) will change Embed registration syntax. |
| **Effort** | HIGH -- This is the most complex ERTE feature (~400 lines JS + Java API). |
| **Files** | `vcf-enhanced-rich-text-editor.js` (lines 777-999, tab engine), `vcf-enhanced-rich-text-editor-blots.js` (TabBlot), `TabStop.java` |

### 2. Rulers (Horizontal + Vertical)

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | Base64-encoded PNG ruler images rendered in dedicated `<div>` elements, `noRulers` property to toggle, click handler `_addTabStop` on horizontal ruler for adding tab stops, `part="horizontalRuler"` and `part="verticalRuler"` for styling |
| **RTE 2 (Vaadin 25)** | Not available. No ruler concept. |
| **Quill 1/2** | Not a Quill feature -- this is pure ERTE DOM/CSS. |
| **Migration status** | **CUSTOM** -- Must inject ruler DOM into RTE 2's shadow DOM at runtime (or via a wrapper element). The click-to-add-tabstop interaction depends on the tab stop system. |
| **Effort** | MEDIUM -- Ruler rendering is straightforward; the challenge is runtime injection into RTE 2's shadow DOM without forking. |
| **Files** | `vcf-enhanced-rich-text-editor.js` (template lines 436-443, ruler display logic) |

### 3. Soft-Break (Shift+Enter with Tab Copying)

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | Custom `SoftBreakBlot` (Embed with `<br>`), Shift+Enter keyboard binding that: (a) finds visual line boundaries, (b) counts tabs before cursor, (c) inserts soft-break embed, (d) copies tabs after soft-break (limited to number of defined tabstops). ~60 lines of handler logic. |
| **RTE 2 (Vaadin 25)** | Not available. Standard Enter creates new paragraph; no soft-break concept. |
| **Quill 1/2** | Quill 2 does not have native soft-break. Standard Quill has no visual-line-break-within-paragraph concept. |
| **Migration status** | **CUSTOM** -- Full migration required. SoftBreakBlot registration + keyboard binding + tab-copying logic. Tightly coupled with the tab stop system. |
| **Effort** | MEDIUM -- Depends on tab stops being implemented first. |
| **Files** | `vcf-enhanced-rich-text-editor-blots.js` (SoftBreakBlot), `vcf-enhanced-rich-text-editor.js` (lines 1710-1772, soft-break binding) |

### 4. Read-Only Sections (Inline contenteditable=false)

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | `ReadOnlyBlot` (Inline blot, `<span class="ql-readonly" contenteditable="false">`), toolbar toggle button, delete protection (reverts delta if readonly blot count changes), styled with gray background |
| **RTE 2 (Vaadin 25)** | **Not available.** RTE 2 has whole-editor `readonly` mode only (hides toolbar, makes entire content non-editable). No inline read-only sections. |
| **Quill 1/2** | Not a native Quill feature. Quill 2 has no built-in inline contenteditable=false support. |
| **Migration status** | **CUSTOM** -- Must register ReadOnlyBlot, add toolbar button, implement delete protection logic. The `contenteditable=false` approach works at DOM level and is framework-independent. |
| **Effort** | MEDIUM -- Blot registration + toolbar button + delete protection handler. |
| **Files** | `vcf-enhanced-rich-text-editor-blots.js` (ReadOnlyBlot), `vcf-enhanced-rich-text-editor.js` (delete protection in text-change handler, readonly button) |

### 5. Placeholders (Embed with Formatting)

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | `PlaceholderBlot` (Embed, complex), stores value as JSON in `data-placeholder`, supports alt appearance with regex pattern, inline formatting (`bold`, `italic`, `font`, `link`, `script`), tags wrapping, combo box dialog for insertion, Ctrl+P shortcut, alt appearance toggle button, complete placeholder lifecycle (insert/remove/undo). Java: `setPlaceholders()`, `getPlaceholders()`, `setPlaceholderAltAppearancePattern()`, `Placeholder.java` class. |
| **RTE 2 (Vaadin 25)** | Not available. No placeholder concept. |
| **Quill 1/2** | Not a native feature. |
| **Migration status** | **CUSTOM** -- Very complex feature. Full migration of PlaceholderBlot + dialog + combo box + appearance toggle + keyboard shortcuts + undo/redo integration. This is arguably the second most complex ERTE feature after tabs. |
| **Effort** | HIGH -- Many moving parts: blot, dialog UI, toolbar buttons, keyboard bindings, appearance modes. |
| **Files** | `vcf-enhanced-rich-text-editor-blots.js` (PlaceholderBlot, ~120 lines), `vcf-enhanced-rich-text-editor.js` (placeholder dialog, handlers, appearance logic), `Placeholder.java`, `EnhancedRichTextEditor.java` (placeholder API) |

### 6. Non-Breaking Space (Shift+Space)

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | `Nbsp` Inline blot (`<span>&nbsp;</span>`), keyboard binding `Shift+Space` inserts nbsp embed |
| **RTE 2 (Vaadin 25)** | Not available. No nbsp insertion feature. |
| **Quill 1/2** | Not a native feature. |
| **Migration status** | **CUSTOM** -- Simple migration. Register Nbsp blot + add keyboard binding. |
| **Effort** | LOW -- ~15 lines of code total. |
| **Files** | `vcf-enhanced-rich-text-editor-blots.js` (Nbsp class), `vcf-enhanced-rich-text-editor.js` (line 1876-1880) |

### 7. Whitespace Indicators (Show/Hide Symbols)

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | `showWhitespace` property, CSS-based indicators using `::before`/`::after` pseudo-elements on `.ql-tab`, `.ql-soft-break`, paragraph ends. Symbols: right-arrow (tab), return-arrow (soft-break), pilcrow (paragraph), down-left-arrow (auto-wrap). Toolbar toggle button. Java: `setShowWhitespace()` / `isShowWhitespace()`. |
| **RTE 2 (Vaadin 25)** | Not available. |
| **Quill 1/2** | Not a native feature. |
| **Migration status** | **CUSTOM** -- Must port CSS indicators and toggle logic. Depends on tab/soft-break blots being present (those blots provide the CSS hooks). |
| **Effort** | LOW-MEDIUM -- Mostly CSS. Needs attribute toggle + toolbar button injection. |
| **Files** | `vcf-enhanced-rich-text-editor-styles.js` (CSS rules), `vcf-enhanced-rich-text-editor.js` (showWhitespace property, _onWhitespaceClick) |

### 8. Toolbar Slot System (24 Slots)

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | 24 named `<slot>` elements in the toolbar template: `toolbar-start`, `toolbar-end`, `toolbar-before-group-*`, `toolbar-after-group-*` for 10 groups, plus `toolbar` (legacy GROUP_CUSTOM). Java `ToolbarSlot` enum (24 values) with `addToolbarComponents(slot, components)`, `addToolbarComponentsAtIndex()`, `getToolbarComponent()`, `removeToolbarComponent()`. |
| **RTE 2 (Vaadin 25)** | **Not available.** RTE 2's toolbar is internal with `::part()` CSS selectors for styling only. No `<slot>` elements for custom content injection. The toolbar groups are exposed via `::part(toolbar-group-*)` for CSS but not for DOM injection. |
| **Quill 1/2** | Quill's toolbar module allows custom handlers but not DOM slot injection. |
| **Migration status** | **CUSTOM** -- This is architecturally the hardest challenge. Options: (a) runtime JS injection of slot-like containers into RTE 2's shadow DOM, (b) wrapper element with external toolbar supplementing RTE 2's toolbar, (c) full toolbar replacement. The user description explicitly calls this out as the key updatability challenge. |
| **Effort** | HIGH -- Requires deep investigation of RTE 2's shadow DOM structure and whether runtime manipulation is feasible without breaking upgrades. |
| **Files** | `vcf-enhanced-rich-text-editor.js` (template slots), `ToolbarSlot.java`, `SlotUtil.java`, `EnhancedRichTextEditor.java` (addToolbarComponents, etc.) |

### 9. Toolbar Button Visibility Toggle

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | `toolbarButtons` property (Object), `_buttonDisplay()` / `_buttonGroupDisplay()` functions that set `display: none` on individual buttons/groups. Java: `setToolbarButtonsVisibility(Map<ToolbarButton, Boolean>)`, `ToolbarButton` enum (28 values including WHITESPACE, READONLY, PLACEHOLDER, PLACEHOLDER_APPEARANCE). |
| **RTE 2 (Vaadin 25)** | **Not available as Java API.** However, since RTE 2 exposes `::part(toolbar-button-*)` CSS selectors, individual buttons CAN be hidden via CSS (`display: none`). But there is no Java method to programmatically toggle visibility. |
| **Quill 1/2** | Not a Quill feature. |
| **Migration status** | **CUSTOM (Java API)** / **PARTIAL (CSS workaround)** -- The CSS approach works for hiding buttons, but the Java API (`setToolbarButtonsVisibility`) and the `ToolbarButton` enum need custom migration. RTE 2 buttons can be styled away via `::part()`, so the runtime JS approach may just need to set styles on parts. |
| **Effort** | MEDIUM -- Java API wrapper + JS bridge to set display styles on toolbar button parts. |
| **Files** | `EnhancedRichTextEditor.java` (ToolbarButton enum, setToolbarButtonsVisibility), `vcf-enhanced-rich-text-editor.js` (_buttonDisplay, _buttonGroupDisplay) |

### 10. Custom Toolbar Keyboard Shortcuts

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | `addStandardButtonBinding(button, keyCode, shortKey, shiftKey, altKey)` -- binds any key combo to any toolbar button action. `addToolbarFocusBinding(key, ...)` -- binds key combo to focus toolbar. Both work by injecting bindings into Quill's keyboard module. |
| **RTE 2 (Vaadin 25)** | Not available. No API for custom keyboard shortcuts. |
| **Quill 1/2** | Quill's keyboard module supports `addBinding()` natively, so this is technically possible at the Quill level. |
| **Migration status** | **CUSTOM** -- Requires access to the Quill instance inside RTE 2. If RTE 2 exposes the Quill instance (or it can be accessed via DOM traversal to `.ql-container.__quill`), bindings can be added. Java API wrapper needed. |
| **Effort** | MEDIUM -- Core logic is simple if Quill instance is accessible. |
| **Files** | `EnhancedRichTextEditor.java` (addStandardToolbarButtonShortcut, addToobarFocusShortcut), `vcf-enhanced-rich-text-editor.js` (addStandardButtonBinding, addToolbarFocusBinding) |

### 11. HTML Sanitization (Server-Side Jsoup)

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | `sanitize(html)` method using Jsoup with custom safelist: basic + `img`, `h1-h3`, `s`, `span`, `br`. Post-sanitization filtering: restricts `span class` to known safe classes (`ql-tab`, `ql-soft-break`, `ql-readonly`, `ql-placeholder`), restricts `contenteditable` to only `false`. |
| **RTE 2 (Vaadin 25)** | **PARTIAL.** RTE 2's Flow component does HTML sanitization, but docs recommend client-side sanitization via `dompurify` for the web component. The exact server-side safelist is not documented in detail, but it handles standard HTML tags. It will NOT know about ERTE-specific classes (`ql-tab`, `ql-readonly`, etc.). |
| **Quill 1/2** | Not a Quill concern. |
| **Migration status** | **CUSTOM** -- Must extend the sanitization safelist to include ERTE-specific span classes and attributes. The Jsoup-based approach should be straightforward to port. |
| **Effort** | LOW -- Port the sanitize method, add ERTE-specific classes to safelist. |
| **Files** | `EnhancedRichTextEditor.java` (sanitize method, lines 258-295) |

### 12. I18n with ERTE-Specific Labels

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | `RichTextEditorI18n` inner class with 32 translatable strings (all standard buttons + ERTE-specific: `readonly`, `placeholder`, `placeholderAppearance`, `placeholderComboBoxLabel`, `placeholderAppearanceLabel1/2`, `placeholderDialogTitle`, `whitespace`). i18n object sent via `runBeforeClientResponse` using `executeJs`. |
| **RTE 2 (Vaadin 25)** | **PARTIAL.** RTE 2 has built-in i18n for standard toolbar buttons (undo, redo, bold, italic, etc.). No documentation on ERTE-specific labels obviously. |
| **Quill 1/2** | Not a Quill feature. |
| **Migration status** | **CUSTOM** -- Must extend RTE 2's i18n with ERTE-specific labels. Standard labels can likely be inherited from RTE 2. |
| **Effort** | LOW -- Additive. Only need i18n for custom ERTE buttons/dialogs. |
| **Files** | `EnhancedRichTextEditor.java` (RichTextEditorI18n, lines 604-1314), `vcf-enhanced-rich-text-editor.js` (i18n property, lines 550-591) |

### 13. Theme Variants

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | Inherits from `HasTheme` via generated base class. Uses Vaadin's ThemableMixin. Custom ERTE styling for `.ql-readonly` (gray background), custom toolbar icons. |
| **RTE 2 (Vaadin 25)** | **NATIVE.** RTE 2 supports `compact` and `no-border` theme variants natively. Extensive CSS custom properties for toolbar, buttons, content area. Both Lumo and Aura themes supported. |
| **Migration status** | **NATIVE + CUSTOM** -- Standard theme variants are available. ERTE-specific styles (readonly blot styling, tab indicators, ruler styling) must be added as custom CSS. |
| **Effort** | LOW -- Leverage RTE 2 variants; add custom CSS for ERTE-specific elements. |
| **Files** | `vcf-enhanced-rich-text-editor-styles.js`, `vcf-enhanced-rich-text-editor-toolbar-styles.js` |

### 14. Programmatic Text Insertion

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | `addText(String text, int position)` -- inserts at specific position. `addText(String text)` -- inserts at current cursor. Both call `_editor.insertText()` via `executeJs`. |
| **RTE 2 (Vaadin 25)** | **Not documented as Java API.** RTE 2 exposes the Quill instance internally, but no public Java method for programmatic text insertion. |
| **Quill 1/2** | Quill's `insertText()` is a native API. |
| **Migration status** | **CUSTOM (Java API)** -- The Java wrapper methods need to be recreated. If the Quill instance is accessible via `element._editor` or similar, the `executeJs` calls can be ported. |
| **Effort** | LOW -- Simple JS bridge methods. |
| **Files** | `EnhancedRichTextEditor.java` (addText methods, lines 391-409) |

### 15. Value Change Mode

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | Implements `HasValueChangeMode`, default `ValueChangeMode.ON_CHANGE`. Custom `setSynchronizedEvent()` logic. |
| **RTE 2 (Vaadin 25)** | **NATIVE.** RTE 2 supports `setValueChangeMode(ValueChangeMode.TIMEOUT)` and other modes. Documented in official examples. |
| **Migration status** | **NATIVE** -- Already available in RTE 2. No migration needed. |
| **Effort** | NONE |
| **Files** | N/A |

### 16. extendOptions Callback Hook

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | Before Quill instantiation, checks `window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendOptions` array and calls each callback with `(options, Quill)`. Used by the tables extension to register additional blots and modules. |
| **RTE 2 (Vaadin 25)** | **Not available.** RTE 2 does not expose a pre-instantiation hook. |
| **Quill 1/2** | Not a Quill feature -- this is ERTE's extension mechanism. |
| **Migration status** | **CUSTOM** -- Critical for the tables extension. Must either: (a) find a way to hook into RTE 2 before Quill init (e.g., by patching the web component's `ready()` method), (b) register Quill modules globally before the element is created, or (c) use a different extension pattern. |
| **Effort** | HIGH -- Architecturally significant. If RTE 2's Quill init cannot be intercepted, this may require a fundamentally different approach for extensions. |
| **Files** | `vcf-enhanced-rich-text-editor.js` (lines 1137-1142) |

### 17. List Indentation (Indent / Outdent)

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | Toolbar buttons for indent/outdent (`ql-indent` with values `-1`/`+1`), dedicated toolbar group `toolbar-group-indent`. I18n labels for `deindent` and `indent`. |
| **RTE 2 (Vaadin 25)** | **Not present as toolbar buttons.** RTE 2's toolbar does NOT include indent/outdent buttons. However, the styling docs do NOT show `toolbar-button-indent` or `toolbar-button-deindent` parts. Lists are supported (ordered + bullet) but without explicit indent/outdent UI. Quill handles Tab key for list indentation natively. |
| **Quill 1/2** | Quill supports `indent` format natively. Tab key indents list items by default. Quill 2 improves list handling. |
| **Migration status** | **PARTIAL** -- Quill handles list indentation via Tab key natively. But if explicit toolbar buttons are needed, they must be added custom. This was already a custom ERTE addition over vanilla RTE 1. |
| **Effort** | LOW-MEDIUM -- If toolbar buttons are required, add them via the slot system. If Tab-key indentation suffices, no work needed. |
| **Files** | `vcf-enhanced-rich-text-editor.js` (indent toolbar group, lines 314-326) |

### 18. Text Color / Background Color

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | **Not present.** ERTE 1 does NOT have text color or background color buttons. |
| **RTE 2 (Vaadin 25)** | **NEW-IN-RTE2.** RTE 2 includes text color and text background buttons with customizable color palette (`setColorOptions(List<String>)`). Toolbar group `toolbar-group-style` with parts `toolbar-button-color` and `toolbar-button-background`. |
| **Migration status** | **NEW-IN-RTE2** -- This is a new RTE 2 feature that ERTE 2 gets for free by extending RTE 2. No migration needed. |
| **Effort** | NONE -- Inherited from RTE 2. |
| **Files** | N/A |

### 19. Align Justify

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | Toolbar button `alignJustify` (value `justify`), included in alignment group. |
| **RTE 2 (Vaadin 25)** | **Not present.** RTE 2 only has left, center, and right alignment. No justify button. |
| **Migration status** | **CUSTOM** -- Must add justify button to toolbar. Quill supports `align: justify` natively, so only the toolbar button needs to be injected. |
| **Effort** | LOW -- Single button injection + Quill format binding. |
| **Files** | `vcf-enhanced-rich-text-editor.js` (align justify button, line 347) |

### 20. Replace Toolbar Button Icons

| Aspect | Detail |
|--------|--------|
| **ERTE 1 implementation** | `replaceStandardToolbarButtonIcon(ToolbarButton, Icon)` -- replaces icon of any standard button via slot mechanism. |
| **RTE 2 (Vaadin 25)** | Not available as API. Button icons are internal. CSS styling of `::part(toolbar-button-*-icon)` is possible but limited. |
| **Migration status** | **CUSTOM** -- Need runtime JS to replace icon content in toolbar buttons. |
| **Effort** | LOW -- DOM manipulation via JS. |
| **Files** | `EnhancedRichTextEditor.java` (replaceStandardToolbarButtonIcon), `SlotUtil.java` |

---

## Summary Table

| # | Feature | RTE 2 Native? | Quill Native? | Migration Action | Effort |
|---|---------|:---:|:---:|---|:---:|
| 1 | Tab stops + alignment | No | No | CUSTOM | HIGH |
| 2 | Rulers | No | No | CUSTOM | MEDIUM |
| 3 | Soft-break (Shift+Enter + tab copy) | No | No | CUSTOM | MEDIUM |
| 4 | Read-only sections (inline) | No | No | CUSTOM | MEDIUM |
| 5 | Placeholders (embed + formatting) | No | No | CUSTOM | HIGH |
| 6 | Non-breaking space (Shift+Space) | No | No | CUSTOM | LOW |
| 7 | Whitespace indicators | No | No | CUSTOM | LOW-MED |
| 8 | Toolbar slot system (24 slots) | No | No | CUSTOM | HIGH |
| 9 | Toolbar button visibility toggle | Partial (CSS) | No | CUSTOM (Java API) | MEDIUM |
| 10 | Custom keyboard shortcuts | No | Partial | CUSTOM | MEDIUM |
| 11 | HTML sanitization (Jsoup, ERTE classes) | Partial | N/A | CUSTOM (extend) | LOW |
| 12 | I18n (ERTE-specific labels) | Partial | No | CUSTOM (additive) | LOW |
| 13 | Theme variants | Yes | N/A | NATIVE + CSS | LOW |
| 14 | Programmatic text insertion | No (Java) | Yes (Quill) | CUSTOM (Java API) | LOW |
| 15 | Value change mode | Yes | N/A | NATIVE | NONE |
| 16 | extendOptions callback hook | No | No | CUSTOM | HIGH |
| 17 | List indentation buttons | Partial (Tab key) | Yes | PARTIAL | LOW-MED |
| 18 | Text color / background color | Yes (new) | Yes | NEW-IN-RTE2 | NONE |
| 19 | Align justify | No | Yes (Quill) | CUSTOM (button only) | LOW |
| 20 | Replace toolbar button icons | No | No | CUSTOM | LOW |

---

## Migration Priority Tiers

### Tier 0 -- Free (Inherited from RTE 2)
- Value change mode (15)
- Text color / background color (18)
- Theme variants (13) -- standard variants

### Tier 1 -- Must Have (Core ERTE Differentiators)
- Tab stops + alignment (1) -- **HIGH effort**, cornerstone feature
- Rulers (2) -- tightly coupled with tab stops
- Soft-break (3) -- depends on tab stops
- Toolbar slot system (8) -- **HIGH effort**, architectural foundation for all custom buttons
- extendOptions hook (16) -- **HIGH effort**, required for tables extension

### Tier 2 -- Must Have (Important Features)
- Read-only sections (4) -- key ERTE differentiator
- Placeholders (5) -- complex, widely used
- Toolbar button visibility (9) -- used by many customers
- Custom keyboard shortcuts (10)

### Tier 3 -- Nice to Have (Lower Complexity)
- Non-breaking space (6)
- Whitespace indicators (7)
- HTML sanitization extension (11)
- I18n extension (12)
- Programmatic text insertion (14)
- List indentation buttons (17)
- Align justify (19)
- Replace toolbar button icons (20)

---

## Key Architectural Decisions Required

1. **Quill instance access**: Can ERTE 2 access `element._editor` or `.ql-container.__quill` on RTE 2? This determines feasibility of keyboard bindings, text insertion, and blot registration.

2. **Shadow DOM manipulation**: Can ERTE 2 inject DOM nodes (slots, rulers, custom buttons) into RTE 2's shadow DOM at runtime without forking? If not, a wrapper/composition pattern is needed.

3. **Pre-Quill-init hook**: The `extendOptions` pattern requires running code BEFORE Quill is instantiated. Does RTE 2 provide any lifecycle hook? If not, can the web component's `ready()` / `connectedCallback()` be monkey-patched?

4. **Blot registration**: Custom blots (TabBlot, SoftBreakBlot, ReadOnlyBlot, PlaceholderBlot, Nbsp) must be registered with Quill BEFORE document content is set. Timing is critical.

5. **Delta format compatibility**: ERTE 1 deltas contain custom blot types (`tab`, `soft-break`, `readonly`, `placeholder`, `nbsp`). RTE 2 will not understand these. A delta migration/conversion strategy is needed (ERTE 1 already has `TabConverter.java` for old-to-new tab format conversion).

6. **Tag identity**: Will ERTE 2 use its own tag (e.g., `<vcf-enhanced-rich-text-editor-v2>`) or extend/wrap `<vaadin-rich-text-editor>`? This affects the Java class hierarchy.
