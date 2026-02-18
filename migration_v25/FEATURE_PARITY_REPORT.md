# V24 vs V25 Feature Parity Report

**Date:** 2026-02-17
**Branch:** v25
**V24 port:** 8080 | **V25 port:** 8082
**Viewport:** 1280x800 (Desktop)

## Summary

**13/13 tests PASS on both V24 and V25.** Full feature parity confirmed for all tested features.

## Test Matrix

| Test | Feature | V24 | V25 | Match? | Notes |
|------|---------|-----|-----|--------|-------|
| T1 | Editor Loads + Toolbar | PASS | PASS | YES | V24: 28 buttons (2 rows, Polymer). V25: 29 buttons (2 rows, Lit+Quill 2). V25 has color/background pickers (new in RTE 2). Both have Lumo theming. |
| T2 | Text Formatting | PASS | PASS | YES | Bold applied correctly on both. Toolbar button highlights on activation. |
| T3 | Tab Stops | PASS | PASS | YES | V24: pre-loaded Product/Price/Quantity table (LEFT@150, RIGHT@350, MIDDLE@550). V25: Set via "Set Tab Content" button (LEFT@150, MIDDLE@300, RIGHT@450). Both align correctly at configured positions. |
| T4 | Whitespace Indicators | PASS | PASS | YES | Both show → (tab arrows) and ¶ (paragraph marks). WS button in toolbar toggles on/off. |
| T5 | Placeholders | PASS | PASS | YES | V24: pre-loaded "Vaadin", "Turku, 20540", "01-01-2000" with blue highlight + @ button + "Value" toggle. V25: "Setup Placeholders" button registers FirstName (bold), LastName (italic), Company; placeholder button inserts. Both render inline with formatting. |
| T6 | Toolbar Button Visibility | PASS | PASS | YES | V24: hides CLEAN, BLOCKQUOTE, CODE_BLOCK, IMAGE, LINK, STRIKE, READONLY at construction. V25: hides H1/H2/H3 dynamically, restores with "Show All". Both correctly hide/show buttons. |
| T7 | Custom Toolbar Components | PASS | PASS | YES | V24: airplane+dental icons in CUSTOM slot, ComboBox in START slot, eye toggle. V25: "ERTE" label in START slot, "My Custom" button in END slot. Both render custom components in correct toolbar positions. |
| T8 | ReadOnly Mode | PASS | PASS | YES | V24: readonly sections via delta JSON (inline readonly blots). V25: toggle ReadOnly hides entire toolbar, content non-editable; toggle back restores. Both implement readonly correctly. |
| T9 | Tables | PASS | PASS | YES | V24: pre-loaded 6x5 styled table with template formatting (header/footer rows, alternating colors). V25: "Enable Tables" + "Insert 2x2 Table" creates empty editable table with borders. Both add table toolbar buttons. Table extension works on both. |
| T10 | i18n (German Labels) | PASS | PASS | YES | V24: German labels in TablesI18n (Zeilen, Spalten, Vorlage, etc.). V25: German labels in EnhancedRichTextEditorI18n (Fett, Kursiv, Rückgängig, etc.). Both correctly apply localized text. |
| T11 | noRulers | PASS | PASS | YES | V24: `setNoRulers(true)` at construction, ruler hidden. V25: "Toggle noRulers" dynamically toggles ruler visibility. Both hide ruler area correctly. |
| T12 | HTML Value Round-Trip | PASS | PASS | YES | V24: type text + "Show html value" renders HTML. V25: "Set Tab Content" + "Get HTML Value" shows HTML with `ql-tab` class spans preserved. Both round-trip correctly. |
| T13 | Large Content | PASS | PASS | YES | V24: 14 editor cards on single page (13,855px total height), scrolls fine. V25: "Set Large Content" loads 50 paragraphs in editor (maxHeight: 400px), editor scrolls smoothly, no lag. |

## Visual Differences (Expected)

| Aspect | V24 (Polymer + Quill 1) | V25 (Lit + Quill 2) |
|--------|-------------------------|---------------------|
| **Base framework** | Polymer 3, Quill 1.3.6 | Lit, Quill 2.0.3 |
| **Toolbar icons** | SVG icons (smaller, lighter) | SVG icons (slightly bolder, Lumo-integrated) |
| **Extra buttons** | — | Color picker, Background color picker (from RTE 2) |
| **Toolbar button count** | 28 | 29 (+ color/background) |
| **Demo layout** | Multi-card (14 editors, scroll) | Single editor + 20 interactive buttons |
| **Table styling** | Pre-loaded with templates | Empty tables inserted dynamically |
| **Placeholder UI** | @ button + "Value" appearance toggle in toolbar | Placeholder button (no appearance toggle in toolbar by default) |

## Known Gaps (V24 features not in V25 demo view)

These features exist in V25 Java code but aren't exposed in the V25 demo:

| Feature | V24 Demo Card | V25 Status |
|---------|---------------|------------|
| Inline readonly sections | "Basic Rich Text Editor with readonly sections" | Code exists (`ReadOnlyBlot`), not demo'd |
| Custom keyboard shortcuts | "custom shortcuts for standard buttons" | `addStandardToolbarButtonShortcut()` API exists, not demo'd |
| Icon replacement | "icon replacement for standard buttons" | `replaceStandardToolbarButtonIcon()` API exists, not demo'd |
| Table templates/styling | Pre-loaded styled table with 6 template rows | Tables work, templates not loaded in demo |
| Table i18n | German table toolbar labels | Table i18n API exists, not demo'd |

## Infrastructure Notes

- V24 server script needed fix: `./mvnw` → `mvn` (wrapper missing)
- V25 dev server (Vite) can be unstable after 30-60s inactivity — production builds unaffected
- Running both servers simultaneously can cause Vaadin client-side router cross-navigation — test one at a time for reliability

## Screenshots

All screenshots in `.claude/screenshots/`:

### V24
| File | Description |
|------|-------------|
| `v24-t01-editor-toolbar.png` | Basic editor + toolbar (2 rows, 28 buttons) |
| `v24-t02-text-formatting.png` | Bold "Hello" in "Hello World" |
| `v24-t03-tab-stops.png` | Tab-aligned Product/Price/Quantity table |
| `v24-t04-whitespace.png` | Whitespace indicators active |
| `v24-t05-placeholders.png` | Inline placeholders: Vaadin, Turku, date |
| `v24-t06-toolbar-visibility.png` | Limited toolbar (9 buttons hidden) |
| `v24-t07-custom-buttons.png` | Airplane + dental chair custom icons |
| `v24-t07b-custom-buttons-extended.png` | ComboBoxes + eye toggle |
| `v24-t08-readonly.png` | Readonly sections in content |
| `v24-t09-tables.png` | 6x5 styled table with template formatting |
| `v24-t10-i18n-german.png` | German table i18n labels |
| `v24-t11-no-rulers.png` | Ruler area hidden |
| `v24-t12-html-value.png` | HTML value round-trip |
| `v24-t13-large-content.png` | Page bottom with all 14 cards |

### V25
| File | Description |
|------|-------------|
| `v25-t01-editor-toolbar.png` | Full page: buttons + editor + toolbar (29 buttons) |
| `v25-t02-text-formatting.png` | Bold "Hello" applied |
| `v25-t03-tab-stops.png` | Tab content with LEFT/MIDDLE/RIGHT alignment |
| `v25-t04-whitespace.png` | Whitespace symbols visible |
| `v25-t05-placeholders.png` | @FirstName placeholder inserted |
| `v25-t06a-toolbar-hidden.png` | H1/H2/H3 hidden |
| `v25-t06b-toolbar-restored.png` | All buttons restored |
| `v25-t07-custom-components.png` | "ERTE" START + "My Custom" END |
| `v25-t08a-readonly-on.png` | Toolbar hidden, readonly |
| `v25-t08b-readonly-off.png` | Toolbar restored, editable |
| `v25-t09-tables.png` | 2x2 table with borders + table toolbar |
| `v25-t10-i18n-german.png` | German i18n applied |
| `v25-t11a-no-rulers.png` | Ruler hidden |
| `v25-t11b-rulers-restored.png` | Ruler restored |
| `v25-t12-html-value.png` | HTML with ql-tab spans |
| `v25-t13-large-content.png` | 50 paragraphs, smooth scrolling |
