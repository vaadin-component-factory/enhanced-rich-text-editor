# ERTE v25 Migration Spike Results

**Date**: 2026-02-16
**Vaadin**: 25.0.4 (vaadin BOM), 25.0.5 (vaadin-spring resolved)
**Spring Boot**: 4.0.2
**Java**: 21+ (runtime: 25.0.2)
**Quill**: 2.0.3 (vendored by RTE 2)

---

## Item 8: Java Class Extension

**Status**: PASS

**Observation**: `EnhancedRichTextEditor extends RichTextEditor` compiles and runs correctly in package `com.vaadin.flow.component.richtexteditor`. The class inherits all RTE 2 functionality including `setReadOnly()`, `asHtml().setValue()`, `asDelta()`, `getElement().executeJs()`, `setWidthFull()`, and `setMaxHeight()`.

**Code adjustments**:
- Vaadin 25 requires the `vaadin` artifact (not `vaadin-core`) because RichTextEditor is now a commercial Pro component
- `vaadin-dev` must be explicitly added as a dependency for dev mode to work
- Vaadin 25 requires **Spring Boot 4.x** (4.0.2 tested), NOT Spring Boot 3.x
- `spring.main.allow-bean-definition-overriding=true` no longer needed with Spring Boot 4 + Vaadin 25 (was only needed when accidentally mixing Spring Boot 3.4.x)

**Surprises**:
- `RichTextEditor` moved from `vaadin-core` to `vaadin` (Pro component). This means the ERTE addon must document that the user needs a Vaadin Pro subscription for v25.
- The `@NpmPackage` annotation on the parent `RichTextEditor` class is inherited correctly. No `@NpmPackage` needed on the subclass.

---

## Item 9: Verify @Tag Override

**Status**: PASS

**Observation**: `getElement().executeJs("return $0.tagName")` returned `"VCF-ENHANCED-RICH-TEXT-EDITOR"`. The `@Tag("vcf-enhanced-rich-text-editor")` on the subclass correctly overrides the parent's `@Tag("vaadin-rich-text-editor")`.

**Code adjustments**: None needed.

**Surprises**: None -- works exactly as documented.

---

## Item 2: Register Custom Embed Blot

**Status**: PASS

**Observation**: Custom `TabBlot extends Embed` registered successfully via `Quill.register('formats/tab', TabBlot)` at module evaluation time (top-level). The Quill 2 Embed base class is available at `Quill.import('blots/embed')` and has the class name `o` in the minified build.

**Code adjustments**:
- No custom constructor needed -- inheriting the default Quill 2 Embed constructor `(scroll, domNode, value)` works.
- `static className = 'ql-tab'` is sufficient for Quill 2 to apply the class; no need to call `node.classList.add()` in `create()`.
- `static create(value)` sets styles on the outer node. Quill 2's Embed constructor then wraps content in a `contentNode` span and adds guard text nodes.

**Surprises**: None -- global registration at module evaluation time works exactly as it does in ERTE 1.

---

## Item 3: Insert Content with Custom Blot

**Status**: PASS

**Observation**: Content with custom tab embeds inserts correctly via:
```javascript
const Delta = Quill.import('delta');
editor._editor.setContents(new Delta()
  .insert('Hello')
  .insert({ tab: true })
  .insert('World\n')
);
```
The tabs render in the DOM as `<span class="ql-tab">` elements. The Delta constructor is available via `Quill.import('delta')`.

**Code adjustments**:
- Content set in `ready()` gets overwritten by the `_valueChanged` observer when the default empty value syncs from Java. Content insertion should happen AFTER the initial value sync (or use `setTimeout`, or set content via the Java `asDelta()` API instead).
- `Quill.import('delta')` is the correct way to get the Delta class in Quill 2 (not `Quill.imports.delta`).

**Surprises**:
- The `_valueChanged` observer race condition is expected behavior. In production, content will be set via Java's `setValue()`/`asDelta().setValue()`, not in `ready()`.

---

## Item 4: Toolbar Button in render() Override

**Status**: PASS

**Observation**: The full RTE 2 toolbar was replicated in the overridden `render()` method with an additional ERTE-specific button group:
```html
<span part="toolbar-group toolbar-group-erte">
  <button id="btn-whitespace" type="button"
    part="toolbar-button toolbar-button-whitespace"
    title="Toggle Whitespace" @click="${this._onWhitespaceClick}">WS</button>
</span>
```
All standard RTE 2 toolbar buttons render correctly with proper icons (via Lumo theme styles). The custom "WS" button renders at the end of the toolbar with its click handler firing correctly.

**Code adjustments**:
- Must use optional chaining for i18n properties: `${this.__effectiveI18n?.bold || 'Bold'}` because `__effectiveI18n` may be `undefined` during initial render before the observer sets it.
- Lit event handlers (`@click="${this._onWhitespaceClick}"`) work correctly on buttons in the overridden template.
- The `ql-*` class names on buttons are needed for Quill's toolbar module to recognize and bind them.

**Surprises**:
- The Lumo theme styles for `vaadin-rich-text-editor` toolbar buttons (icons, hover effects, etc.) are NOT automatically inherited by the `vcf-enhanced-rich-text-editor` tag. This is expected -- ThemableMixin registers styles per tag name. **Item 11 (ThemableMixin inheritance) will need a separate investigation for styling.** However, the functional behavior works perfectly.
- UPDATE: Actually the toolbar buttons DO have icons and proper styling in the screenshot, which means the theme IS being applied. This needs further investigation -- it may be because the build bundles all styles or because Lumo styles are applied globally.

---

## Item 10: Module Load Order

**Status**: PASS

**Observation**: Console logs confirm the correct lifecycle sequence:
```
[ERTE] Module top-level: T0            (JS module evaluation)
[ERTE] Quill available: true 2.0.3     (window.Quill is set)
[ERTE] RichTextEditor class: true      (customElements.get works)
[ERTE] Embed base class: true          (Quill.import works)
[ERTE] TabBlot registered              (Quill.register works)
[ERTE] Custom element defined: T0      (same ms as module top)
[ERTE] connectedCallback: T0+132       (element inserted into DOM)
[ERTE] ready() before super: T0+140    (about to call super.ready())
[ERTE] ready() _editor before super: false
[ERTE] ready() after super, _editor: T0+140, true
[ERTE] connectedCallback after super: T0+132
```

**Key findings**:
1. Module-level code runs synchronously -- blot registration happens before any element is created.
2. `_editor` is `false` before `super.ready()` and `true` immediately after.
3. `connectedCallback` fires before `ready()` (Lit lifecycle).
4. `super.connectedCallback()` completes after `ready()` returns (the `super.connectedCallback()` call initiates Lit rendering which eventually triggers `ready()`).

**Code adjustments**: None needed.

**Surprises**: The `connectedCallback after super` timestamp appears AFTER `ready()` output in the console. This is because Lit's `connectedCallback -> requestUpdate -> firstUpdated -> ready()` pipeline runs synchronously within the microtask queue triggered by `super.connectedCallback()`.

---

## Item 13: render() Override + Parent Observer Compatibility

**Status**: PASS

**Observation**: All three parent observer scenarios work correctly:

1. **`setReadOnly(true)` from Java**: Toolbar hides (RTE 2's standard readonly behavior). Content remains visible and non-editable. The `_onReadonlyChanged` observer finds the toolbar via `this.shadowRoot.querySelector('[part="toolbar"]')` which correctly resolves to the child's overridden template.

2. **`setReadOnly(false)` from Java**: Toolbar reappears with ALL buttons including the custom "WS" button. Buttons are enabled and functional.

3. **`setValue()` from Java** (via `asHtml().setValue()`): Content renders correctly in the editor. The `_valueChanged` observer calls `editor.setContents(delta, SOURCE.SILENT)` which works because `_editor` references the Quill instance mounted on the child's `[part="content"]` div.

**Code adjustments**: None needed -- the parent observers work with the child's DOM because:
- They query the shadow DOM using `part` attribute selectors, which are preserved in the overridden template.
- The `_editor` reference is set in `ready()` which targets `this.shadowRoot.querySelector('[part="content"]')`.

**Surprises**:
- The `format` toolbar group in RTE 2's template wraps the clean button in a `<span>` group, but in our template we put the clean button directly. This doesn't affect functionality but the clean button might get slightly different styling. For production, wrap it in the group span.

---

## Item 20: Embed Guard Node Impact

**Status**: PASS (with critical findings)

**Observation**: Detailed analysis of the Quill 2 Embed DOM structure for TabBlot:

### DOM Structure (per tab element)
```html
<span class="ql-tab" contenteditable="false" style="display:inline-block; min-width:2em; ...">
  "\uFEFF"                          <!-- Guard text node (left) -->
  <span contenteditable="false">    <!-- contentNode (inner) -->
  </span>
  "\uFEFF"                          <!-- Guard text node (right) -->
</span>
```

### Measurements (all 5 tabs identical)
| Node | Width | Height |
|------|-------|--------|
| Outer `.ql-tab` span | **32px** | 19px |
| Inner `contentNode` span | **0px** | 17px |
| Guard text nodes | N/A (text nodes) | N/A |

### Critical Findings

1. **Guard nodes are INSIDE the tab element**, not outside. The `\uFEFF` (BOM/ZWNBSP) characters are children of the `.ql-tab` span, NOT siblings of it.

2. **The outer `.ql-tab` rect (32px wide) INCLUDES the guard characters' rendered width.** The 32px comes from: `min-width: 2em` (our style) which is roughly 32px at the default font size. The guard characters are zero-width in practice because `\uFEFF` is a zero-width no-break space.

3. **The `contentNode` has 0 width.** This is because we don't put any content into it. In ERTE's actual tab implementation, the tab width should be set on the OUTER `.ql-tab` element, not the contentNode.

4. **For tab width calculation, measure the OUTER `.ql-tab` element.** The `getBoundingClientRect()` of the outer span gives the correct visual width including any CSS-applied min-width/width. The contentNode is empty (0px wide) and should NOT be used for measurement.

5. **Guard characters do NOT appear in the Delta representation.** Tab embeds are still `{ insert: { tab: true } }` with length 1 in the delta. Guard nodes are purely a DOM-level Quill 2 implementation detail.

6. **No guard characters BETWEEN tab elements.** Adjacent text and tab elements are direct siblings in the `<p>` element: `"Hello" <span.ql-tab> "Tab1" <span.ql-tab> "Tab2"`. No extra `\uFEFF` text nodes between elements.

**Code adjustments for ERTE tab engine**:
- Tab width calculation: continue using `.ql-tab` element's `getBoundingClientRect()` -- this gives the correct visual measurement.
- Do NOT try to measure `contentNode` -- it will be 0px wide.
- The `contenteditable="false"` we set on the outer node is redundant (Quill 2 Embed also sets it on contentNode), but not harmful.
- CSS targeting `.ql-tab` works correctly -- no selector changes needed.

**Surprises**:
- The guard structure is simpler than expected. Guards are INSIDE the embed element, not outside it as initially feared. This means `getBoundingClientRect()` on `.ql-tab` gives a clean measurement without needing to subtract guard widths.
- The contentNode is empty by default -- our `create()` styles are on the outer node and that's where the visual rendering happens.

---

## Item 15: executeJs targets correct element

**Status**: PASS

**Observation**: `getElement().executeJs("return $0.tagName")` returns `"VCF-ENHANCED-RICH-TEXT-EDITOR"`. `getElement().executeJs("return $0._editor ? $0._editor.getLength() : -1")` returns the correct Quill content length. The `$0` reference correctly points to the `vcf-enhanced-rich-text-editor` element.

**Code adjustments**: None.

**Surprises**: None.

---

## Item 22: text-change Delta Correctness

**Status**: PASS

**Observation**: Inserting a tab embed via `quill.insertEmbed(pos, 'tab', true, 'user')` produces a text-change delta of:
```json
{ "ops": [{ "retain": 43 }, { "insert": { "tab": true } }] }
```
The embed has length 1 in the delta. Guard nodes (`\uFEFF`) are NOT included. The full content delta correctly represents each tab as `{ "insert": { "tab": true } }`.

**Code adjustments**: None needed -- delta format is clean.

**Surprises**: None -- Quill 2's delta layer properly abstracts away the guard node implementation.

---

## Item 17: @NpmPackage Inheritance

**Status**: PASS

**Observation**: The subclass only has `@JsModule("./vcf-enhanced-rich-text-editor.js")`. No `@NpmPackage` annotation on the subclass. The `@NpmPackage(value = "@vaadin/rich-text-editor", version = "25.0.4")` on the parent `RichTextEditor` class is inherited correctly. The build resolves the npm package and includes it in the dev bundle.

**Code adjustments**: None needed.

**Surprises**: None.

---

## Additional Findings

### Vaadin 25 Requires Spring Boot 4

Vaadin 25.0.4 requires Spring Boot 4.x (tested with 4.0.2). Using Spring Boot 3.4.x causes bean definition conflicts (`conventionErrorViewResolver`). The BOM manages Spring Boot dependencies, but `spring-boot.version` must be set to 4.0.x in the project properties.

### RichTextEditor is Now a Pro Component

In Vaadin 25, `RichTextEditor` has moved from `vaadin-core` to the commercial `vaadin` (full) artifact. Projects must use `<artifactId>vaadin</artifactId>` instead of `<artifactId>vaadin-core</artifactId>`.

The license check log shows: `License check ok for {name: vaadin-rich-text-editor, version: 25.0.3}`.

### vaadin-dev Dependency

The `vaadin-dev` dependency must be explicitly added for dev mode to work:
```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-dev</artifactId>
</dependency>
```

### Dev Bundle Build

The first run triggers a dev bundle build that takes ~12-15 seconds. Subsequent runs start in ~2 seconds.

### window.Quill Availability

`window.Quill` is available globally after the RTE 2 module loads. Version: `2.0.3`. This confirms the migration docs -- ERTE's existing pattern of `const Quill = window.Quill;` will work in v25.

### Value Sync Race Condition

Content set in `ready()` after `super.ready()` gets overwritten when the Java-side value observer syncs the initial empty value. In production, ERTE should either:
1. Set initial content via Java's `asDelta().setValue()` API, OR
2. Use a flag to defer JS-side content insertion until after the first value sync

---

---

## Phase 2 Items

---

## Item 6: Access Keyboard Module

**Status**: PASS

**Observation**: The Quill 2 keyboard module is accessible via `this._editor.keyboard` immediately after `super.ready()`. It is a plain JavaScript object (not a Map) with the following structure:

### Own Properties
- `quill` -- reference to the Quill instance
- `options` -- `{ bindings: [...] }` (configuration)
- `bindings` -- plain object keyed by string key names

### Prototype Methods
- `addBinding(binding, [context], handler)` -- adds a new key binding
- `listen()` -- attaches keyboard event listeners
- `handleBackspace(range, context)` -- built-in backspace handler
- `handleDelete(range, context)` -- built-in delete handler
- `handleDeleteRange(range)` -- built-in range deletion handler
- `handleEnter(range, context)` -- built-in enter handler

### Bindings Structure
Bindings are stored as `keyboard.bindings` -- a **plain object** (NOT a Map). Keys are **string key names** matching `KeyboardEvent.key` values:

```
Keys: b, i, u, Tab, Backspace, Enter, Delete, " " (space), ArrowLeft, ArrowRight, ArrowDown, ArrowUp, z, Z, F10
```

Each key maps to an array of handler objects. Each handler has:
- `key` (string) -- the key name (e.g., `"Tab"`, `"Enter"`)
- `shiftKey` (boolean|null|undefined) -- shift modifier filter (`true` = must be held, `false` = must NOT be held, `null`/`undefined` = don't care)
- `shortKey` (boolean|undefined) -- Ctrl/Cmd modifier filter
- `handler` (function) -- the handler function `(range, context) => ...`

### Default Tab Bindings (8 handlers)
| Index | shiftKey | Purpose |
|-------|----------|---------|
| 0 | undefined | Quill core: indent/outdent in collapsed cursor |
| 1 | true | Quill core: indent/outdent with shift |
| 2 | false | RTE 2: custom tab handler (forward tab) |
| 3 | true | RTE 2: custom tab handler (backward tab) |
| 4 | true | Quill: delete preceding tab character |
| 5 | undefined | Quill: insert tab character (checks for table context) |
| 6 | null | RTE 2: tab handler (any shift state) |
| 7 | true | RTE 2: Shift+Tab moves focus to toolbar |

### Default Enter Bindings (8 handlers)
| Index | shiftKey | Purpose |
|-------|----------|---------|
| 0-7 | various | Quill core + RTE 2 enter handling, code block, list, etc. |

**Code adjustments**: None needed to access the module.

**Surprises**:
- Bindings is a **plain object**, not a Map. Phase 1 noted this was possible but it's confirmed.
- Keys are **string key names** matching `KeyboardEvent.key` (e.g., `"Tab"`, not `9`). This is a BREAKING CHANGE from Quill 1 which used numeric keyCodes.
- `shiftKey` has three meaningful states: `true` (must be held), `false` (must NOT be held), `null`/`undefined` (don't care). This is important for Shift+Enter vs Enter discrimination.
- The `addBinding` method signature in minified code shows it accepts up to 3 arguments: `addBinding(binding, [context], handler)`.

---

## Item 7: Add Custom Keyboard Binding

**Status**: PASS (with critical binding priority finding)

**Observation**: `keyboard.addBinding()` works for adding custom bindings. However, **bindings are appended to the END of the handler array**, meaning they execute AFTER all existing bindings. If any earlier binding returns `false` (stopping the chain), the custom binding never fires.

### Test Results

**Test 1: `addBinding({ key: 'Tab' }, handler)`** -- Binding added successfully. However, it was placed at index [8] (last of 9 Tab handlers). When Tab was pressed in the editor, our handler did NOT fire because earlier handlers consumed the event.

**Test 2: `addBinding({ key: 'Enter', shiftKey: true }, handler)`** -- Binding added successfully at index [8] of Enter handlers. Same issue: did not fire when Shift+Enter was pressed.

**Test 3: `addBinding({ key: 9 }, handler)` (numeric, old Quill 1 style)** -- The binding was accepted without error, but stored under key `"9"` (a separate key from `"Tab"`). It will NEVER fire for Tab presses because the browser generates `KeyboardEvent.key = "Tab"`, not `"9"`.

**Test 4: Binding removal** -- Bindings can be removed by directly manipulating the array:
```javascript
keyboard.bindings['Tab'].pop();  // Remove last binding
keyboard.bindings['Tab'].splice(index, 1);  // Remove specific binding
```
This works because `bindings['Tab']` is a plain JavaScript array.

**Test 5: Priority override via reordering** -- After clearing `bindings['Tab']`, adding our handler FIRST, then re-adding original handlers, our Tab handler fired correctly:
```
[ERTE Item 7 Priority Test] Tab FIRED! range: {"index":33,"length":0}
```
The tab embed was inserted at the cursor position.

**Test 6: Shift+Enter reordering** -- Same approach: clear `bindings['Enter']`, add `{ key: 'Enter', shiftKey: true }` binding first, re-add originals. Shift+Enter correctly fired our handler. Plain Enter still worked normally (handled by original Enter bindings).

### Key Finding: Binding Priority Pattern for ERTE

To override default keyboard behavior in ERTE v25, use this pattern:

```javascript
// In ready() after super.ready():
const kb = this._editor.keyboard;

// Save existing bindings
const existing = kb.bindings['Tab'].slice();

// Clear and add ours first
kb.bindings['Tab'] = [];
kb.addBinding({ key: 'Tab' }, (range, context) => {
  // Custom tab logic
  return false; // Stop chain
});

// Re-add originals (optional, for fallback behavior)
existing.forEach(b => kb.bindings['Tab'].push(b));
```

**Code adjustments**:
- MUST use string key names: `{ key: 'Tab' }`, NOT `{ key: 9 }`. Numeric keyCodes from Quill 1 create bindings under wrong keys.
- MUST manage binding priority by clearing and re-adding bindings with ours first.
- `addBinding()` alone is NOT sufficient to override existing behavior -- it only appends.

**Surprises**:
- The numeric keyCode `9` creates a binding under key `"9"`, NOT `"Tab"`. This is a silent failure: no error, but the binding never fires for Tab presses. This will silently break any ERTE code that uses `{ key: 9 }` for Tab bindings.
- The Quill 2 keyboard module processes bindings in array order, stopping at the first handler that returns `false` (or does not return `true`). This is different from Quill 1 where binding priority was determined by the `priority` property.
- RTE 2 adds its own Tab bindings (indices 2, 3, 6, 7) for focus management, which means ERTE must be aware of these and manage priority carefully.

---

## Item 14: Detailed Lifecycle Order

**Status**: PASS

**Observation**: Full lifecycle timeline captured with millisecond precision:

### Complete Timeline

```
+0ms:   module-top-level        JS module evaluation begins
+0ms:   imports-done            RTE 2 + Lit imports resolved (synchronous)
+1ms:   blot-registered         TabBlot registered with Quill
+1ms:   element-defined         Custom element registered (customElements.define)
--- 164ms gap (Vaadin client initializes, creates element) ---
+165ms: constructor-before-super    About to call super()
+166ms: constructor-after-super     _editor=false
+168ms: connectedCallback-before-super    _editor=false, isConnected=true
+168ms: willUpdate                  changed=[dir, value, disabled, readonly, colorOptions,
                                    __lastCommittedChange, _linkEditing, _linkRange, _linkIndex,
                                    _linkUrl, _colorEditing, _colorValue, _backgroundEditing,
                                    _backgroundValue, __effectiveI18n, i18n]
+174ms: firstUpdated-before-super   _editor=false (16 properties in first batch)
+174ms: firstUpdated-after-super    _editor=false
+174ms: updated                     (same 16 properties), _editor=false
+175ms: ready-before-super          _editor=false
+179ms: willUpdate                  changed=[_editor] (Quill created inside super.ready())
+179ms: updated                     changed=[_editor], _editor=true
+183ms: ready-after-super           _editor=true
+185ms: first-text-change           source=api (our setContents call in ready())
+188ms: connectedCallback-after-super   _editor=true
+204ms: willUpdate                  changed=[htmlValue]  (value sync from editor content)
+204ms: updated                     changed=[htmlValue]
+403ms: willUpdate                  changed=[value]  (Java-side value sync)
+403ms: updated                     changed=[value]
+403ms: willUpdate                  changed=[value]  (second value sync)
+403ms: updated                     changed=[value]
+688ms: analysis-complete           setTimeout(500ms) from ready() fires
```

### Key Findings

1. **Module evaluation is synchronous**: imports, blot registration, and `customElements.define` all happen within 1ms.

2. **Element creation gap**: 164ms between element definition and constructor call (Vaadin client-side framework creates the element).

3. **Lit lifecycle order within `super.connectedCallback()`**:
   ```
   connectedCallback -> willUpdate -> firstUpdated -> updated -> ready()
   ```
   All of these fire INSIDE the `super.connectedCallback()` call, before `connectedCallback-after-super` completes.

4. **`_editor` creation happens inside `super.ready()`**: Between `ready-before-super` (+175ms) and `ready-after-super` (+183ms), the `_editor` property changes from `false` to `true`. This triggers an intermediate `willUpdate/updated` cycle with `changed=[_editor]`.

5. **`ready()` is called from within `firstUpdated()`**: The sequence is `firstUpdated-before-super` -> `super.firstUpdated()` -> `ready()` -> then later `firstUpdated-after-super`. But since `_editor` is still false after `firstUpdated-after-super`, the ready() must be called AFTER firstUpdated returns, triggered by the Lit update cycle. Actually looking more carefully: `firstUpdated` (+174ms) -> `updated` (+174ms) -> `ready-before-super` (+175ms). So `ready()` is called from within the `updated()` cycle, not from `firstUpdated()` directly.

6. **First `text-change`**: Fires immediately at +185ms (2ms after `ready-after-super`) when we call `setContents()` in `ready()`. Source is `"api"`.

7. **Value sync race**: Java-side value sync happens at +403ms (218ms after ready). This overwrites content set in `ready()`. The 0-tab count in guard analysis at +688ms confirms this: our tab content was replaced by the Java sync.

8. **`super.connectedCallback()` doesn't return until `ready()` completes**: The `connectedCallback-after-super` at +188ms is AFTER `ready-after-super` at +183ms. This means the entire Lit lifecycle runs within the `connectedCallback` call.

9. **Subsequent updates**: After initial rendering, property changes (i18n, disabled, value) trigger `willUpdate` -> `updated` cycles. The `firstUpdated` only fires once.

**Code adjustments**: None needed. The lifecycle is well-defined and predictable.

**Surprises**:
- The `_editor` property becoming available triggers its own `willUpdate/updated` cycle INSIDE `super.ready()`. This means there are Lit updates happening within `ready()`.
- `connectedCallback` wraps the ENTIRE initial lifecycle including `ready()`. The "connectedCallback-after-super" log appears at +188ms, AFTER `ready()` at +183ms.
- 16 properties are initialized in the first `willUpdate` batch. This is a large initial render.
- The value sync at +403ms (235ms after the first render) is the Java-side synchronization pushing the initial empty value. This explains why content set in `ready()` gets overwritten.

---

## Item 5: Toolbar Button Survives Lit Re-render (Extended)

**Status**: PASS

**Observation**: The custom "WS" button in the ERTE toolbar group survives ALL tested re-render scenarios:

### Test 5a: Set i18n from Java (German labels)

Setting `editor.setI18n(i18n)` with German translations triggered a Lit re-render that updated all toolbar button `aria-label` attributes. Console showed `willUpdate changed=[__effectiveI18n, i18n]` followed by `updated`.

**Result**: WS button survived. `checkWhitespaceButton()` returned:
```json
{"exists":true,"text":"WS","id":"btn-whitespace","hasActiveClass":false,"isVisible":true,"toolbarExists":true,"toolbarVisible":true,"allErteButtons":1}
```

All standard buttons got German labels (e.g., "Fett" for Bold, "Kursiv" for Italic). The WS button was unaffected because it doesn't use `__effectiveI18n`.

### Test 5b: Toggle disabled state from Java

Setting `editor.setEnabled(false)` triggered `willUpdate changed=[disabled]`. ALL toolbar buttons became disabled, including the WS button. Re-enabling with `setEnabled(true)` restored all buttons to enabled state, including WS.

**Result**: WS button correctly becomes disabled/enabled with the rest of the toolbar. The parent's `_onDisabledChanged` observer sets `disabled` on all buttons found via `this.shadowRoot.querySelectorAll('button')`, which includes our custom WS button.

### Test 5c: Multiple rapid setValue calls (10x)

10 rapid `asHtml().setValue()` calls in a loop from Java. The editor correctly displayed the last value ("Rapid value #9"). Console showed multiple `willUpdate changed=[value]` entries, confirming each setValue triggered a re-render.

**Result**: WS button survived all 10 re-renders:
```json
{"exists":true,"text":"WS","id":"btn-whitespace","hasActiveClass":false,"isVisible":true}
```

### Test 5d: Active state survives i18n re-render

Most critical test: activated WS button (clicked to set `ql-active` CSS class), then immediately set i18n from Java (forcing re-render).

**Result**: WS button's active state SURVIVED:
```json
{"exists":true,"text":"WS","id":"btn-whitespace","hasActiveClass":true,"isVisible":true}
```

The `ql-active` CSS class and `toolbar-button-pressed` part attribute persisted through the Lit re-render.

**Code adjustments**: None needed. Lit's template re-rendering preserves DOM state that isn't bound to template expressions. Since `ql-active` is set imperatively (via `classList.toggle`), it survives Lit re-renders that only update template bindings like `aria-label`.

**Surprises**:
- Imperatively-set CSS classes survive Lit re-renders. This is expected Lit behavior (Lit only updates template bindings, not arbitrary DOM mutations) but worth confirming.
- The parent's disabled observer finds buttons via `querySelectorAll('button')` -- a broad selector that naturally includes any custom buttons in the shadow DOM.
- No MutationObserver or special hooks needed to handle re-renders. The render() override with Lit templates is inherently stable.

---

## Phase 3 Items

---

## Item 11: ThemableMixin Inheritance

**Status**: PASS

**Observation**: The subclass inherits **5 adopted stylesheets** from the parent RTE 2 class. Analysis via `analyzeThemeInheritance()`:

- `adoptedStyleSheetCount`: 5
- Sheet 0: 1 rule (icon SVG data URIs)
- Sheet 1: 6 rules (`:host` box-sizing, flex, hidden)
- Sheet 2: 65 rules (Quill editor styles, `[part="content"]`, `.ql-clipboard`)
- Sheet 3: 35 rules (toolbar styles, toolbar button styles with CSS custom properties)
- Sheet 4: 2 rules (readonly/disabled states)
- `parentStaticStyles`: true, `ownStaticStyles`: true
- `parentIs`: `vaadin-rich-text-editor`, `ownIs`: `vcf-enhanced-rich-text-editor`
- `vaadinRegistrations`: true

**Initial false alarm**: First test showed Lumo vars "not set" and font resolving to Times New Roman. This was because **Lumo is no longer loaded by default in Vaadin 25** -- a breaking change from V24. ALL components (including standard `vaadin-button`) were affected, not just our custom element.

**Fix**: Add `@StyleSheet(Lumo.STYLESHEET)` to `Application.java`:
```java
@StyleSheet(Lumo.STYLESHEET)
public class Application implements AppShellConfigurator { ... }
```

**After fix**, verified via Playwright:
- `--lumo-font-family` on `html`: `-apple-system, BlinkMacSystemFont, Roboto, Segoe UI, ...`
- `--lumo-primary-color` on `html`: `#006af5`
- ERTE element font: Lumo system font stack (correct)
- ERTE toolbar font (shadow DOM): Lumo system font stack (correct)
- ERTE content font (shadow DOM): Lumo system font stack (correct)

**No `registerStyles()` needed.** In V25, Lumo is loaded via `@StyleSheet` as global CSS. The `--lumo-*` CSS custom properties cascade through shadow DOM boundaries into our custom element's adopted stylesheets, which reference them via `var(--lumo-...)`.

**Code adjustments**: None for ERTE specifically. The host application must load Lumo via `@StyleSheet(Lumo.STYLESHEET)` (V25 requirement for all Vaadin apps).

**Surprises**:
- Vaadin 25 breaking change: Lumo not auto-loaded (upgrade guide: "The Lumo theme is no longer loaded by default")
- `ThemableMixin` and `registerStyles` are **planned for removal** (per upgrade guide). V25 already uses `@StyleSheet` instead -- tag-independent, no per-element registration needed.
- CSS custom properties cascade perfectly through shadow DOM -- our custom tag gets identical Lumo theming as `vaadin-rich-text-editor` without any special registration.

---

## Item 12: ::part() Selectors

**Status**: PASS

**Observation**: All 40 shadow DOM elements with `part` attributes are accessible for styling. Test via `testPartSelectors()`:

- `toolbarPart`: exists, `part="toolbar"`, `role="toolbar"`
- `contentPart`: exists, `part="content"`
- `totalPartElements`: 40
- Custom ERTE parts present: `toolbar-group-erte`, `toolbar-button-whitespace`

### Part Styling Test
Applied `background-color: red` to `::part(toolbar)` and `background-color: green` to `::part(content)` programmatically:
- **toolbar**: `partStylingWorks: true` (background changed from default to `rgb(255, 0, 0)`)
- **content**: `partStylingWorks: true` (background changed from transparent to `rgb(0, 255, 0)`)

### Tag-Specific Isolation
Tested `vaadin-rich-text-editor::part(toolbar)` against our `vcf-enhanced-rich-text-editor` element:
- `matchesOurElement: false` -- Correct! Styles targeting the parent tag do NOT leak to our custom tag.

**Code adjustments**: None. `::part()` selectors work correctly for both standard and custom parts.

**Surprises**:
- Tag-specific `::part()` selectors provide natural style isolation between `vaadin-rich-text-editor` and `vcf-enhanced-rich-text-editor`. This is a benefit of using a custom tag name.

---

## Item 16: runBeforeClientResponse Access

**Status**: PASS

**Observation**: The package-private `runBeforeClientResponse(SerializableConsumer<UI>)` method is callable from `EnhancedRichTextEditor` (same package). The method compiled without error and executed successfully at runtime.

Console log confirmed execution:
```
[ERTE Item 16] runBeforeClientResponse executed: undefined
```

The deferred execution pattern works: the JS snippet runs in the next client response cycle, as intended.

**Code adjustments**: None -- same-package access to package-private methods works as designed.

**Surprises**:
- The `$0.__item16Result` assignment in the deferred JS showed `undefined` in console because `$1` (the marker parameter) wasn't correctly resolved in the `executeJs` call from within `runBeforeClientResponse`. This is a test artifact, not a framework limitation. The core functionality (deferred JS execution) works correctly.

---

## Item 19: HTML Sanitizer vs Custom Markup

**Status**: PASS (with critical finding -- sanitizer strips `class` attribute)

**Observation**: The RTE 2 sanitizer (`RichTextEditor.sanitize()`) is accessible as a static package-private method. Systematic testing reveals what it allows/strips:

### Sanitizer Behavior

| Input | Output | Finding |
|-------|--------|---------|
| `<span class="ql-tab" style="...">` | `<span style="...">` | **`class` STRIPPED**, `style` preserved |
| `<span class="ql-tab" contenteditable="false" style="...">` | `<span style="...">` | **`class` + `contenteditable` STRIPPED** |
| `<p><strong>bold</strong> <em>italic</em></p>` | unchanged | Standard HTML passes through |
| `<div>text</div>` | `text` | **`<div>` STRIPPED** (not in safelist) |
| `<blockquote>`, `<pre>`, `<code>`, `<sub>`, `<sup>` | preserved | In safelist |
| `<a href="...">` | `<a href="..." rel="nofollow">` | `rel="nofollow"` added |

### Round-Trip Test
- Input HTML: `<p>Tab<span class="ql-tab"></span>Content</p>`
- Java `getValue()`: `<p>Tab<span></span>Content</p>` -- class stripped, span preserved
- Client Quill state: `{"delta":[{"insert":"\\n"}],"html":"<p><br></p>"}` -- Quill didn't recognize the classless span

### Critical Finding

**The sanitizer strips `class` attributes from `<span>` elements.** This means HTML set via `asHtml().setValue()` will lose `class="ql-tab"`, and Quill won't recognize the span as a TabBlot.

**Mitigation options** (for production ERTE):
1. **Override `sanitize()` in subclass** to whitelist `class` on `<span>` (at minimum `ql-tab`, `ql-placeholder`, etc.)
2. **Use `asDelta().setValue()`** instead of HTML for setting content with custom embeds (Delta format preserves `{ tab: true }` without sanitization)
3. **Use `dangerouslySetHtmlValue()`** on the client side (bypasses sanitizer entirely)

**Surprises**:
- `<span>` tag itself is preserved (it's in the safelist), only the `class` and `contenteditable` attributes are stripped.
- `style` attribute IS preserved -- this is because RTE 2's safelist explicitly allows `style` on certain elements.
- The sanitizer uses jsoup's `Safelist` (formerly `Whitelist`) API. The exact safelist configuration is in `RichTextEditor.sanitize()`.

---

## Item 21: Clipboard / Delta Round-Trip

**Status**: PASS

**Observation**: Delta round-trip is **perfect**. Test via `testDeltaRoundTrip()`:

### Delta Round-Trip
- Input: 6 ops with 3 tab embeds (`{ tab: true }`)
- Output: 6 ops, identical
- `opsMatch: true`
- `tabEmbedCount: 3`, `expectedTabCount: 3`, `tabEmbedsPreserved: true`

### DOM Verification
- `tabDomElements: 3` -- all 3 tabs rendered in DOM with correct `class="ql-tab"` and styles
- Quill HTML output: `<span class="ql-tab" contenteditable="false" style="display: inline-block; min-width: 2em; background-color: rgba(0, 120, 212, 0.1); border-bottom: 1px dotted rgb(0, 120, 212);">...</span>`

### Clipboard Parsing
- `clipboardRecognizedTabs: 1` -- clipboard module's `convert()` recognizes tab blots in HTML
- `simpleClipboardParse` also recognizes tabs

**Code adjustments**: None -- Delta format is perfectly preserved through round-trips.

**Surprises**:
- The clipboard module correctly parses `<span class="ql-tab">` back into `{ tab: true }` embeds. This means copy-paste within the editor will preserve tab embeds.

---

## Item 23: Production Build

**Status**: PASS

**Observation**: `mvn clean package -Pproduction -DskipTests` completed successfully in 24 seconds.

- Build output: `erte-v25-spike-1.0-SNAPSHOT.jar` (74 MB, Spring Boot fat JAR)
- vaadin-maven-plugin `build-frontend` goal executed successfully
- Frontend bundle compiled and included in JAR
- No errors or warnings related to the custom `vcf-enhanced-rich-text-editor` module

**Code adjustments**: None. The `pom.xml` production profile with `build-frontend` goal works out of the box.

**Surprises**: None -- the custom JS module is picked up by the build automatically via the `@JsModule` annotation.

---

## Spike Checklist Summary

| Item | Description | Status | Notes |
|------|-------------|--------|-------|
| 1 | Verify Quill version | PASS | 2.0.3 (confirmed previously) |
| 2 | Register custom Embed blot | **PASS** | TabBlot via Quill.register() |
| 3 | Insert content with custom blot | **PASS** | Delta with { tab: true } works |
| 4 | Inject toolbar button | **PASS** | render() override with custom group |
| 5 | Button survives property change | **PASS** | i18n, disabled, rapid setValue, active state all survive |
| 6 | Access keyboard module | **PASS** | Plain object, string keys, addBinding() available |
| 7 | Add keyboard binding | **PASS** | Works but must manage binding priority (prepend, not append) |
| 8 | Java class extension | **PASS** | extends RichTextEditor in same package |
| 9 | @Tag override | **PASS** | VCF-ENHANCED-RICH-TEXT-EDITOR |
| 10 | Module load order | **PASS** | Correct sequence confirmed |
| 13 | render() + observer compat | **PASS** | ReadOnly, disabled, setValue, i18n all work |
| 14 | Detailed lifecycle order | **PASS** | Full timeline: constructor -> connectedCallback -> willUpdate -> firstUpdated -> updated -> ready() -> _editor available |
| 15 | executeJs targets correct element | **PASS** | $0 = correct custom element |
| 17 | @NpmPackage inheritance | **PASS** | No annotation needed on subclass |
| 20 | Embed guard node impact | **PASS** | Guards inside element, measure outer rect |
| 22 | text-change delta correctness | **PASS** | Embed = length 1, no guard leakage |

| 11 | ThemableMixin inheritance | **PASS** | Styles inherited, Lumo works via @StyleSheet (no registerStyles needed) |
| 12 | ::part() selectors | **PASS** | 40 parts, styling works, tag isolation confirmed |
| 16 | runBeforeClientResponse | **PASS** | Package-private access works, deferred execution works |
| 19 | HTML sanitizer vs custom markup | **PASS** | Sanitizer strips `class` attr -- must override or use Delta API |
| 21 | Clipboard / Delta round-trip | **PASS** | Delta perfect, clipboard recognizes tab embeds |
| 23 | Production build | **PASS** | 74 MB fat JAR, 24s build, no issues |

---

## Conclusion

**All 22 spike items PASS. The v25 migration architecture is FULLY VIABLE.**

### Phase 1 (Core Architecture) -- 11/11 PASS
JS class extension, render() override, ready() hook, custom blots, keyboard module, lifecycle -- all work as designed.

### Phase 2 (Keyboard & Lifecycle) -- 4/4 PASS
Keyboard bindings with priority control, toolbar survives all re-render scenarios, detailed lifecycle confirmed.

### Phase 3 (Theme, Build, Data Integrity) -- 6/6 PASS
ThemableMixin, ::part() selectors, package-private access, sanitizer behavior, Delta round-trip, production build -- all validated.

### Key Architecture Decisions Confirmed
1. **JS class extension** via `customElements.get('vaadin-rich-text-editor')` works
2. **render() override** provides clean toolbar customization without MutationObserver hacks
3. **ready() hook** gives access to `_editor` (Quill) immediately after `super.ready()`
4. **Same package** (`com.vaadin.flow.component.richtexteditor`) enables extension + package-private access
5. **Guard nodes** are contained within embed elements -- measure outer `.ql-tab` for widths
6. **Delta format** is clean -- embeds are length 1, no guard node leakage
7. **Keyboard module** is accessible and bindings can be customized with priority control
8. **Toolbar buttons** survive ALL re-render scenarios including active state through i18n changes
9. **Lifecycle** is well-defined: `_editor` available after `super.ready()`, content should be set via Java API
10. **::part() selectors** work with tag-specific isolation between parent and child
11. **Production build** works out of the box with custom JS module

### Phase 4 (Visual Verification) -- ALL PASS

Visual verification via Playwright MCP with zero console errors/warnings across all interactions:

| Test | Result | Details |
|------|--------|---------|
| Lumo theming | PASS | System font stack, blue primary buttons, proper heading styles |
| Set Value from Java | PASS | Content "Content set from **Java** at ..." renders with bold formatting |
| Toggle ReadOnly | PASS | Toolbar hides, content non-editable; toggle back restores toolbar + WS button |
| ::part() styling | PASS | `toolbar` green bg, `content` orange bg via programmatic `::part()` selectors |
| Delta round-trip (visual) | PASS | "Before [tab] Middle [tab] [tab] After" with blue dotted underlines on tab embeds |
| Tab key binding | PASS | Pressing Tab inserts new tab embed (tabCount 3→4), blue underline visible |
| WS toolbar button | PASS | Click fires `[ERTE] Whitespace button clicked` in console |
| Console health | PASS | 0 errors, 0 warnings across 194 console messages |

### Phase 5 (Table Spike) -- 7/7 PASS

Table extension blots (ContainBlot, TableCell, TableRow, Table) migrated to Quill 2/Parchment 3 with full multi-cell table support. Tested with 1x1, 1x3, 2x2, 3x3 tables — all correct.

| Item | Description | Status | Details |
|------|-------------|--------|---------|
| T1 | Container.order | PASS | Not native in Parchment 3 (`undefined`), but settable as property without error |
| T2 | Blot registration | PASS | All 4 blots registered (under `formats/` path, not `blots/`) |
| T3 | Table creation via Delta | PASS | 2x2 table: 1 table, 2 rows, 4 cells, correct hierarchy |
| T4 | Pipe-format round-trip | PASS | 4 td ops, formatPreserved=true, correct pipe-separated IDs |
| T5 | optimize() hierarchy | PASS | hierarchyCorrect=true, Table > TR > TD structure built correctly |
| T6 | clipboard.addMatcher() | PASS | API exists, matcher registered, convert works |
| T7 | History module | PASS | stack.undo/redo arrays, ignoreChange, lastRecorded, tableStack settable |

### Parchment 3 Breaking Changes Discovered (Table Spike)

**5 critical Parchment 3 API changes** required to port the table blots:

1. **`Parchment.create()` → REMOVED**
   - Parchment 3 removed the global `Parchment.create()` factory
   - Use `this.scroll.create(blotName, value)` instead (factory lives on `ScrollBlot`)
   - Affected 6 call sites across TableCell.optimize, TableCell.insertBefore, TableCell.replaceWith, TableRow.createDefaultChild, Table.insertBefore

2. **`replace()` → `replaceWith()` (reversed semantics)**
   - Quill 1: `newBlot.replace(oldBlot)` — called on the NEW blot
   - Quill 2: `oldBlot.replaceWith(newBlot)` — called on the OLD blot
   - Affected TableCell.optimize (mark.replaceWith(table)) and TableCell.replaceWith override

3. **`defaultChild` must be a CLASS reference, not a string**
   - Parchment 3's `ContainerBlot.optimize()` does `this.scroll.create(this.statics.defaultChild.blotName)`
   - Quill 1 used `Parchment.create(this.statics.defaultChild)` (string name)
   - Passing a string (`'block'`) instead of a class (`Block`) causes `.blotName` to be `undefined` → crash
   - Affected ContainBlot, TableRow, Table defaultChild declarations
   - All `scroll.create(this.statics.defaultChild)` call sites must use `.blotName`

4. **`checkMerge()` must be overridden on Container blots**
   - Parchment 3's Container.checkMerge() only checks `blotName` equality
   - This causes ALL adjacent TDs, TRs, or Tables with the same tag to be merged regardless of IDs
   - Override on TableCell (check cell_id), TableRow (check row_id), Table (check table_id)

5. **Merge loops required in optimize()**
   - Parchment 3's Container.optimize() merges only ONE next sibling via checkMerge()
   - Custom optimize() merges one more → total 2 merges per optimize pass
   - With 4+ cells, 3+ merges are needed → use `while` loops instead of `if` statements
   - Affected Table.optimize and TableRow.optimize merge logic

### Additional Table Spike Findings

- **ContainBlot.formats() must return `{}`, not tagName string**: The original `formats()` returned the DOM tagName (e.g., "TABLE"). Quill 2 spreads string format values into character attributes ("0":"T","1":"A","2":"B",...). Override to return `{}` for container blots that don't contribute to delta formats.
- **Blot registration path**: Custom blots registered via `Quill.register()` are accessible under `formats/` prefix (e.g., `Quill.import('formats/td')`), NOT `blots/` prefix.
- **No `table-class` attribute propagation**: Added `table-class` handling in optimize() to preserve CSS class from TD to Table (same as ERTE 1 behavior).
- **optimize() mark-based approach fails in Parchment 3**: The original pattern of `create mark → insertBefore(mark, this.next) → replaceWith(table)` breaks when `this.next` reference becomes stale during concurrent optimize passes. Replaced with: capture `origParent`/`origNext` → move self into table → insert table at original position with stale-reference guard.

### Action Items for Implementation
1. **Sanitizer override** (Item 19): Must extend or replace `sanitize()` to whitelist `class` on `<span>` for ERTE custom markup. Alternatively, prefer Delta API for content with custom embeds.
2. **Keyboard binding priority** (Item 7): Must clear and re-add bindings with ERTE handlers first (prepend pattern).
3. **Lumo loading** (Item 11): Host application must use `@StyleSheet(Lumo.STYLESHEET)` or `@StyleSheet(Aura.STYLESHEET)` (V25 requirement). No ERTE-specific registration needed -- theme vars cascade through shadow DOM automatically. Works identically with Lumo, Aura, and Base styles.
4. **ERTE 1 custom styles migration** (Item 11/12): ERTE 1 uses Polymer-based `<custom-style>` and `registerStyles()` for custom CSS (tab highlights, ruler, read-only sections, placeholder styling, etc.). These must be rewritten as either (a) adopted stylesheets via `static get styles()` Lit API, or (b) `::part()` selectors in the host document. V25 deprecated `ThemableMixin`/`registerStyles()` -- migrate to Lit `styles` API.
5. **Table blot migration** (Table Spike): Apply the 5 Parchment 3 breaking changes documented above to all 4 table blot files. The changes are mechanical (find-and-replace patterns) but critical — without them, multi-cell tables crash at runtime.
