# ERTE V25 Architecture

Internal architecture of the Enhanced Rich Text Editor (ERTE) V25. For practical extension patterns, see [`EXTENDING.md`](./EXTENDING.md). For public API, see [`API_REFERENCE.md`](../../enhanced-rich-text-editor/docs/API_REFERENCE.md).

---

## Three-Layer Architecture

ERTE extends Vaadin 25's Rich Text Editor (RTE 2, built on Quill 2 and Parchment 3) with custom blots, toolbar slots, and sanitization:

```
┌──────────────────────────────────────────────────────────────────┐
│  Application                                                       │
├──────────────────────────────────────────────────────────────────┤
│  Java Layer: EnhancedRichTextEditor (com.vaadin.componentfactory) │
│  ├─ Public API (toolbar, placeholders, tabstops, etc.)            │
│  ├─ Event listeners & configuration                              │
│  └─ Server-side sanitizer (ERTE extensions)                      │
├──────────────────────────────────────────────────────────────────┤
│  JavaScript Layer: vcf-enhanced-rich-text-editor.js              │
│  ├─ Quill 2 blots (Tab, Placeholder, Readonly, etc.)            │
│  ├─ Toolbar slot DOM injection                                   │
│  └─ Client-side value preservation & i18n                        │
├──────────────────────────────────────────────────────────────────┤
│  RTE 2 (Vaadin's web component, Quill 2 + Parchment 3)           │
│  └─ Core editor engine                                           │
└──────────────────────────────────────────────────────────────────┘
```

## Module Structure

- **`enhanced-rich-text-editor/`** — Core addon module
  - Java classes in `com.vaadin.componentfactory` (all ERTE logic)
  - JavaScript frontend in `frontend/` (2651 lines)
  - CSS custom properties in `frontend/styles/vcf-enhanced-rich-text-editor-styles.css`
  - Compiled as `.jar` artifact in Maven build

- **`enhanced-rich-text-editor-tables/`** — Tables addon (extends core ERTE with table support)

- **`enhanced-rich-text-editor-demo/`** — Demo application
  - Spring Boot application with sample views (ErteSamplesView, ErtePlaygroundView, etc.)
  - Prototype tests

- **`enhanced-rich-text-editor-it/`** — Integration tests
  - Test views (Java, package `com.vaadin.componentfactory`)
  - Playwright test suite (see TEST_INVENTORY.md)
  - Runs on port 8081

## JavaScript Layer

File: `vcf-enhanced-rich-text-editor.js` (2651 lines)

### Web Component Extension

```javascript
const RteBase = customElements.get('vaadin-rich-text-editor');
class VcfEnhancedRichTextEditor extends RteBase {
  static get is() { return 'vcf-enhanced-rich-text-editor'; }
  // ...
}
customElements.define('vcf-enhanced-rich-text-editor', VcfEnhancedRichTextEditor);
```

Design rationale: `customElements.get()` decouples from RTE 2's internals, direct ES class extension ensures full lifecycle participation, stable import path (`@vaadin/rich-text-editor`).

### Key Lifecycle Methods

- **`render()`** (line 438) — Passes through `super.render()`. No template override.
- **`ready()`** (line 511) — Vaadin lifecycle hook. Injects toolbar slots, initializes blots, registers keyboard bindings, sets up property observers.

### Blot Registration

Five custom blots registered globally via `Quill.register()` before element creation (lines 55–379):

| Blot | Type | Purpose | HTML Tag |
|------|------|---------|----------|
| **ReadOnlyBlot** | Inline | Mark text as read-only | `<span class="ql-readonly">` |
| **TabBlot** | Embed | Tabstop position | `<span class="ql-tab">` |
| **SoftBreakBlot** | Embed | Soft line break (Shift+Enter) | `<span class="ql-soft-break"><br></span>` |
| **PlaceholderBlot** | Embed | Configurable placeholder token | `<span class="ql-placeholder">` |
| **NbspBlot** | Embed | Non-breaking space (Shift+Space) | `<span class="ql-nbsp">` |

**Guard nodes:** Quill 2 places zero-width guard TextNodes (`\uFEFF`) inside the Embed's domNode. Never set `contenteditable="false"` on outer domNode — guard nodes must remain editable. See [EXTENDING.md](EXTENDING.md#embed-blot-gotchas).

### Toolbar Slot System

ERTE injects 27 `<slot>` elements into RTE 2's toolbar after `ready()` completes via `_injectToolbarSlots()` (line 1069):

- **START** and **END** — outermost slots
- **BEFORE/AFTER_GROUP_*** — 27 slots across 11 groups (history, emphasis, style, heading, glyph-transformation, list, indent, alignment, rich-text, block, format, custom)

Injected slots survive all Lit re-renders (i18n, readonly, `requestUpdate`) because Lit ignores nodes between comment markers. Persist for lifetime once injected.

### Client-Side Value Preservation

ERTE completely overrides `__updateHtmlValue()` (line 1840) to preserve ERTE classes that RTE 2 would strip:

1. Reads HTML from `this._editor.getSemanticHTML()`
2. Filters classes: keeps non-`ql-` classes, `ql-align-*`, `ql-indent-*`, and ERTE classes
3. Processes via `this.__processQuillClasses(content)` and `this._setHtmlValue(content)`

```javascript
__updateHtmlValue() {
  let content = this._editor.getSemanticHTML();
  content = content.replace(/class="([^"]*)"/gu, (_match, group1) => {
    const classes = group1.split(' ').filter((className) => {
      if (!className.startsWith('ql-')) return true;
      if (className.startsWith('ql-align') || className.startsWith('ql-indent')) return true;
      if (ERTE_PRESERVED_CLASSES.includes(className)) return true;
      return false;
    });
    return `class="${classes.join(' ')}"`;
  });
  content = this.__processQuillClasses(content);
  this._setHtmlValue(content);
}
```

The `ERTE_PRESERVED_CLASSES` array (line 385) contains: `'ql-readonly'`, `'ql-tab'`, `'ql-soft-break'`, `'ql-placeholder'`, `'ql-nbsp'`, `'ql-editor__table--hideBorder'`. The server-side list additionally includes `'td-q'`.

## Java Layer

All ERTE logic lives in `EnhancedRichTextEditor` (1799 lines), which extends Vaadin's `RichTextEditor`.

**Major nested types:**
- **`ToolbarButton` enum** (lines 679–724) — 30 buttons (25 standard + 5 ERTE-specific)
- **`ToolbarSlot` enum** — 27 slots (see `toolbar/ToolbarSlot.java`)
- **Event classes** (lines 1226+) — `PlaceholderBeforeInsertEvent`, `PlaceholderInsertedEvent`, etc.

### Sanitizer (Dual-Layer)

- **Server-side (`erteSanitize()`, lines 223–480)**: jsoup Safelist + post-filters for ERTE classes (`ql-readonly`, `ql-tab`, `ql-soft-break`, `ql-placeholder`, `ql-nbsp`, `td-q`, `ql-editor__table--hideBorder`) and standard Quill classes (`ql-align-*`, `ql-indent-*`). Validates styles and `data:` URLs.

- **Client-side (`__updateHtmlValue()` in JS)**: Preserves ERTE classes during model→presentation cycle.

### Lumo Theme Integration

JS overrides `static get lumoInjector()` (line 428) to reuse parent's tag name for consistent toolbar icons and colors.

## Toolbar Slot System (27 Slots, 11 Groups)

```
START
├─ BEFORE_GROUP_HISTORY, [undo, redo], AFTER_GROUP_HISTORY
├─ BEFORE_GROUP_EMPHASIS, [bold, italic, underline, strike], AFTER_GROUP_EMPHASIS
├─ BEFORE_GROUP_STYLE, [color, background], AFTER_GROUP_STYLE  ← NEW in V25
├─ BEFORE_GROUP_HEADING, [h1, h2, h3], AFTER_GROUP_HEADING
├─ BEFORE_GROUP_GLYPH_TRANSFORMATION, [subscript, superscript], AFTER_GROUP_GLYPH_TRANSFORMATION
├─ BEFORE_GROUP_LIST, [ordered, bullet], AFTER_GROUP_LIST
├─ BEFORE_GROUP_INDENT, [outdent, indent], AFTER_GROUP_INDENT
├─ BEFORE_GROUP_ALIGNMENT, [left, center, right], AFTER_GROUP_ALIGNMENT
├─ BEFORE_GROUP_RICH_TEXT, [image, link], AFTER_GROUP_RICH_TEXT
├─ BEFORE_GROUP_BLOCK, [blockquote, code-block], AFTER_GROUP_BLOCK
├─ BEFORE_GROUP_FORMAT, [clean], AFTER_GROUP_FORMAT
├─ BEFORE_GROUP_CUSTOM, [toolbar custom group], AFTER_GROUP_CUSTOM
END
```

Custom components use part name `toolbar-custom-component`, enabling CSS styling via `::slotted([part~='toolbar-custom-component'])`.

## Value Format

ERTE V25 adopts RTE 2's **HTML-primary format** (unlike ERTE V24's Delta-primary):
- `setValue(String html)` and `getValue()` return HTML strings
- `asDelta()` wrapper provides Delta access via Quill on the client

## Sanitizer Architecture

### Server-Side (`erteSanitize()`)

Called on `setPresentationValue()`:

1. jsoup Safelist with extensions: `img`, `h1`–`h3`, `s`, `style`/`class` on all elements, `span[contenteditable, aria-readonly, data-placeholder]`, `img[align, alt, height, src, title, width]` with safe protocols
2. Class whitelist: `ql-align-*`, `ql-indent-*`, ERTE classes
3. Style whitelist: safe properties only (color, font-*, text-*, padding/margin, border, display, etc.)
4. Data URL validation: safe image MIME types only (no SVG)
5. `contenteditable="false"` only

### Client-Side (`__updateHtmlValue()` in JS)

Completely overrides RTE 2's method to preserve ERTE classes during model→presentation cycle (see [Client-Side Value Preservation](#client-side-value-preservation)).

## Custom CSS Properties (22 Total)

| Category | Properties |
|----------|-----------|
| **Readonly** (6) | `--vaadin-erte-readonly-{color, background, border-color, border-width, border-radius, padding}` |
| **Placeholder** (6) | `--vaadin-erte-placeholder-{color, background, border-color, border-width, border-radius, padding}` |
| **Whitespace** (3) | `--vaadin-erte-whitespace-{indicator-color, paragraph-indicator-color, indicator-spacing}` |
| **Ruler** (7) | `--vaadin-erte-ruler-{height, border-color, background, marker-size, marker-color, vertical-width, vertical-background}` |

Toolbar buttons inherit from RTE 2's `--vaadin-rich-text-editor-toolbar-button-*` properties.

## Lumo Theme Injection

ERTE overrides `static get lumoInjector()` (line 428) to reuse parent's tag name (`vaadin-rich-text-editor`), ensuring Lumo theme CSS applies to ERTE's toolbar. Without this, ERTE would get base SVG icons instead of Lumo's text-based icons.

## Key Source Files

| File | Lines | Purpose |
|------|-------|---------|
| `vcf-enhanced-rich-text-editor.js` | 2651 | Web component, blots, toolbar injection, i18n |
| `EnhancedRichTextEditor.java` | 1799 | Public API, events, configuration, sanitizer |
| `ToolbarSlot.java` | 117 | Enum of 27 toolbar slots |
| `ToolbarSwitch.java` | 144 | Toggle button helper (extends Button) |
| `ToolbarPopover.java` | 132 | Dropdown panel helper (extends Popover) |
| `ToolbarDialog.java` | 230 | Modal dialog helper (extends Dialog) |
| `ToolbarSelectPopup.java` | 59 | Context menu helper (extends ContextMenu) |
| `SlotUtil.java` | 168 | Toolbar slot injection utilities |
| `TabConverter.java` | 219 | Delta ↔ Tab value conversion |
| `Placeholder.java` | 158 | Placeholder data class |
| `TabStop.java` | 77 | TabStop data class |
| `vcf-enhanced-rich-text-editor-styles.css` | 355 | CSS custom properties, blot styles, slots |

---

**See also:** [`EXTENDING.md`](./EXTENDING.md) for extension patterns, [`API_REFERENCE.md`](../../enhanced-rich-text-editor/docs/API_REFERENCE.md) for public API, [`USER_GUIDE.md`](../../enhanced-rich-text-editor/docs/USER_GUIDE.md) for features.
