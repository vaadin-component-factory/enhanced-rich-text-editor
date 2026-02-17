# ERTE v25 Migration - Implementation Notes

Concrete technical findings from source code analysis of RTE 2 (Vaadin 25.0.4/25.0.5), Quill v1 vs v2 API differences, and ERTE 1 feature comparison. These notes are intended to guide the actual implementation.

**Companion documents:**
- `agents_analysis.md` -- Original agent review + updated answers
- `feature_comparison.md` -- 20-feature ERTE 1 vs RTE 2 comparison matrix
- `quill_v1_to_v2_api_diff.md` -- 22-area Quill v1 to v2 API migration guide

---

## 1. RTE 2 Web Component Architecture (v25.0.4)

### Class Hierarchy
```javascript
class RichTextEditor extends RichTextEditorMixin(
  ElementMixin(ThemableMixin(PolylitMixin(LumoInjectionMixin(LitElement))))
)
```
- **Lit-based**, NOT Polymer. Uses `PolylitMixin` (compatibility shim for Polymer-style observers).
- Rendering via Lit `html` tagged templates, NOT Polymer's `static get template()`.
- **Impact**: Lit re-renders the template on property changes. Injected DOM nodes in the toolbar WILL be destroyed on re-render.

### Shadow DOM
- **Open** shadow root (standard LitElement default).
- Accessible via `element.shadowRoot.querySelector(...)`.
- Structure:
```
<vaadin-rich-text-editor>
  #shadow-root (open)
    <div class="vaadin-rich-text-editor-container">
      <div part="toolbar" role="toolbar">
        <span part="toolbar-group toolbar-group-history">...</span>
        <span part="toolbar-group toolbar-group-emphasis">...</span>
        <span part="toolbar-group toolbar-group-style">...</span>       ← NEW (color/bg)
        <span part="toolbar-group toolbar-group-heading">...</span>
        <span part="toolbar-group toolbar-group-glyph-transformation">...</span>
        <span part="toolbar-group toolbar-group-list">...</span>
        <span part="toolbar-group toolbar-group-indent">...</span>
        <span part="toolbar-group toolbar-group-alignment">...</span>
        <span part="toolbar-group toolbar-group-rich-text">...</span>
        <span part="toolbar-group toolbar-group-block">...</span>
        <span part="toolbar-group toolbar-group-format">...</span>
        <input id="fileInput" type="file" ...>
      </div>
      <div part="content">                                               ← Quill mounts here
      </div>
      <div class="announcer" aria-live="polite"></div>
    </div>
    <slot name="tooltip"></slot>
    <slot name="link-dialog"></slot>
    <slot name="color-popup"></slot>
    <slot name="background-popup"></slot>
```

### Toolbar: NO Slots
- The toolbar has **zero `<slot>` elements** for custom content injection.
- All buttons are hardcoded in the Lit `render()` method.
- Buttons use `::part()` CSS selectors (e.g., `::part(toolbar-button-bold)`).
- Groups use `::part()` (e.g., `::part(toolbar-group-emphasis)`).
- Only slots are for overlays: `tooltip`, `link-dialog`, `color-popup`, `background-popup`.

### Quill Instance Access
- Exposed as `element._editor` (underscore-prefixed, undocumented internal API).
- Set in `ready()`: `this._editor = new Quill(editor, { modules: { toolbar: this._toolbarConfig } });`
- Accessible from outside since it's NOT a `#private` field.

### No Pre-Instantiation Hook
- NO `extendOptions` callback pattern.
- NO lifecycle hook before `new Quill(...)`.
- Quill options are hardcoded: only `toolbar` module configured.

### Global Blot Registration Works
RTE 2 already demonstrates this pattern:
```javascript
const QuillCodeBlockContainer = Quill.import('formats/code-block-container');
class CodeBlockContainer extends QuillCodeBlockContainer { ... }
Quill.register('formats/code-block-container', CodeBlockContainer, true);
```
This happens at **module evaluation time** (top-level). Any JS module loaded before RTE 2's module can register blots globally.

---

## 2. RTE 2 Java/Flow Component (v25.0.5)

### Class Hierarchy
```java
@Tag("vaadin-rich-text-editor")
@NpmPackage(value = "@vaadin/rich-text-editor", version = "25.0.4")
@JsModule("@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js")
public class RichTextEditor
    extends AbstractSinglePropertyField<RichTextEditor, String>
    implements CompositionNotifier, InputNotifier, KeyNotifier,
               HasSize, HasStyle, HasValueChangeMode,
               HasThemeVariant<RichTextEditorVariant>
```

### Key Differences from ERTE 1
| Aspect | ERTE 1 | RTE 2 |
|--------|--------|-------|
| Tag | `vcf-enhanced-rich-text-editor` | `vaadin-rich-text-editor` |
| Hierarchy | 2-level (via Generated) | Flat (single class) |
| Primary value | Delta JSON (`"value"`) | HTML (`"htmlValue"`) |
| `Focusable` | Yes | No |
| Theme | `HasTheme` | `HasThemeVariant<RichTextEditorVariant>` |
| JSON library | `elemental.json` | `tools.jackson` |
| i18n delivery | Per-key `$0.set('i18n.key', $1)` | `Object.assign({}, this.i18n, $0)` |
| Sanitization | Custom Jsoup safelist | Package-private `sanitize()` |
| `runBeforeClientResponse()` | Accessible (own class) | Package-private |

### Value Format: HTML Primary (Breaking Change)
```java
// RTE 2 constructor
super("htmlValue", "", String.class,
    RichTextEditor::presentationToModel,
    RichTextEditor::modelToPresentation);
```
- `setValue("html")` / `getValue()` work with HTML.
- **Throws `IllegalArgumentException`** if value starts with `[` or `{` (Delta guard).
- `asDelta()` returns `HasValue<..., String>` wrapper for Delta format.
- `asHtml()` returns `HasValue<..., String>` wrapper (bypasses Delta guard).

### Package-Private Methods
These are NOT accessible from a subclass in a different package:
- `sanitize(String html)` -- static, package-private
- `runBeforeClientResponse(SerializableConsumer<UI>)` -- inherited behavior, but registration is package-private

**Decision**: ERTE 2 will use package `com.vaadin.flow.component.richtexteditor` -- gives direct access to both methods.

### Extension Approach: Direct Inheritance (Recommended)
```java
@Tag("vaadin-rich-text-editor")         // Same tag = reuse web component
@JsModule("./erte-enhancement.js")      // Additional JS for ERTE features
public class EnhancedRichTextEditor extends RichTextEditor {
    // Override/add methods, add ERTE-specific properties
}
```
- **Pro**: Inherits all RTE 2 features, auto-inherits fixes.
- **Con**: Cannot change constructor property binding (HTML-primary), package-private methods inaccessible.
- Use `asDelta()` for backward-compatible Delta access.
- `@Tag` on subclass DOES override the superclass annotation.

---

## 3. Quill v1 to v2: HIGH Impact Changes

### 3.1 Keyboard Bindings (HIGHEST RISK)

**ERTE uses numeric keyCodes extensively:**
```javascript
// Current (Quill 1)
const TAB_KEY = 9;
keyboard.bindings[TAB_KEY] = [customBinding, ...originalBindings];
keyboard.bindings[13] = [softBreakBinding, hardBreakBinding, ...enterBindings];
keyboard.addBinding({ key: 121, altKey: true, handler: focusToolbar }); // F10
```

**Quill 2 stores bindings under `evt.key` strings AND does dual lookup:**
```javascript
// Quill 2 internal
const bindings = (this.bindings[evt.key] || []).concat(this.bindings[evt.which] || []);
```

**The trap**: ERTE reads `keyboard.bindings[9]` expecting existing Quill bindings. But Quill 2 stores its defaults under `"Tab"`. So ERTE gets `undefined`, replaces nothing, and Quill's defaults AND ERTE's customs BOTH fire.

**Required migration:**
```javascript
// Key name mapping
9  → 'Tab'       13 → 'Enter'      8  → 'Backspace'
46 → 'Delete'    38 → 'ArrowUp'    40 → 'ArrowDown'
90 → 'z'         86 → 'v'          121 → 'F10'
80 → 'p'         32 → ' '
```

**30+ lines** in `vcf-enhanced-rich-text-editor.js` need updating.
**Tables extension** already uses string key names (lower risk).

### 3.2 History Stack Structure

**Quill 1 stack items:**
```javascript
{ undo: Delta, redo: Delta }
// Accessed as: entry.undo.ops, entry.redo.ops
```

**Quill 2 stack items:**
```javascript
{ delta: Delta, range: Range | null }
// delta is the INVERSE of the change (for undo)
```

**ERTE placeholder code** accesses `entry.undo.ops` -- must change to `entry.delta.ops`.
**Tables extension** pushes custom `{ type: 'tableHistory', id }` entries -- incompatible with v2's expected format. **Entire TableHistory system needs redesign.**

### 3.3 Embed Blot DOM Structure

**Quill 1**: Embed DOM is the raw node from `create()`.
**Quill 2**: Embed wraps content in `contentNode` span + adds `\uFEFF` guard characters:
```
[guard-left: \uFEFF] [contentNode: <span contenteditable="false">...</span>] [guard-right: \uFEFF]
```

**Impact on ERTE:**
- TabBlot `node.innerText = '\u200B'` -- content moves into `contentNode`
- SoftBreakBlot `node.innerHTML = '<br>'` -- same
- Tab width calculation via `getBoundingClientRect()` may include guard nodes
- CSS selectors targeting `.ql-tab` directly may need updating
- Click handlers may interact differently with guard nodes

### 3.4 Blot Constructor Signature

**Quill 1**: `constructor(domNode, value)`
**Quill 2**: `constructor(scroll, domNode, value)` -- `scroll` (ScrollBlot) is new first parameter.

**Only PlaceholderBlot** has a custom constructor in ERTE. ReadOnlyBlot, TabBlot, SoftBreakBlot inherit defaults.

---

## 4. Quill v1 to v2: MODERATE Impact Changes

### 4.1 `__quill` Property Removed
```javascript
// Quill 1
const quill = containerEl.__quill;

// Quill 2: use Quill.find()
const quill = Quill.find(containerEl);
```
Affects: `vcf-enhanced-rich-text-editor-blots.js` lines 55-58.

### 4.2 `__blot` Property Removed
```javascript
// Quill 1
const blot = domNode.__blot.blot;

// Quill 2: use Quill.find() or registry
const blot = Quill.find(domNode);
```
Affects: `erte-table/connector.js` line 101.

### 4.3 Delta Constructor Access
```javascript
// Quill 1 (works but unsupported)
const Delta = Quill.imports.delta;

// Quill 2 (official)
const Delta = Quill.import('delta');
```
4 locations in `vcf-enhanced-rich-text-editor.js`.

### 4.4 List Markup Change
**Quill 1**: `<ul>` for bullet, `<ol>` for ordered.
**Quill 2**: ALL lists use `<ol>` with `data-list` attribute.
Impact: CSS and clipboard matchers targeting `<ul>`.

---

## 5. Quill v1 to v2: LOW/NO Impact (Safe)

These APIs are **drop-in compatible**:
- `Quill.sources` constants (USER, API, SILENT)
- `Quill.find(domNode)` static method
- Blot traversal (`.children.head`, `.next`, `.prev`, `.parent`, `.statics`)
- `Quill.register()` for blot/module registration
- `clipboard.addMatcher()` API (same signature)
- `Inline.order` array (still exists)
- Toolbar module configuration and handlers

---

## 6. Decided Extension Strategy: JS Class Extension

Architecture decided: Extend the RTE 2 JS class directly (not prototype patching).

### JS Side
```javascript
// vcf-enhanced-rich-text-editor.js
import '@vaadin/rich-text-editor';  // loads RTE 2 + Quill 2.0.3

const Quill = window.Quill;
const RichTextEditor = customElements.get('vaadin-rich-text-editor');

// Register custom blots GLOBALLY (before any editor is created)
Quill.register('formats/tab', TabBlot);
Quill.register('formats/soft-break', SoftBreakBlot);
Quill.register('formats/readonly', ReadOnlyBlot);
Quill.register('formats/placeholder', PlaceholderBlot);
Quill.register('formats/nbsp', NbspBlot);

class EnhancedRichTextEditor extends RichTextEditor {

  render() {
    // Override template: standard RTE 2 buttons + ERTE buttons + slots
    return html`
      <div class="vaadin-rich-text-editor-container">
        <div part="toolbar" role="toolbar">
          <!-- Standard toolbar groups (keep same part names for theme compat) -->
          <span part="toolbar-group toolbar-group-history">...</span>
          <!-- ... standard groups ... -->
          <!-- ERTE-specific groups -->
          <span part="toolbar-group toolbar-group-erte">
            <button class="ql-whitespace" part="toolbar-button toolbar-button-whitespace">...</button>
            <slot name="toolbar-extra"></slot>
          </span>
        </div>
        <div part="content"></div>
      </div>
    `;
  }

  ready() {
    // BEFORE super.ready(): prepare Quill options if needed
    super.ready();  // → creates Quill instance

    // AFTER Quill init: keyboard bindings, tab engine, rulers, etc.
    this._editor.keyboard.addBinding({ key: 'Tab', handler: ... });
  }
}

customElements.define('vcf-enhanced-rich-text-editor', EnhancedRichTextEditor);
```

### Java Side
```java
package com.vaadin.flow.component.richtexteditor;  // same package for access

@Tag("vcf-enhanced-rich-text-editor")
@JsModule("./vcf-enhanced-rich-text-editor.js")
public class EnhancedRichTextEditor extends RichTextEditor {
    // Inherits: setValue/getValue (HTML), asDelta(), setI18n(), sanitize(), etc.
    // Adds: setTabStops(), setPlaceholders(), setShowWhitespace(), etc.
}
```

### Why This Approach
- `render()` override → toolbar slots are Lit-native, no MutationObserver hacks
- `ready()` override → clean Quill pre/post-init hooks without prototype patching
- ES module import chain → guaranteed load order
- Same package → access to package-private methods
- Inherits all RTE 2 logic (properties, observers, value handling, i18n mechanics)
- Only the template is custom → "thin fork", not full fork

### Theming Consideration
- Must keep same `part` attribute names in overridden `render()` for `::part()` CSS compatibility
- Spike must verify if `ThemableMixin` inherits parent styles for the new tag (item #11)

### Tab/Ruler System
- Ruler `<div>` elements rendered directly in the `render()` template (above/below toolbar)
- Tab width calculation engine runs inside the shadow DOM context
- Uses `this._editor` (Quill instance) for all Quill API calls

---

## 7. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Lit re-render destroys toolbar injections | ~~HIGH~~ RESOLVED | `render()` override in JS subclass -- toolbar is part of ERTE's own template |
| `_editor` renamed/removed in future Vaadin | MEDIUM | Defensive access with fallback: `this._editor \|\| this.shadowRoot.querySelector('.ql-editor').__quill` |
| Keyboard binding double-firing | HIGH | Must fully migrate to string key names before any testing |
| Embed guard nodes break tab width calc | HIGH | Measure `contentNode` instead of outer node; test thoroughly |
| Package-private `sanitize()` | ~~LOW~~ RESOLVED | Same package `com.vaadin.flow.component.richtexteditor` |
| HTML-primary value format | ~~MEDIUM~~ RESOLVED | Decided: HTML-primary + `asDelta()`. Clean break API. |
| Vaadin minor release breaks shadow DOM structure | MEDIUM | Pin to specific Vaadin 25.0.x; test each minor upgrade |

---

## 8. Table Extension: Fork Analysis & Migration Strategy

### Fork Origin
`dost/quilljs-table` → `acatec/quill-table-tbf` → `dclement8/quill1-table` → **ERTE Tables**

Integrated into ERTE repo on Feb 4, 2025 (commit `b2b9606` - "Tables final 24 (#37)").

### Code Breakdown

| Category | LOC | Origin | Quill 2 Impact |
|----------|-----|--------|----------------|
| **Core Blots** (TableBlot, TableRowBlot, TableCellBlot, ContainBlot) | ~1,800 JS | Original fork | **100% rewrite** -- constructor signature, Parchment v3 |
| **TableHistory** | ~200 JS | ERTE addition | **100% rewrite** -- Quill 2 history stack incompatible |
| **Keyboard Bindings** (index.js) | ~100 JS | Mix | **50% rewrite** -- string keys, handler signatures |
| **TableSelection, TableToolbar, TableTrick** | ~500 JS | ERTE addition | **30% changes** -- Quill API access patterns |
| **Connector** (Java-JS bridge) | ~100 JS | ERTE addition | Minor -- `__blot` → `Quill.find()` |
| **Clipboard Matchers** | ~60 JS | Mix | Minor -- `addMatcher()` API unchanged |
| **Java Server-Side** (Templates, Events, i18n) | ~3,500 Java | 100% ERTE | **Unchanged** -- format-agnostic |

### Delta Format (Preserved)
Pipe-separated `td` attribute: `TABLE_ID|ROW_ID|CELL_ID|MERGE_ID|COLSPAN|ROWSPAN|TEMPLATE_ID`

Delta format is **independent of Quill version** -- only the blot registration changes, not the data encoding. Existing table deltas remain compatible.

### Decision: Rewrite Table Blots for Quill 2

Rationale:
- `dclement8/quill1-table` has **no Quill 2 port** and no active development
- No existing Quill 2 table addon uses the same delta format
- Adopting a different addon would break delta compatibility
- The core blots (~1,800 LOC) must be rewritten anyway for Parchment v3
- Java code (~3,500 LOC) stays untouched
- ERTE-specific JS additions (~800 LOC) need moderate changes

### Specific Quill 2 Changes for Tables

1. **Blot constructors**: `static create(value)` + `constructor(node)` → `constructor(scroll, node, value)`
2. **`__blot` property**: `domNode.__blot.blot` → `Quill.find(domNode)` (connector.js:101)
3. **Container.order**: May not exist in Parchment v3 -- needs investigation
4. **History stack**: `{undo: Delta, redo: Delta}` → `{delta: Delta, range: Range}` -- TableHistory custom entries (`{type: 'tableHistory', id}`) incompatible
5. **`history.ignoreChange`**: Still exists but semantics may differ
6. **Keyboard bindings** (index.js): Already uses string key names (lower risk than core ERTE)
7. **Clipboard matchers**: `addMatcher()` API unchanged -- minor adaptations only

---

## 9. Confirmed: RTE 2 Uses Quill 2.0.3

Verified from the actual vendored file at `packages/rich-text-editor/vendor/vaadin-quill.js` (tag v25.0.4):
- **Quill 2.0.3** -- header comment: `Quill Editor v2.0.3`, static: `VERSION = "2.0.3"`
- Quill is vendored (not an npm dependency), so it doesn't appear in `package.json`

**Impact**: ALL Quill v1→v2 migration notes in `quill_v1_to_v2_api_diff.md` apply to the immediate migration. The 4 HIGH-impact changes (keyboard bindings, history stack, embed DOM structure, blot constructors) must be addressed as part of the ERTE 2 work.

*(An earlier agent incorrectly claimed RTE 2 still used Quill 1 based on a web search finding an open GitHub issue. The actual vendored file confirms Quill 2.0.3.)*

---

## 9. Spike Checklist (2-3 Days)

Before full implementation, validate these in a minimal Vaadin 25 project:

1. [x] **Verify Quill version** in RTE 2: **Confirmed Quill 2.0.3** (vendored at `vendor/vaadin-quill.js`)
2. [ ] **Register a custom Embed blot** (e.g., TabBlot) via `Quill.register()` before element creation
3. [ ] **Insert content with custom blot** via `element._editor.setContents(delta)`
4. [ ] **Inject a button** into the toolbar shadow DOM
5. [ ] **Verify button survives** property change -- trigger i18n update, readonly toggle, disabled toggle, AND setValue from Java. Each causes Lit re-render.
6. [ ] **Access keyboard module**: `element._editor.getModule('keyboard')`
7. [ ] **Add a keyboard binding**: `element._editor.keyboard.addBinding({ key: 'Tab', handler: ... })`
8. [ ] **Extend RichTextEditor** in Java: `class EnhancedRichTextEditor extends RichTextEditor`
9. [ ] **Override @Tag** on the subclass: verify it uses the same web component
10. [ ] **Load custom @JsModule**: verify import order -- add `console.log` at top of each module, check browser console for correct sequence
11. [ ] **ThemableMixin inheritance**: Does extending the JS class inherit Lumo/Material styles registered for `vaadin-rich-text-editor`? Or must styles be re-registered for the new tag via `registerStyles()`?
12. [ ] **`::part()` selectors**: Verify that keeping the same `part` attribute names in the overridden `render()` template preserves external theme styling

### Critical (spike fails if these fail)

13. [ ] **`render()` override + parent observer compatibility**: Override `render()` in JS subclass. Set `readonly=true` from Java → toolbar must disable. Set `value` from Java → content must render. Do parent observers (`_onReadonlyChanged`, `_onDisabledChanged`, `_onValueChanged`) still work when child overrides `render()`?
14. [ ] **Lifecycle order**: Add `console.log` timestamps in subclass `connectedCallback()`, `firstUpdated()`, `ready()` (before/after `super.ready()`). Log when `this._editor` first becomes non-null. Document exact order.
20. [ ] **Embed guard node impact on tab width**: Register TabBlot with Quill 2 Embed. Insert 5 tabs with tabstops. Measure `tabNode.getBoundingClientRect().width` -- does it include guard nodes? Is `contentNode` the correct measurement target?

### High (architecture-relevant)

15. [ ] **`executeJs("$0.method")` targets correct element**: From Java, call `getElement().executeJs("return $0.tagName")` → must return `VCF-ENHANCED-RICH-TEXT-EDITOR`. Then access `$0._editor.getLength()`.
16. [ ] **`runBeforeClientResponse()` from subclass**: Call from Java subclass, verify JS executes on correct element at correct time.
17. [ ] **`@NpmPackage` inheritance**: Build with only subclass `@JsModule` (no `@NpmPackage` on subclass). Does `@vaadin/rich-text-editor` resolve from parent annotation?
19. [ ] **HTML sanitizer vs Quill 2 output**: Create content with bullet list, ordered list, code block, custom embed, color. Call `getValue()`, inspect HTML. Check which tags/attributes the sanitizer strips or passes incorrectly.
21. [ ] **Clipboard round-trip**: Insert content with custom blot (tab). Select all, copy, paste. Verify pasted content has correct blot (not raw HTML).
22. [ ] **`text-change` delta correctness**: Listen to `text-change`. Insert/delete content with custom embed. Verify delta length for embed is 1 (not 3 from guard nodes).
23. [ ] **Production build**: Run `mvn clean package -Pproduction`. Verify production bundle contains both custom elements. Open production app, verify ERTE works.

### Deferred (first sprint, not spike)

18. [ ] **Global blot registry override risk**: Security note -- third-party scripts could re-register blot names with `Quill.register('formats/tab', MaliciousBlot, true)`.

### Priority grouping

**Must-pass for viability** (spike fails): 2, 3, 4, 8, 9, 13, 20
**Must-pass for architecture** (workaround possible): 5, 6, 7, 10, 14, 15, 22
**Informs implementation** (can defer): 11, 12, 16, 17, 18, 19, 21, 23
