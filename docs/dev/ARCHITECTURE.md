# ERTE V25 Architecture

How ERTE is built internally. Read this if you want to understand the codebase before making changes — it covers the three-layer architecture, module structure, and key design decisions. For practical extension patterns, see [`EXTENDING.md`](./EXTENDING.md).

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

The most important rule: **never copy RTE 2 source code.** ERTE extends at runtime via ES class inheritance (JS) and Java class inheritance (Java). This keeps ERTE updatable — when Vaadin ships a new RTE version, ERTE picks up the improvements automatically. The only "fork" is the `render()` passthrough in the JS subclass.

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

Using `customElements.get()` instead of a direct import keeps ERTE decoupled from RTE 2's internal module paths. Direct ES class extension means ERTE participates fully in the Lit lifecycle — no hacks, no side-channel patching.

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

**Quill 2 Embed guard nodes — this is a common gotcha.** Quill 2 places invisible zero-width characters (`\uFEFF`) inside Embed domNodes so the cursor can sit next to them. If you set `contenteditable="false"` on the outer domNode, those guard nodes become non-editable too, and the cursor can't be placed before or after the embed. The inner `contentNode` already has `contenteditable="false"` — that's all you need. See also the [guard node rules](EXTENDING.md#embed-blot-lifecycle) in the extension guide.

### Toolbar Slot Injection

ERTE injects `<slot>` elements into RTE 2's toolbar DOM after `ready()`. Injected slots survive all Lit re-renders (i18n changes, readonly toggle, `requestUpdate`) because Lit ignores DOM nodes between its comment markers. No template copy needed, updatability preserved.

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

ERTE overrides `static get lumoInjector()` to reuse the parent's tag name (`vaadin-rich-text-editor`). This is easy to miss — without it, ERTE falls back to base SVG toolbar icons instead of Lumo's font-based icons.

## Java Layer

All ERTE logic lives in `EnhancedRichTextEditor`, which extends Vaadin's `RichTextEditor`.

**Package structure:**
- `com.vaadin.componentfactory` — All ERTE code (API, toolbar helpers, events, data classes)

`EnhancedRichTextEditor` extends `RichTextEditor` directly.

### Dual-Layer Sanitizer

- **Server-side (`erteSanitize()`)**: jsoup Safelist extended with ERTE classes, attributes, and safe CSS properties. Called on `setPresentationValue()`.
- **Client-side (`__updateHtmlValue()`)**: Preserves ERTE classes during the Quill → HTML → server round-trip.

Both layers must agree on which classes are allowed — if you add a class to one side but forget the other, content will either be stripped on save or not preserved on load. See [EXTENDING.md — Sanitizer Integration](./EXTENDING.md#sanitizer-integration) for how to add custom classes.

### Value Format

HTML-primary (matching RTE 2). `setValue()`/`getValue()` work with HTML strings. Delta access via `asDelta()` wrapper for programmatic readonly, placeholders, tabs, and batch updates.

## Key Source Files

| File | Path (within `enhanced-rich-text-editor/`) | Purpose |
|------|------|---------|
| `vcf-enhanced-rich-text-editor.js` | `src/main/resources/META-INF/resources/frontend/` | Web component, blots, toolbar injection, i18n |
| `vcf-enhanced-rich-text-editor-styles.css` | `src/main/resources/META-INF/resources/frontend/styles/` | CSS custom properties, blot styles, slot styling |
| `EnhancedRichTextEditor.java` | `src/main/java/com/vaadin/componentfactory/` | Public API, events, configuration, sanitizer |
| `toolbar/ToolbarSlot.java` | `src/main/java/com/vaadin/componentfactory/toolbar/` | Enum of toolbar slot positions |
| `toolbar/ToolbarSwitch.java` | `src/main/java/com/vaadin/componentfactory/toolbar/` | Toggle button helper |
| `toolbar/ToolbarPopover.java` | `src/main/java/com/vaadin/componentfactory/toolbar/` | Dropdown panel helper |
| `toolbar/ToolbarDialog.java` | `src/main/java/com/vaadin/componentfactory/toolbar/` | Dialog helper |
| `toolbar/ToolbarSelectPopup.java` | `src/main/java/com/vaadin/componentfactory/toolbar/` | Context menu helper |
| `SlotUtil.java` | `src/main/java/com/vaadin/componentfactory/` | Toolbar slot injection utilities |
| `TabConverter.java` | `src/main/java/com/vaadin/componentfactory/` | Delta ↔ Tab value conversion |
| `Placeholder.java` | `src/main/java/com/vaadin/componentfactory/` | Placeholder data class |
| `TabStop.java` | `src/main/java/com/vaadin/componentfactory/` | TabStop data class |

---

**See also:** [`EXTENDING.md`](./EXTENDING.md) for extension patterns, [User Guide](../BASE_USER_GUIDE.md) for features.
