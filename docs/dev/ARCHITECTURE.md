# ERTE V25 Architecture

How ERTE is built internally. Read this if you want to understand the codebase before making changes. For practical extension patterns, see [`EXTENDING.md`](./EXTENDING.md).

---

## Three-Layer Architecture

ERTE extends Vaadin 25's Rich Text Editor (RTE 2, built on Quill 2 and Parchment 3):

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

Key constraint: **never copy RTE 2 source code.** ERTE extends at runtime via ES class inheritance (JS) and Java class inheritance (Java). The only "fork" is the `render()` passthrough in the JS subclass.

## Module Structure

| Module | Purpose |
|--------|---------|
| `enhanced-rich-text-editor/` | Core addon (Java API + JS web component + CSS) |
| `enhanced-rich-text-editor-tables/` | Tables addon (extends core ERTE with table blots + template dialog) |
| `enhanced-rich-text-editor-demo/` | Demo application (sample views, prototype tests) |
| `enhanced-rich-text-editor-it/` | Integration tests (test views + Playwright specs) |

## JavaScript Layer

### Web Component Extension

```javascript
const RteBase = customElements.get('vaadin-rich-text-editor');
class VcfEnhancedRichTextEditor extends RteBase {
  static get is() { return 'vcf-enhanced-rich-text-editor'; }
}
customElements.define('vcf-enhanced-rich-text-editor', VcfEnhancedRichTextEditor);
```

`customElements.get()` decouples from RTE 2's import path. Direct ES class extension ensures full Lit lifecycle participation.

### Lifecycle

- **`render()`** — Passes through `super.render()`. No template override (preserves updatability).
- **`ready()`** — Injects toolbar slots, initializes blots, registers keyboard bindings, sets up property observers. The Quill editor instance (`this._editor`) is available immediately after `super.ready()`.

### Custom Blots

Five blots registered globally via `Quill.register()` before element creation:

| Blot | Type | CSS Class | Purpose |
|------|------|-----------|---------|
| ReadOnlyBlot | Inline | `.ql-readonly` | Protect text from editing |
| TabBlot | Embed | `.ql-tab` | Tabstop-aligned whitespace |
| SoftBreakBlot | Embed | `.ql-soft-break` | Line break within paragraph |
| PlaceholderBlot | Embed | `.ql-placeholder` | Configurable placeholder token |
| NbspBlot | Embed | `.ql-nbsp` | Non-breaking space |

**Quill 2 Embed guard nodes:** Quill 2 places zero-width guard characters (`\uFEFF`) inside Embed domNodes. Never set `contenteditable="false"` on the outer domNode — guard nodes must remain editable for cursor placement. The inner `contentNode` already has `contenteditable="false"`. See [EXTENDING.md](EXTENDING.md) for details.

### Toolbar Slot Injection

ERTE injects `<slot>` elements into RTE 2's toolbar DOM after `ready()`. Injected slots survive all Lit re-renders (i18n changes, readonly toggle, `requestUpdate`) because Lit ignores DOM nodes between its comment markers.

```
START
├─ [BEFORE/AFTER_GROUP_HISTORY] [undo, redo]
├─ [BEFORE/AFTER_GROUP_EMPHASIS] [bold, italic, underline, strike]
├─ [BEFORE/AFTER_GROUP_STYLE] [color, background]
├─ [BEFORE/AFTER_GROUP_HEADING] [h1, h2, h3]
├─ ... (glyph-transformation, list, indent, alignment, rich-text, block, format)
├─ [BEFORE/AFTER_GROUP_CUSTOM] [custom group]
END
```

### Client-Side Value Preservation

ERTE overrides `__updateHtmlValue()` to preserve ERTE CSS classes that RTE 2's built-in method would strip. A class whitelist (`ERTE_PRESERVED_CLASSES`) controls which `ql-*` classes survive the model-to-presentation cycle.

### Lumo Theme Injection

ERTE overrides `static get lumoInjector()` to reuse the parent's tag name (`vaadin-rich-text-editor`). Without this, ERTE would get base SVG toolbar icons instead of Lumo's font-based icons.

## Java Layer

All ERTE logic lives in `EnhancedRichTextEditor`, which extends Vaadin's `RichTextEditor`.

**Package structure:**
- `com.vaadin.flow.component.richtexteditor` — `RteExtensionBase` only (bridge class that lifts package-private RTE 2 methods to protected)
- `com.vaadin.componentfactory` — Everything else (ERTE API, toolbar helpers, events, data classes)

Only one class lives in the foreign package. This is the minimal bridge needed to access RTE 2 internals.

### Dual-Layer Sanitizer

- **Server-side (`erteSanitize()`)**: jsoup Safelist extended with ERTE classes, attributes, and safe CSS properties. Called on `setPresentationValue()`.
- **Client-side (`__updateHtmlValue()`)**: Preserves ERTE classes during the Quill → HTML → server round-trip.

Both layers must agree on which classes are allowed. See [EXTENDING.md — Sanitization Integration](./EXTENDING.md#sanitization-integration) for how to add custom classes.

### Value Format

HTML-primary (matching RTE 2). `setValue()`/`getValue()` work with HTML strings. Delta access via `asDelta()` wrapper for programmatic readonly, placeholders, tabs, and batch updates.

## Key Source Files

| File | Purpose |
|------|---------|
| `vcf-enhanced-rich-text-editor.js` | Web component, blots, toolbar injection, i18n |
| `vcf-enhanced-rich-text-editor-styles.css` | CSS custom properties, blot styles, slot styling |
| `EnhancedRichTextEditor.java` | Public API, events, configuration, sanitizer |
| `RteExtensionBase.java` | Bridge class (package-private → protected) |
| `toolbar/ToolbarSlot.java` | Enum of toolbar slot positions |
| `toolbar/ToolbarSwitch.java` | Toggle button helper |
| `toolbar/ToolbarPopover.java` | Dropdown panel helper |
| `toolbar/ToolbarDialog.java` | Dialog helper |
| `toolbar/ToolbarSelectPopup.java` | Context menu helper |
| `toolbar/SlotUtil.java` | Toolbar slot injection utilities |
| `TabConverter.java` | Delta ↔ Tab value conversion |
| `Placeholder.java` | Placeholder data class |
| `TabStop.java` | TabStop data class |

---

**See also:** [`EXTENDING.md`](./EXTENDING.md) for extension patterns, [`USER_GUIDE.md`](../BASE_USER_GUIDE.md) for features.
