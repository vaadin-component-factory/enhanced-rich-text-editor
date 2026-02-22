# ERTE V25 Architecture

This document describes the internal architecture of the Enhanced Rich Text Editor (ERTE) V25 component. It is intended for developers extending ERTE or contributing to the project.

**Audience:** Java/JavaScript developers, component contributors, maintainers

**Related:** [`EXTENDING.md`](./EXTENDING.md) for practical extension patterns, [`API_REFERENCE.md`](./API_REFERENCE.md) for public API details.

## Overview

ERTE V25 is built on Vaadin 25's Rich Text Editor (RTE 2) component, which is based on Quill 2 and Parchment 3. The architecture uses a three-layer approach:

```
┌──────────────────────────────────────────────────────────────────┐
│  Application                                                       │
├──────────────────────────────────────────────────────────────────┤
│  Java Layer: EnhancedRichTextEditor (com.vaadin.componentfactory) │
│  ├─ Public API (toolbar, placeholders, tabstops, etc.)            │
│  └─ Event listeners & configuration                              │
├──────────────────────────────────────────────────────────────────┤
│  Java Bridge: RteExtensionBase (c.v.f.c.richtexteditor)         │
│  ├─ Visibility lifting (package-private → protected)             │
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

The ERTE V25 project consists of:

- **`enhanced-rich-text-editor-v25/`** — Core addon module (Spring Boot compatible)
  - Java classes in `com.vaadin.componentfactory` (main) and `com.vaadin.flow.component.richtexteditor` (bridge)
  - JavaScript frontend in `frontend/` (2635 lines)
  - CSS custom properties in `frontend/styles/vcf-enhanced-rich-text-editor-styles.css`
  - Compiled as `.jar` artifact in Maven build

- **`enhanced-rich-text-editor-tables-v25/`** — Tables addon (extends core ERTE with table support)
  - Separate module depending on core ERTE
  - Reuses same Quill 2 infrastructure

- **`enhanced-rich-text-editor-demo/`** — Demo application
  - Spring Boot application with test views
  - Playwright test suite (255+ tests)

- **Legacy modules** (`enhanced-rich-text-editor`, `enhanced-rich-text-editor-tables`)
  - V24 reference code (Vaadin 24, Quill 1)
  - Excluded from build, kept for reference only

## JavaScript Layer

**File:** `/workspace/enhanced-rich-text-editor-v25/src/main/resources/META-INF/resources/frontend/vcf-enhanced-rich-text-editor.js` (2635 lines)

### Web Component Extension

The JS layer is a single ES6 class extending RTE 2's web component via `customElements.get()`:

```javascript
const RteBase = customElements.get('vaadin-rich-text-editor');
class VcfEnhancedRichTextEditor extends RteBase {
  static get is() { return 'vcf-enhanced-rich-text-editor'; }
  // ...
}
customElements.define('vcf-enhanced-rich-text-editor', VcfEnhancedRichTextEditor);
```

**Design rationale:**
- Using `customElements.get()` instead of direct import decouples from RTE 2's internal module structure
- Direct ES class extension (not composition or mixin) ensures full lifecycle participation
- Import path (`@vaadin/rich-text-editor`) is stable API as of Vaadin 25.0.5

### Key Lifecycle Methods

- **`render()`** (line 438) — Passes through to `super.render()`. No template override — we extend RTE 2's Lit template, not replace it.
- **`ready()`** (line 511) — Vaadin-specific lifecycle hook (inherited from Polymer compat layer). Called within the Lit update cycle after `connectedCallback` → `willUpdate` → `firstUpdated` → `updated`. Injects toolbar slots, initializes blots, registers keyboard bindings, and sets up property observers.

### Blot Registration

Five custom blots are registered globally via `Quill.register()` **before element creation** (lines 55–379):

| Blot | Type | Purpose | HTML Tag |
|------|------|---------|----------|
| **ReadOnlyBlot** | Inline | Mark text as read-only | `<span class="ql-readonly">` |
| **TabBlot** | Embed | Tabstop position | `<span class="ql-tab">` |
| **SoftBreakBlot** | Embed | Soft line break (Shift+Enter) | `<span class="ql-soft-break"><br></span>` |
| **PlaceholderBlot** | Embed | Configurable placeholder token | `<span class="ql-placeholder">` |
| **NbspBlot** | Embed | Non-breaking space (Shift+Space) | `<span class="ql-nbsp">` |

**Critical pattern:** All Embed blots have `constructor()` implementations that run **after** the parent's Embed constructor creates `contentNode` and guard nodes. Static methods like `create()` run **before** the constructor. This is why lifecycle-dependent setup (like wrapping guard nodes in TabBlot) happens in the constructor, not `create()`.

**Guard nodes:** Quill 2 places zero-width guard TextNodes (`\uFEFF`) **inside** the Embed's domNode to mark logical boundaries. TabBlot wraps these in named `<span class="ql-tab-guard">` elements to make them visible for caret rendering. Never set `contenteditable="false"` on the outer domNode — the inner `contentNode` already has it, and guard nodes must remain editable for cursor placement.

### Toolbar Slot System

RTE 2's toolbar is a Lit template rendered once at element init. ERTE injects 27 `<slot>` elements into the shadow DOM toolbar after `ready()` completes:

- **START** and **END** — outermost slots
- **BEFORE/AFTER_GROUP_*** — 25 slots around 12 button groups (history, emphasis, style, heading, glyph-transformation, list, indent, alignment, rich-text, block, format, custom)

Each slot is injected by `_injectToolbarSlots()` (line 1069), which:
1. Traverses RTE 2's toolbar DOM via `this.shadowRoot.querySelector('[part="toolbar"]')`
2. Inserts `<slot>` elements at the correct positions between button groups
3. Assigns unique names via `slot` attribute

**Critical behavior:** Injected DOM nodes survive all Lit re-renders (i18n change, readonly toggle, `requestUpdate`) because Lit's template diffing ignores nodes inserted between its comment marker boundaries. No re-injection is needed — the slots are injected once in `ready()` and persist for the component's lifetime.

### Client-Side Value Preservation

RTE 2's `__updateHtmlValue()` reads the editor's HTML and strips unknown `ql-*` classes. ERTE **completely overrides** this method (line 1840) — it does not call `super.__updateHtmlValue()`. Instead, it:

1. Reads HTML from `this._editor.getSemanticHTML()`
2. Filters classes via regex: keeps non-`ql-` classes, `ql-align-*`, `ql-indent-*`, and ERTE classes
3. Processes Quill classes via `this.__processQuillClasses(content)`
4. Sets the value via `this._setHtmlValue(content)`

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

The `ERTE_PRESERVED_CLASSES` array (line 385) contains: `'ql-readonly', 'ql-tab', 'ql-soft-break', 'ql-placeholder', 'ql-nbsp'`.

## Java Layer

### Two-Package Bridge Pattern

ERTE uses an unusual two-package pattern to access RTE 2's package-private API:

| Package | Class | Purpose |
|---------|-------|---------|
| `com.vaadin.flow.component.richtexteditor` | **RteExtensionBase** | Bridge class in RTE 2's package. Lifts package-private methods to `protected`. |
| `com.vaadin.componentfactory` | **EnhancedRichTextEditor** | All ERTE business logic. Extends RteExtensionBase. Users interact with this class. |

**Design rationale:** RTE 2's `RichTextEditor` has many package-private methods that are essential for ERTE (e.g., `runBeforeClientResponse()`, `sanitize()`). Java visibility rules prevent `EnhancedRichTextEditor` from accessing them if it's in a different package. The solution is a single bridge class (`RteExtensionBase`) in the foreign package that overrides and widens visibility. This keeps only one class as a "cross-package bridge" — the minimal intrusion into Vaadin's package structure.

### RteExtensionBase

**File:** `/workspace/enhanced-rich-text-editor-v25/src/main/java/com/vaadin/flow/component/richtexteditor/RteExtensionBase.java` (297 lines)

**Responsibilities:**
1. **Visibility lifting** — `@Override protected void runBeforeClientResponse(...)` widens package-private to protected for subclasses
2. **ERTE sanitizer** — `erteSanitize(String html)` replaces RTE 2's package-private `sanitize()` method for server-side sanitization
3. **Value path override** — `setPresentationValue(String)` intercepts server→client HTML sync to use ERTE sanitizer

**Sanitizer strategy (dual-layer):**

- **Server-side (`erteSanitize`)**: Uses jsoup Safelist to whitelist safe HTML, then post-filters class attributes to only allow ERTE classes (`ql-readonly`, `ql-tab`, etc.) and standard Quill classes (`ql-align-*`, `ql-indent-*`). Also restricts `style` attributes to safe CSS properties and validates `data:` URLs.

- **Client-side (`__updateHtmlValue` in JS)**: Preserves ERTE classes when RTE 2 re-renders to avoid stripping during the model→presentation cycle.

**Patterns:** Regex patterns used for post-filtering are defined as static final fields (lines 90–104):
- `CLASS_ATTR_PATTERN` — matches `class="..."` attributes
- `STYLE_ATTR_PATTERN` — matches `style="..."` attributes
- `CSS_COMMENT_PATTERN` — removes CSS comments from style values
- `CSS_FUNCTION_PATTERN` — detects CSS function calls for validation
- `DATA_SRC_PATTERN` — matches `src="data:..."` URLs for MIME type validation

### EnhancedRichTextEditor

**File:** `/workspace/enhanced-rich-text-editor-v25/src/main/java/com/vaadin/componentfactory/EnhancedRichTextEditor.java` (1324 lines)

**Responsibilities:**
- Public API for all ERTE features
- Event listener registration
- Toolbar management (slots, buttons, custom components, keyboard shortcuts)
- Placeholder configuration and dialog
- Tabstop and ruler management
- Readonly sections, whitespace indicators, NBSP, soft-break
- Value format handling (HTML primary, Delta via `asDelta()`)

**Major nested types:**
- **`ToolbarButton` enum** (lines 222–258) — 25 standard toolbar buttons (RTE 2) + 5 ERTE-specific = 30 total
- **`ToolbarSlot` enum** — 27 slots (see `toolbar/ToolbarSlot.java`)
- **Event classes** (lines 485+) — `PlaceholderBeforeInsertEvent`, `PlaceholderInsertedEvent`, etc.

**Lumo Theme Integration:** The JS class overrides `static get lumoInjector()` (line 428) to reuse the parent's tag name, so Vaadin's LumoInjector applies the parent's Lumo theme CSS to ERTE. This ensures toolbar icons, colors, and spacing are consistent with RTE 2.

## Toolbar Slot System

### 27 Slots Across 12 Groups

The `ToolbarSlot` enum defines 27 slots that correspond to Vaadin's RTE 2 button grouping:

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

Custom components are added via `addToolbarComponents(ToolbarSlot, Component...)` and are given the part name `toolbar-custom-component`, allowing CSS styling via `::slotted([part~='toolbar-custom-component'])`.

## Value Format

ERTE V25 adopts RTE 2's **HTML-primary format**:

- **Java getter/setter** — returns/accepts HTML strings
- **`setValue(String html)`** — sets editor content via HTML
- **`getValue()`** — returns current HTML
- **`asDelta()`** — optional wrapper that provides Delta access via `Quill.getContents()` on the client

This is a clean break from ERTE V24 / Quill 1, which was Delta-primary. The HTML format is simpler for most use cases and aligns with RTE 2's design.

## Sanitizer Architecture

ERTE implements a **dual-layer sanitizer** to defend against XSS while preserving ERTE features:

### Server-Side (`RteExtensionBase.erteSanitize()`)

Called when HTML is set via `setPresentationValue()` (HTML from Java → client):

1. **jsoup Safelist** — Starts with `Safelist.basic()` and extends with:
   - Additional tags: `img`, `h1`, `h2`, `h3`, `s`
   - `style` and `class` on **all** elements (`:all`)
   - `span[contenteditable, aria-readonly, data-placeholder]`
   - `img[align, alt, height, src, title, width]` with `data:`, `http:`, `https:` protocols

2. **Post-filter: Class whitelist** — Only allows:
   - Standard Quill classes: `ql-align-*`, `ql-indent-*`
   - ERTE classes: `ql-readonly`, `ql-tab`, `ql-soft-break`, `ql-placeholder`, `ql-nbsp`
   - Strips all other class values

3. **Post-filter: Style whitelist** — Restricts `style` attributes to safe CSS properties:
   - Allows: color, font-*, text-*, padding/margin, border, display, etc.
   - Rejects: `@import`, `javascript:`, unknown functions (only `rgb()`, `rgba()`, `hsl()`, `hsla()`, `calc()` allowed)

4. **Post-filter: Data URL validation** — Only allows safe MIME types:
   - `image/png`, `image/jpeg`, `image/jpg`, `image/gif`, `image/webp`, `image/bmp`, `image/x-icon`
   - Rejects `image/svg+xml` (can contain scripts)

5. **Post-filter: contenteditable cleanup** — Only allows `contenteditable="false"` (not `"true"` or empty)

### Client-Side (`__updateHtmlValue()` in JS)

RTE 2's internal `__updateHtmlValue()` method strips unknown `ql-*` classes when generating HTML from the editor. ERTE **completely replaces** this method (not a wrapper — does not call `super`) to preserve ERTE-specific classes that the server-side sanitizer already allowed. See the [Client-Side Value Preservation](#client-side-value-preservation) section above for the actual implementation.

This **two-stage approach** is necessary because:
- RTE 2's `sanitize()` is `static` and `package-private` in Java → cannot be overridden
- The client-side `__updateHtmlValue()` is a complete override (not a `super` wrapper), bypassing RTE 2's class-stripping entirely
- Server sanitization protects against malicious HTML; client-side preservation ensures ERTE classes survive the model→presentation cycle

## Custom Properties (CSS)

ERTE defines 20 custom CSS properties that users can override at the host element level:

### Readonly Sections (6)
- `--vaadin-erte-readonly-color`
- `--vaadin-erte-readonly-background`
- `--vaadin-erte-readonly-border-color`
- `--vaadin-erte-readonly-border-width`
- `--vaadin-erte-readonly-border-radius`
- `--vaadin-erte-readonly-padding`

### Placeholders (6)
- `--vaadin-erte-placeholder-color`
- `--vaadin-erte-placeholder-background`
- `--vaadin-erte-placeholder-border-color`
- `--vaadin-erte-placeholder-border-width`
- `--vaadin-erte-placeholder-border-radius`
- `--vaadin-erte-placeholder-padding`

### Whitespace Indicators (3)
- `--vaadin-erte-whitespace-indicator-color`
- `--vaadin-erte-whitespace-paragraph-indicator-color`
- `--vaadin-erte-whitespace-indicator-spacing`

### Ruler (5)
- `--vaadin-erte-ruler-height`
- `--vaadin-erte-ruler-border-color`
- `--vaadin-erte-ruler-background`
- `--vaadin-erte-ruler-marker-size`
- `--vaadin-erte-ruler-marker-color`

**Toolbar buttons** inherit from RTE 2's existing properties (`--vaadin-rich-text-editor-toolbar-button-*`) so that user theme overrides apply uniformly to both standard and custom buttons.

## Lumo Theme Injection

RTE 2 uses Vaadin's `LumoInjector` mechanism to inject theme-specific CSS for toolbar icons. By default, components get icon styles based on their custom element tag name:

```javascript
static get lumoInjector() {
  return { is: 'vaadin-rich-text-editor' };
}
```

ERTE overrides this to **reuse the parent's tag name** (`vaadin-rich-text-editor`) so the same Lumo injection applies. Without this, ERTE would look for theme CSS under `vcf-enhanced-rich-text-editor` (which doesn't exist in Lumo), leaving it with base SVG icons instead of Lumo's text-based/font icons. This single line ensures toolbar visual consistency.

## Files Reference

Key source files by responsibility:

| File | Lines | Purpose |
|------|-------|---------|
| `vcf-enhanced-rich-text-editor.js` | 2635 | Web component, blots, toolbar injection, i18n |
| `RteExtensionBase.java` | 297 | Bridge class, sanitizer, visibility lifting |
| `EnhancedRichTextEditor.java` | 1324 | Public API, events, configuration |
| `ToolbarSlot.java` | 117 | Enum of 27 toolbar slots |
| `ToolbarButton.java` | Nested | Enum of 30 toolbar buttons (25 standard + 5 ERTE) |
| `ToolbarSwitch.java` | 119 | Toggle button helper (extends Button) |
| `ToolbarPopover.java` | 132 | Dropdown panel helper (extends Popover) |
| `ToolbarDialog.java` | 230 | Modal dialog helper (extends Dialog) |
| `ToolbarSelectPopup.java` | 59 | Context menu helper (extends ContextMenu) |
| `Placeholder.java` | 158 | Placeholder data class |
| `TabStop.java` | 77 | TabStop data class |
| `vcf-enhanced-rich-text-editor-styles.css` | 338 | CSS custom properties, blot styles, slots |

---

**See also:**
- [`EXTENDING.md`](./EXTENDING.md) — Practical patterns for extending ERTE (custom blots, keyboard shortcuts, toolbar components)
- [`API_REFERENCE.md`](./API_REFERENCE.md) — Complete public API documentation
- [`USER_GUIDE.md`](./USER_GUIDE.md) — User-facing features and configuration
