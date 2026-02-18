# ERTE V25 Migration — Use Case Analysis (Step 0)

Cross-reference of every ERTE 1 feature with RTE 2 (Vaadin 25). Sources: V24 source code (JS + Java), `feature_comparison.md`, `SPIKE_RESULTS.md`, `SECURITY.md`, `quill_v1_to_v2_api_diff.md`.

---

## Feature 1: Tabstops (L/R/M Alignment + Width Calculation Engine)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | User presses Tab to insert a tab embed. Tab width is iteratively calculated based on defined tabstop positions. Supports LEFT (text after tab), RIGHT (text before tab), and MIDDLE (centered) alignment. Tabs on wrapped lines get fixed width. |
| **Key JS Methods** | `_updateTabWidths()`, `_requestTabUpdate()`, `_createMeasureSpan()`, `_measureTextWidth()`, `_measureContentWidth()`, `_isWrappedLine()`, `_tabStopsChanged()`, `_addTabStop()` |
| **Key Java API** | `TabStop` class (Direction enum: LEFT/RIGHT/MIDDLE, position), `tabStops` property (JsonArray, synced to client), `setTabStops()`/`getTabStops()` via property binding |
| **Custom Blots** | `TabBlot` (Embed, `<span class="ql-tab">`, contenteditable=false, zero-width-space content, smart cursor placement on click via mousedown handler) |
| **Events** | None (tab operations are client-side Quill changes, reflected via value-changed) |
| **RTE 2 Support** | **NONE** — No tab stop concept in RTE 2 or Quill 2. |
| **Migration Path** | **CUSTOM** — Register TabBlot as custom Embed via `Quill.register()`. Port iterative width engine (~200 lines). Wire tabStops property. Spike confirmed: Item 2 (blot registration), Item 20 (guard nodes INSIDE embed — measure OUTER .ql-tab rect). |
| **Dependencies** | None (foundation feature) |
| **Security Notes** | None — TabBlot uses zero-width-space text, no dynamic content injection. |
| **Effort** | **HIGH** (~400 lines JS + Java API) |

---

## Feature 2: Rulers (Horizontal + Vertical)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Horizontal and vertical rulers rendered as base64 PNG background images. Horizontal ruler has click-to-add tabstop with alignment cycling (LEFT → RIGHT → MIDDLE → remove). `noRulers` property toggles visibility. |
| **Key JS Methods** | `_addTabStop(event)` (click handler on horizontal ruler), `_rulerDisplay()`, `_rulerDisplayFlexWrapper()` |
| **Key Java API** | `noRulers` property (boolean, reflectToAttribute), `setNoRulers()`/`isNoRulers()` |
| **Custom Blots** | None (rulers are pure DOM/CSS, not Quill blots) |
| **Events** | None |
| **RTE 2 Support** | **NONE** — No ruler concept in RTE 2. |
| **Migration Path** | **CUSTOM** — Inject ruler DOM into RTE 2's shadow DOM via `render()` override. Click handler creates/cycles/removes TabStop objects. Spike confirmed: Item 4 (toolbar/render override works). |
| **Dependencies** | Tabstops (Feature 1) — ruler interacts with tabstop array |
| **Security Notes** | None — rulers are static images, click handler only modifies tabStops array. |
| **Effort** | **MEDIUM** |

---

## Feature 3: Soft-Break (Shift+Enter + Tab Copying)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Shift+Enter inserts a visual line break within a paragraph (no new `<p>`). Tabs from the visual line start to cursor position are copied after the soft-break. Number of copied tabs is limited to the count of defined tabstops (customer requirement). |
| **Key JS Methods** | `_patchKeyboard()` (Shift+Enter binding), soft-break handler (~60 lines): finds visual line boundaries, counts tabs before cursor, inserts SoftBreakBlot, copies tabs |
| **Key Java API** | None (entirely client-side behavior) |
| **Custom Blots** | `SoftBreakBlot` (Embed, `<span class="ql-soft-break">`, contains `<br>`) |
| **Events** | None (value change via Quill text-change) |
| **RTE 2 Support** | **NONE** — No soft-break concept in RTE 2 or Quill 2. |
| **Migration Path** | **CUSTOM** — Register SoftBreakBlot + add Shift+Enter keyboard binding via `_editor.keyboard.addBinding()`. Spike confirmed: Item 7 (binding priority — addBinding appends to END, must clear and re-add to override defaults). |
| **Dependencies** | Tabstops (Feature 1) — tab copying depends on tabstop count and tab blot |
| **Security Notes** | SoftBreakBlot uses `innerHTML = '<br>'` — static content, but should use `createElement('br')` + `appendChild()` in V25 per security rules. |
| **Effort** | **MEDIUM** |

---

## Feature 4: Readonly Sections (Inline contenteditable=false)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Toolbar toggle wraps selected text in `<span class="ql-readonly" contenteditable="false">`. Delete protection: text-change handler reverts delta if readonly blot count decreases. Gray background styling. Cannot type/paste inside. |
| **Key JS Methods** | `_onReadonlyClick()` (toolbar handler), text-change handler (lines 1147-1162: compares readonly blot counts, reverts via `setContents(oldDelta)`), `_disabledChanged()` |
| **Key Java API** | None specific (uses Quill format API). `ToolbarButton.READONLY` for visibility control. |
| **Custom Blots** | `ReadOnlyBlot` (Inline, `<span class="ql-readonly" contenteditable="false">`, allowedChildren includes Block/Inline/TextBlot/ListItem/ListContainer) |
| **Events** | None |
| **RTE 2 Support** | **NONE** for inline readonly. RTE 2 has whole-editor readonly mode only (`setReadOnly(true)`). |
| **Migration Path** | **CUSTOM** — Register ReadOnlyBlot + add toolbar button + implement delete protection in text-change handler. Spike confirmed: Item 13 (setReadOnly observer works on extended component). |
| **Dependencies** | Toolbar Slot System (Feature 8) — needs button injection |
| **Security Notes** | ReadOnlyBlot sets `contenteditable="false"` — sanitizer must whitelist this attribute on `<span>`. Already handled in ERTE 1 sanitizer. |
| **Effort** | **MEDIUM** |

---

## Feature 5: Placeholders (Embed + Dialog + Formatting)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Embedded tokens in editor content. Configured via `setPlaceholders()`. Inserted via toolbar button → confirm dialog with combo-box, or Ctrl+P/Meta+P shortcut. Supports: normal/alt appearance with regex pattern, inline formatting (bold/italic/font/link/script), tags wrapping (`{{Name}}`), alt format. Complete lifecycle with cancel/confirm pattern — default action is ALWAYS prevented, Java listener must call `event.insert()` or `event.remove()`. |
| **Key JS Methods** | `_onPlaceholderClick()`, `_onPlaceholderEditConfirm()`, `_onPlaceholderEditRemove()`, `_onPlaceholderEditCancel()`, `_confirmInsertPlaceholders()`, `_confirmRemovePlaceholders()`, `_removePlaceholders()`, `_placeholdersChanged()`, `_placeholderTagsChanged()`, `_placeholderAltAppearanceChanged()`, `_placeholderAltAppearancePatternChanged()` |
| **Key Java API** | `setPlaceholders(Collection<Placeholder>)`, `getPlaceholders()`, `setPlaceholderAltAppearancePattern(String)`, `setPlacehoderAltAppearance(boolean)`, `Placeholder` class (text, format, altFormat, index) |
| **Custom Blots** | `PlaceholderBlot` (Embed, `<span class="ql-placeholder">`, JSON in `data-placeholder`, static tags/altAppearanceRegex, `setText()` renders with tags+format, `deltaToInline()` applies formatting) |
| **Events** | `PlaceholderButtonClickedEvent` (position), `PlaceholderBeforeInsertEvent` (cancel/confirm pattern, `event.insert()`), `PlaceholderInsertedEvent`, `PlaceholderBeforeRemoveEvent` (cancel/confirm, `event.remove()`), `PlaceholderRemovedEvent`, `PlaceholderSelectedEvent`, `PlaceholderLeaveEvent`, `PlaceholderAppearanceChangedEvent` (altAppearance, appearanceLabel) |
| **RTE 2 Support** | **NONE** |
| **Migration Path** | **CUSTOM** — Full migration of PlaceholderBlot + confirm dialog (vaadin-confirm-dialog + vaadin-combo-box) + toolbar buttons + keyboard bindings + appearance toggle. Spike confirmed: Item 3 (custom embed insert), Item 19 (sanitizer strips class — use Delta API or override sanitize). |
| **Dependencies** | Toolbar Slot System (Feature 8) — needs button injection for placeholder + appearance toggle |
| **Security Notes** | **CRITICAL: PlaceholderBlot XSS** — `node.innerHTML = text` at line 197 of blots.js. Text comes from `data-placeholder` JSON. MUST use `textContent` instead of `innerHTML` in V25. Also: `_wrapContent()` uses `innerHTML` — rewrite with `createElement`/`appendChild`. |
| **Effort** | **HIGH** (second most complex feature after tabstops) |

---

## Feature 6: Non-Breaking Space (Shift+Space)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Shift+Space inserts a non-breaking space character. Preserved in content, not collapsed by browser. |
| **Key JS Methods** | Keyboard binding in `_patchKeyboard()` (~4 lines) |
| **Key Java API** | None (client-side only) |
| **Custom Blots** | `Nbsp` (Inline, `<span>&nbsp;</span>`, registered as `formats/nbsp`) |
| **Events** | None |
| **RTE 2 Support** | **NONE** |
| **Migration Path** | **CUSTOM** — Register Nbsp blot + add keyboard binding. ~15 lines total. |
| **Dependencies** | None |
| **Security Notes** | Nbsp uses `innerHTML = '&nbsp;'` — static content, safe. Could use `textContent = '\u00A0'` in V25. |
| **Effort** | **LOW** |

---

## Feature 7: Whitespace Indicators (Symbols + Legend + Toggle)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Toggle button shows/hides whitespace symbols: → (tab), ↵ (soft-break), ¶ (paragraph end), ⮐ (auto-wrap). CSS `::before`/`::after` pseudo-elements on blot classes. `show-whitespace` class on `.ql-editor`. Legend overlay available. |
| **Key JS Methods** | `_onWhitespaceClick()`, `_showWhitespaceChanged(show)` (toggles `show-whitespace` class + button active state) |
| **Key Java API** | `setShowWhitespace(boolean)`, `isShowWhitespace()` (element property), `ToolbarButton.WHITESPACE` |
| **Custom Blots** | None (CSS-only, applied to existing blot classes) |
| **Events** | None |
| **RTE 2 Support** | **NONE** |
| **Migration Path** | **CUSTOM** — Port CSS indicators (content styles), add toggle button via render() override, wire showWhitespace property. Depends on tab/soft-break blots providing CSS hooks (`.ql-tab`, `.ql-soft-break`). |
| **Dependencies** | Tabstops (Feature 1), Soft-Break (Feature 3) — CSS targets their blot classes |
| **Security Notes** | None — pure CSS pseudo-elements. |
| **Effort** | **LOW-MEDIUM** |

---

## Feature 8: Toolbar Slot System (24 Named Slots)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | 24 named `<slot>` elements in toolbar template: START, END, BEFORE/AFTER for 10 groups (history, emphasis, heading, glyph-transformation, list, indent, alignment, rich-text, block, format), plus GROUP_CUSTOM. Java Vaadin components are added to slots and rendered in the toolbar. |
| **Key JS Methods** | `_onToolbarSlotChange()`, `_setCustomButtons()`, `_updateToolbarButtons()`, `addCustomButton()`, `setCustomButtonLabel()`, `setCustomButtonIcon()`, `setCustomButtonClickListener()`, `setCustomButtonKeyboardShortcut()` |
| **Key Java API** | `ToolbarSlot` enum (24 values), `addToolbarComponents(slot, components)`, `addToolbarComponentsAtIndex(slot, index, components)`, `getToolbarComponent(slot, id)`, `removeToolbarComponent(slot, id/component)`, `addCustomToolbarComponents(components)` (legacy, uses GROUP_CUSTOM), `SlotUtil` helper class |
| **Custom Blots** | None |
| **Events** | None (custom button clicks handled by Java component event listeners) |
| **RTE 2 Support** | **NONE** — RTE 2's toolbar is internal with `::part()` CSS selectors only. No slot elements. |
| **Migration Path** | **CUSTOM** — Override `render()` in JS subclass. Add `<slot>` elements around standard RTE 2 toolbar groups. This is the key architectural challenge for updatability. Spike confirmed: Item 4 (render override works), Item 5 (custom buttons survive re-renders). |
| **Dependencies** | None (foundation for all custom buttons) |
| **Security Notes** | None — slots are structural DOM, no dynamic content. |
| **Effort** | **HIGH** |

---

## Feature 9: Toolbar Button Visibility (Show/Hide)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Individual toolbar buttons can be hidden via `setToolbarButtonsVisibility(Map<ToolbarButton, Boolean>)`. Groups auto-hide when all buttons hidden. CSS `display: none` applied per button. |
| **Key JS Methods** | `_buttonDisplay(toolbarButtons, button)`, `_buttonGroupDisplay(toolbarButtons, group)` |
| **Key Java API** | `setToolbarButtonsVisibility(Map<ToolbarButton, Boolean>)`, `getToolbarButtonsVisibility()`, `ToolbarButton` enum (28 values: standard buttons + WHITESPACE, READONLY, PLACEHOLDER, PLACEHOLDER_APPEARANCE) |
| **Custom Blots** | None |
| **Events** | None |
| **RTE 2 Support** | **PARTIAL** — RTE 2 exposes `::part(toolbar-button-*)` for CSS hiding, but no Java API. |
| **Migration Path** | **CUSTOM (Java API)** — Wrap RTE 2's part-based buttons + ERTE custom buttons. Set display styles via JS bridge. ToolbarButton enum extended with ERTE-specific entries. |
| **Dependencies** | Toolbar Slot System (Feature 8) |
| **Security Notes** | None |
| **Effort** | **MEDIUM** |

---

## Feature 10: Custom Keyboard Shortcuts

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | `addStandardToolbarButtonShortcut()` binds any key combo to any toolbar button action. `addToobarFocusShortcut()` binds key combo to focus toolbar. Both inject bindings into Quill's keyboard module. |
| **Key JS Methods** | `addStandardButtonBinding(button, keyCode, shortKey, shiftKey, altKey)`, `addToolbarFocusBinding(keyCode, shortKey, shiftKey, altKey)` |
| **Key Java API** | `addStandardToolbarButtonShortcut(ToolbarButton, keyCode, shortKey, shiftKey, altKey)`, `addToobarFocusShortcut(keyCode, shortKey, shiftKey, altKey)` |
| **Custom Blots** | None |
| **Events** | None |
| **RTE 2 Support** | **NONE** |
| **Migration Path** | **CUSTOM** — Access Quill instance via `this._editor.keyboard` and use `addBinding()`. Spike confirmed: Item 6 (keyboard module accessible), Item 7 (binding priority pattern). **V25 change**: Use string key names instead of numeric keyCodes (Quill 2 requirement). |
| **Dependencies** | None (but useful with Toolbar Slot System for custom button shortcuts) |
| **Security Notes** | None |
| **Effort** | **MEDIUM** |

---

## Feature 11: HTML Sanitization (Jsoup + ERTE Class Whitelist)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Server-side Jsoup sanitizer. Base safelist (basic + img, h1-h3, s, span, br). Post-sanitization: restricts `span class` to known ERTE classes (`ql-tab`, `ql-soft-break`, `ql-readonly`, `ql-placeholder`), restricts `contenteditable` to `false` only. |
| **Key JS Methods** | None (server-side only) |
| **Key Java API** | `sanitize(String html)` method, `getHtmlValue()` (calls sanitize) |
| **Custom Blots** | None (sanitizer preserves blot class names in HTML output) |
| **Events** | None |
| **RTE 2 Support** | **PARTIAL** — RTE 2 has its own sanitization but won't know about ERTE-specific classes. |
| **Migration Path** | **CUSTOM (extend)** — Override/extend RTE 2's sanitization. Add ERTE class whitelist. Spike confirmed: Item 19 (sanitizer strips `class` — must override or use Delta API). |
| **Dependencies** | All blot features (Features 1, 3, 4, 5) — their classes must be whitelisted |
| **Security Notes** | **WARNING: Unrestricted `style` attribute** — `.addAttributes(":all", "style")` without value sanitization. Fix: restrict style properties. **WARNING: `data:` protocol** — allowed on images without MIME restriction. Fix: restrict to `data:image/*`. |
| **Effort** | **LOW** |

---

## Feature 12: I18n (32 Translatable Strings)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | `RichTextEditorI18n` inner class with 32 translatable strings: standard button tooltips (undo, redo, bold, italic, underline, strike, h1-h3, subscript, superscript, listOrdered, listBullet, deindent, indent, alignLeft/Center/Right/Justify, image, link, blockquote, codeBlock, clean) + ERTE-specific (whitespace, readonly, placeholder, placeholderAppearance, placeholderComboBoxLabel, placeholderAppearanceLabel1/2, placeholderDialogTitle) + dialog (linkDialogTitle, ok, cancel, remove). |
| **Key JS Methods** | `i18n` property (Object with 32 keys, default English values) |
| **Key Java API** | `setI18n(RichTextEditorI18n)`, `getI18n()`, `RichTextEditorI18n` inner class with fluent setters |
| **Custom Blots** | None |
| **Events** | None |
| **RTE 2 Support** | **PARTIAL** — RTE 2 has i18n for standard buttons. ERTE-specific labels not included. |
| **Migration Path** | **CUSTOM (additive)** — Extend RTE 2's i18n with ERTE-specific labels. Standard labels inherited. |
| **Dependencies** | Placeholders (Feature 5), Whitespace (Feature 7), Readonly (Feature 4) — their labels are ERTE-specific |
| **Security Notes** | None |
| **Effort** | **LOW** |

---

## Feature 13: Theme Variants (Lumo + Custom ERTE Styles)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Inherits Vaadin's ThemableMixin. Custom CSS for `.ql-readonly` (gray background `#f1f1f1`, border-radius), tab indicators (blue underlines via whitespace indicators), toolbar styling. Lumo and Material themes. |
| **Key JS Methods** | CSS in `vcf-enhanced-rich-text-editor-styles.js`, `vcf-enhanced-rich-text-editor-toolbar-styles.js`, `vcf-enhanced-rich-text-editor-content-styles.js` |
| **Key Java API** | `EnhancedRichTextEditorVariant` (extends HasTheme) |
| **Custom Blots** | None |
| **Events** | None |
| **RTE 2 Support** | **NATIVE** — `compact` and `no-border` variants. Extensive CSS custom properties. Both Lumo and Aura themes. |
| **Migration Path** | **NATIVE + CUSTOM CSS** — Standard variants inherited. ERTE-specific styles (readonly, tab indicators, ruler) added as custom CSS. Spike confirmed: Item 11 (Lumo loading via `@StyleSheet(Lumo.STYLESHEET)`), Item 12 (`::part()` selectors work, tag-specific isolation). |
| **Dependencies** | None |
| **Security Notes** | None |
| **Effort** | **LOW** |

---

## Feature 14: Programmatic Text Insertion (addText API)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | `addText(text, position)` inserts at specific position. `addText(text)` inserts at current cursor. Both validate bounds and call Quill's `insertText()` via `executeJs`. |
| **Key JS Methods** | Quill's `_editor.insertText(index, text)`, `_editor.getSelection()` |
| **Key Java API** | `addText(String text, int position)`, `addText(String text)`, `getTextLength()` |
| **Custom Blots** | None |
| **Events** | None (text-change triggers value-changed) |
| **RTE 2 Support** | **NONE (Java API)** — Quill's `insertText()` exists but no Java wrapper in RTE 2. |
| **Migration Path** | **CUSTOM (Java API)** — Recreate Java wrapper methods using `executeJs` with `$0._editor.insertText()`. |
| **Dependencies** | None |
| **Security Notes** | `executeJs` with parameter binding (`$0`, `$1`) — safe pattern. Do NOT concatenate text into JS strings. |
| **Effort** | **LOW** |

---

## Feature 15: Value Change Mode

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Implements `HasValueChangeMode`. Default `ON_CHANGE`. Supports EAGER, LAZY, TIMEOUT. |
| **Key JS Methods** | None (handled by Vaadin framework) |
| **Key Java API** | `setValueChangeMode(ValueChangeMode)`, `getValueChangeMode()` |
| **Custom Blots** | None |
| **Events** | `value-changed` (standard Vaadin event) |
| **RTE 2 Support** | **NATIVE** — Fully supported. |
| **Migration Path** | **INHERIT** — No migration needed. |
| **Dependencies** | None |
| **Security Notes** | None |
| **Effort** | **NONE** |

---

## Feature 16: extendOptions Hook (Pre-Quill-Init Callback)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Before Quill instantiation, checks `window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendOptions` array and calls each callback with `(options, Quill)`. Used by the tables extension to register additional blots and modules before Quill init. |
| **Key JS Methods** | `ready()` method (lines 1137-1142): iterates `extendOptions` callbacks, passes options object + Quill class |
| **Key Java API** | None (JS-only extension mechanism, tables extension registers via its connector.js) |
| **Custom Blots** | None (enables external blot registration) |
| **Events** | None |
| **RTE 2 Support** | **NONE** — No pre-instantiation hook in RTE 2. |
| **Migration Path** | **CUSTOM** — Critical for tables extension. Options: (a) override `ready()` in JS subclass to call hooks before `super.ready()`, (b) register Quill modules globally before element creation, (c) use different extension pattern. Spike confirmed: Item 10 (module load order: blot registration → element definition → ready → _editor available). |
| **Dependencies** | None (but tables extension depends on this) |
| **Security Notes** | Callbacks receive full Quill class — ensure only trusted code can register hooks. |
| **Effort** | **HIGH** (architecturally significant) |

---

## Feature 17: List Indentation Buttons (Indent/Outdent)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Dedicated toolbar buttons for indent (`ql-indent +1`) and outdent (`ql-indent -1`). Separate toolbar group `toolbar-group-indent`. Works on list items. |
| **Key JS Methods** | Quill's built-in `indent` format handler (triggered by toolbar button class `ql-indent`) |
| **Key Java API** | `ToolbarButton.INDENT`, `ToolbarButton.DEINDENT`, I18n labels (`deindent`, `indent`) |
| **Custom Blots** | None (uses Quill's native indent format) |
| **Events** | None |
| **RTE 2 Support** | **PARTIAL** — Tab key indents list items natively. But no explicit toolbar buttons for indent/outdent. |
| **Migration Path** | **PARTIAL** — Add indent/outdent buttons via render() override. Quill handles the format natively. Only button injection needed. |
| **Dependencies** | Toolbar Slot System (Feature 8) — for button placement |
| **Security Notes** | None |
| **Effort** | **LOW-MEDIUM** |

---

## Feature 18: Align Justify (Toolbar Button)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Toolbar button `alignJustify` with value `justify`, in the alignment group alongside left/center/right. Quill handles the `align: justify` format natively. |
| **Key JS Methods** | Quill's built-in `align` format handler (triggered by toolbar button class `ql-align` value `justify`) |
| **Key Java API** | `ToolbarButton.ALIGN_JUSTIFY`, I18n label `alignJustify` |
| **Custom Blots** | None (uses Quill's native align format) |
| **Events** | None |
| **RTE 2 Support** | **NONE** — RTE 2 only has left, center, right alignment buttons. No justify button. |
| **Migration Path** | **CUSTOM (button only)** — Add justify button via render() override. Quill supports `align: justify` natively. |
| **Dependencies** | Toolbar Slot System (Feature 8) |
| **Security Notes** | None |
| **Effort** | **LOW** |

---

## Feature 19: Replace Toolbar Button Icons (Slot-Based)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | Each standard toolbar button has a named `<slot>` containing a `<vaadin-icon>`. `replaceStandardToolbarButtonIcon()` replaces the icon in a specific button's slot. |
| **Key JS Methods** | `setCustomButtonIcon(icon, btn)` — creates/replaces `<vaadin-icon>` element |
| **Key Java API** | `replaceStandardToolbarButtonIcon(ToolbarButton, Icon)`, `SlotUtil.replaceStandardButtonIcon()` |
| **Custom Blots** | None |
| **Events** | None |
| **RTE 2 Support** | **NONE** — Button icons are internal. `::part(toolbar-button-*-icon)` allows CSS styling but not DOM replacement. |
| **Migration Path** | **CUSTOM** — Runtime JS to replace icon content in toolbar buttons. Since render() override creates the buttons, we can include slots for icon replacement. |
| **Dependencies** | Toolbar Slot System (Feature 8) |
| **Security Notes** | None — Icon replacement uses Vaadin Icon component (safe). |
| **Effort** | **LOW** |

---

## Feature 20: Arrow Navigation (Tab-Aware Cursor Movement)

| Field | Detail |
|-------|--------|
| **ERTE 1 Behavior** | ArrowUp/ArrowDown through tab-filled lines. Tab blots are inline-block with tiny font-size, which confuses browser's vertical cursor navigation (skips intermediate lines). Custom handler computes target line and positions cursor at closest X position. |
| **Key JS Methods** | `_handleArrowNavigation(range, direction)` (~60 lines): finds current line index, checks if adjacent line has tabs, gets cursor X position, binary-searches for closest position on target line |
| **Key Java API** | None (client-side only) |
| **Custom Blots** | None (depends on TabBlot being present to trigger the issue) |
| **Events** | None |
| **RTE 2 Support** | **NONE** |
| **Migration Path** | **CUSTOM** — Port arrow navigation handler. Add keyboard bindings for ArrowUp/ArrowDown. |
| **Dependencies** | Tabstops (Feature 1) — only needed when tabs are present |
| **Security Notes** | None |
| **Effort** | **LOW-MEDIUM** |

---

## Summary Table

| # | Feature | RTE 2 | Migration | Effort | Tier |
|---|---------|:-----:|-----------|:------:|:----:|
| 1 | Tabstops (L/R/M alignment) | NONE | CUSTOM | HIGH | 1 |
| 2 | Rulers (horizontal + vertical) | NONE | CUSTOM | MEDIUM | 1 |
| 3 | Soft-Break (Shift+Enter + tab copy) | NONE | CUSTOM | MEDIUM | 1 |
| 4 | Readonly Sections (inline) | NONE | CUSTOM | MEDIUM | 2 |
| 5 | Placeholders (embed + dialog) | NONE | CUSTOM | HIGH | 2 |
| 6 | Non-Breaking Space (Shift+Space) | NONE | CUSTOM | LOW | 3 |
| 7 | Whitespace Indicators | NONE | CUSTOM | LOW-MED | 3 |
| 8 | Toolbar Slot System (24 slots) | NONE | CUSTOM | HIGH | 1 |
| 9 | Toolbar Button Visibility | PARTIAL | CUSTOM | MEDIUM | 2 |
| 10 | Custom Keyboard Shortcuts | NONE | CUSTOM | MEDIUM | 2 |
| 11 | HTML Sanitization | PARTIAL | CUSTOM (extend) | LOW | 3 |
| 12 | I18n (32 strings) | PARTIAL | CUSTOM (additive) | LOW | 3 |
| 13 | Theme Variants | NATIVE | NATIVE + CSS | LOW | 0 |
| 14 | Programmatic Text Insertion | NONE | CUSTOM (Java API) | LOW | 3 |
| 15 | Value Change Mode | NATIVE | INHERIT | NONE | 0 |
| 16 | extendOptions Hook | NONE | CUSTOM | HIGH | 1 |
| 17 | List Indentation Buttons | PARTIAL | PARTIAL | LOW-MED | 3 |
| 18 | Align Justify | NONE | CUSTOM (button) | LOW | 3 |
| 19 | Replace Toolbar Button Icons | NONE | CUSTOM | LOW | 3 |
| 20 | Arrow Navigation | NONE | CUSTOM | LOW-MED | 3 |

### Migration Priority Tiers

- **Tier 0 (Free/Inherited):** Features 13, 15 (+ Feature 18 from RTE 2: Text Color/Background)
- **Tier 1 (Must Have — Core Differentiators):** Features 1, 2, 3, 8, 16
- **Tier 2 (Must Have — Important):** Features 4, 5, 9, 10
- **Tier 3 (Nice to Have):** Features 6, 7, 11, 12, 14, 17, 18, 19, 20

---

## V25 Migration Risk: Overlay/Popup Parent Re-Attachment

### Problem

Several ERTE toolbar popup components manually manipulate overlay DOM positioning via `getElement().executeJs()` and `Element.appendChild()`. In Vaadin 25, the overlay architecture changed — overlays no longer use the same `this.$.overlay.$.overlay` internal structure, and manual `appendChild` of Vaadin components into arbitrary parents may break.

### Affected Components (all in `com.vaadin.componentfactory.toolbar`)

#### 1. `ToolbarPopover` (extends `Popover`)
**File:** `enhanced-rich-text-editor/src/.../toolbar/ToolbarPopover.java`
**Pattern:** On attach, appends itself as child of the switch's **parent** element:
```java
referencedSwitch.addAttachListener(event -> {
    event.getSource().getParent().orElseThrow().getElement().appendChild(getElement());
});
```
Also sets `restoreFocusOnClose` via direct property access (commented-out API: `setRestoreFocusOnClose()` — already broken in V24).
**Used by:** `EnhancedRichTextEditorTables` — "Add Table" popup (rows x cols input).

#### 2. `ToolbarDialog` (extends `Dialog`)
**File:** `enhanced-rich-text-editor/src/.../toolbar/ToolbarDialog.java`
**Pattern:** Positions overlay absolutely relative to switch via direct `$.overlay.$.overlay.style` manipulation:
```java
getElement().executeJs("""
    const {left, top, width, height} = $0.getBoundingClientRect();
    this.$.overlay.$.overlay.style.position = 'absolute';
    this.$.overlay.$.overlay.style.left = left + 'px';
    this.$.overlay.$.overlay.style.top = top + height + 'px';""",
    getToolbarSwitch());
```
**Used by:** `TemplateDialog` (table styling).

#### 3. `ToolbarSelectPopup` (extends `ContextMenu`)
**File:** `enhanced-rich-text-editor/src/.../toolbar/ToolbarSelectPopup.java`
**Pattern:** Uses `ContextMenu(target)` + `setOpenOnClick(true)`. ContextMenu internally creates `vaadin-context-menu-overlay` which teleports to body.
**Used by:** `EnhancedRichTextEditorTables` — "Modify Table" dropdown (row/column/merge operations).

#### 4. `TemplateDialog` (extends `ToolbarDialog`)
**File:** `enhanced-rich-text-editor-tables/src/.../templates/TemplateDialog.java`
**Pattern:** Additional `$.overlay.$.overlay.style` positioning on open (same as ToolbarDialog, but with `left + width` for side-by-side placement).

### V25 Breaking Changes Expected

1. **`this.$.overlay.$.overlay`** — Vaadin 25 Dialog/Overlay internal structure may differ. The `$` ID-based lookup is Polymer; Lit-based components in V25 may use different internal structure.
2. **`Element.appendChild(getElement())`** — Re-parenting a Vaadin server-side component's element to an arbitrary parent can break Flow's component tree tracking.
3. **`Popover` API** — `restoreFocusOnClose` was already broken in V24 (manually set via property). V25 Popover may have different API.
4. **`ContextMenu` overlay** — V25 may change how context menu overlays are positioned/attached.

### Recommendation

These 4 classes must be **rewritten for V25** using the new overlay/popover APIs. Investigate:
- V25 `Popover` target/positioning API (may natively support anchor-based positioning)
- V25 `Dialog` positioning API (may replace manual `$.overlay` manipulation)
- V25 `ContextMenu` — check if `setOpenOnClick` + target pattern still works
- Whether `Element.appendChild` for re-parenting is still valid in Flow 7 / Vaadin 25

**Priority:** Critical for tables extension migration (Step 4). Also affects ERTE core (link dialog, placeholder dialog use `vaadin-confirm-dialog` in the JS template — different pattern but worth verifying).

### Spike Validation Cross-Reference

| Spike Item | Feature(s) | Key Finding |
|-----------|-----------|-------------|
| Item 2 | 1 | TabBlot (Embed) registers via `Quill.register()` |
| Item 3 | 5 | Custom embed insert via `new Delta().insert({ tab: true })` |
| Item 4 | 8, 2, 17, 18 | Toolbar button injection via `render()` override |
| Item 5 | 8 | Custom toolbar buttons survive ALL re-renders |
| Item 6 | 10 | Keyboard module accessible: `this._editor.keyboard` |
| Item 7 | 3, 10 | Binding priority: `addBinding()` appends to END |
| Item 10 | 16 | Module load order: blot registration → element definition → ready |
| Item 11 | 13 | Lumo via `@StyleSheet(Lumo.STYLESHEET)` |
| Item 12 | 13 | `::part()` selectors work, tag-specific isolation |
| Item 13 | 4 | setReadOnly observer works on extended component |
| Item 14 | All | Lifecycle: `_editor` available after `super.ready()` |
| Item 19 | 11 | Sanitizer strips `class` — override or use Delta API |
| Item 20 | 1 | Guard nodes INSIDE embed — measure OUTER .ql-tab rect |
| Item 21 | 1, 3, 5 | Delta round-trip perfect: embeds preserved |

### Security Fixes Required During Migration

| Feature | Issue | Fix |
|---------|-------|-----|
| 5 (Placeholders) | `node.innerHTML = text` (XSS) | Use `textContent` for text, `createElement`/`appendChild` for DOM |
| 3 (Soft-Break) | `node.innerHTML = '<br>'` | Use `createElement('br')` + `appendChild()` |
| 11 (Sanitizer) | Unrestricted `style` attribute | Restrict allowed style properties |
| 11 (Sanitizer) | `data:` protocol without MIME check | Restrict to `data:image/*` only |
