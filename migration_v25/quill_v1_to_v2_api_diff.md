# Quill v1 to v2 API Differences: ERTE Migration Impact

This document catalogues the concrete API-level differences between Quill.js v1 (1.3.6) and Quill.js v2 (2.0.x) that are relevant to the Enhanced Rich Text Editor (ERTE) codebase. Each section documents what the ERTE currently uses, what changed in v2, and the migration impact.

---

## 1. Global Object / Import Pattern

### What ERTE Currently Uses (Quill 1)

The ERTE loads Quill from a bundled vendor file (`vendor/vaadin-quill.js`) that sets `window.Quill` as a global, then all JS files reference it:

```javascript
// vcf-enhanced-rich-text-editor-blots.js, line 1
const Quill = window.Quill;

// vcf-enhanced-rich-text-editor.js, lines 28, 37
const Quill = window.Quill;

// erte-table/index.js, line 13
const Quill = window.Quill;
```

### What Changed in Quill 2

Quill 2 is distributed as an ES module package. The CDN/UMD build still exposes `window.Quill`, but the npm/bundler approach uses standard ES imports:

```javascript
import Quill from 'quill';           // full build
import Quill from 'quill/core';      // core build (no default formats)
```

`Quill.import()` still works in v2 for retrieving registered items. The `imports` static property still exists as `Record<string, unknown>`.

### Migration Impact: MODERATE

If ERTE 2 is based on the Vaadin RTE 2 (which likely uses Quill 2 as an ES module), the `window.Quill` global pattern may or may not be available depending on how Vaadin bundles it. The ERTE extension code will need to match whatever import pattern the RTE 2 uses. If `window.Quill` is still exposed, the existing pattern can work; otherwise, all files need to switch to ES module imports.

---

## 2. Delta Constructor Access

### What ERTE Currently Uses (Quill 1)

The ERTE accesses the Delta constructor through `Quill.imports.delta` (the internal imports map directly):

```javascript
// vcf-enhanced-rich-text-editor.js, lines 1202, 1206
const Delta = Quill.imports.delta;
return new Delta().insert({ tab: true });

// vcf-enhanced-rich-text-editor.js, line 2178
new Quill.imports.delta()
    .retain(range.index)
    .delete(range.length)
    .insert({ image })

// vcf-enhanced-rich-text-editor.js, line 2273
const delta = new Quill.imports.delta(parsedValue);
```

The tables extension imports Delta directly from npm:
```javascript
// erte-table/index.js, line 2
import Delta from 'quill-delta';
```

### What Changed in Quill 2

In Quill 2, `Quill.imports` still exists as a static property and `Quill.imports.delta` still resolves to the Delta class. However, the recommended approach is either:

```javascript
const Delta = Quill.import('delta');    // using Quill.import() method
// or
import Delta from 'quill-delta';       // direct npm import
```

Direct access to `Quill.imports` is documented as unsupported; the official API is `Quill.import()` (note: method vs property).

### Migration Impact: LOW-MODERATE

- Replace `Quill.imports.delta` with `Quill.import('delta')` or direct ES import
- Replace `new Quill.imports.delta()` with `new Delta()` using an imported reference
- The tables extension already uses the correct `import Delta from 'quill-delta'` pattern

**Affected locations:**
- `vcf-enhanced-rich-text-editor.js` lines 1202, 1206, 2178, 2273

---

## 3. Parchment / Blot Registration

### What ERTE Currently Uses (Quill 1)

Blots are retrieved via `Quill.import()` with v1 path names and registered with `Quill.register()`:

```javascript
// vcf-enhanced-rich-text-editor-blots.js, lines 2-8
const BlockEmbed = Quill.import('blots/block/embed');
const Block = Quill.import('blots/block');
const Inline = Quill.import('blots/inline');
const Embed = Quill.import('blots/embed');
const TextBlot = Quill.import('blots/text');
const ListItem = Quill.import('formats/list/item');
const ListContainer = Quill.import('formats/list');

// Registration
Quill.register(ReadOnlyBlot);        // auto-detects format path from blotName
Quill.register(TabBlot);
Quill.register(SoftBreakBlot);
Quill.register(PlaceholderBlot);
Quill.register({ 'formats/nbsp': Nbsp });  // explicit path

// Tables extension
const Container = Quill.import('blots/container');
const Parchment = Quill.import('parchment');
const Scroll = Quill.import('blots/scroll');
Quill.register('modules/table', TableModule);
```

### What Changed in Quill 2

**Import paths are the same.** `Quill.import('blots/inline')`, `Quill.import('blots/embed')`, etc. still work. The returned classes are now the Parchment v3 versions with different constructor signatures.

**Parchment export names changed** (when accessing from the Parchment library directly):
- `Parchment.Scroll` -> `Parchment.ScrollBlot`
- `Parchment.Embed` -> `Parchment.EmbedBlot`
- `Parchment.Block` -> `Parchment.BlockBlot`
- `Parchment.Inline` -> `Parchment.InlineBlot`
- `Parchment.Text` -> `Parchment.TextBlot`
- `Parchment.Container` -> `Parchment.ContainerBlot`
- `Parchment.Attributor.Class` -> `Parchment.ClassAttributor`
- `Parchment.Attributor.Style` -> `Parchment.StyleAttributor`

**Per-instance Registry:** Quill 2 added a `registry` option to the constructor, enabling multiple editors with different format sets on the same page. `Quill.register()` still works for global registration.

**Static `register()` on blots:** Blot classes can now have a static `register()` method for registering dependent blots.

### Migration Impact: LOW

The `Quill.import('blots/...')` paths still work. `Quill.register()` still works. The main risk is if code accesses Parchment classes directly by old names (e.g., `Parchment.Embed` instead of `Parchment.EmbedBlot`). The tables extension does `Quill.import('parchment')` and uses `Parchment` -- any code using `Parchment.Scope` or other subproperties needs checking.

**Affected locations:**
- All blot files in tables extension that do `const Parchment = Quill.import('parchment')`

---

## 4. Blot Constructor Signature

### What ERTE Currently Uses (Quill 1)

Quill 1/Parchment v1 blot constructor: `constructor(domNode, value)`

```javascript
// vcf-enhanced-rich-text-editor-blots.js, lines 139-142
class PlaceholderBlot extends Embed {
  constructor(node, value) {
    super(node, value);
    this.applyFormat();
  }
}
```

All other ERTE blots (ReadOnlyBlot, TabBlot, SoftBreakBlot) do NOT define custom constructors -- they only override `static create()` and inherit the default.

### What Changed in Quill 2

Quill 2/Parchment v3 blot constructor: `constructor(scroll, domNode, value)`

The `scroll` (ScrollBlot instance) is now the FIRST parameter. This is required for all blots.

```javascript
// Quill 2 pattern
class MyEmbed extends Embed {
  constructor(scroll, node, value) {
    super(scroll, node, value);
    // custom init
  }
}
```

### Migration Impact: HIGH (for PlaceholderBlot)

- **PlaceholderBlot** has a custom constructor that must be updated to accept `scroll` as the first parameter
- ReadOnlyBlot, TabBlot, SoftBreakBlot have no custom constructors, so they inherit the correct signature automatically
- Any blot in the tables extension with custom constructors needs the same update

**Affected locations:**
- `vcf-enhanced-rich-text-editor-blots.js` line 139: `PlaceholderBlot.constructor(node, value)` -> `constructor(scroll, node, value)`
- All table blots: check each for custom constructors

---

## 5. Inline.order (Blot Ordering)

### What ERTE Currently Uses (Quill 1)

```javascript
// vcf-enhanced-rich-text-editor.js, line 32
Inline.order.push(PlaceholderBlot.blotName, ReadOnlyBlot.blotName);
```

This controls nesting priority of inline blots in the DOM.

### What Changed in Quill 2

`Inline.order` still exists in Quill 2. It is still an array of blot names controlling DOM nesting order. This is not an official API but a de-facto pattern.

### Migration Impact: LOW

The pattern should still work. Verify that `Quill.import('blots/inline').order` is still an accessible array.

---

## 6. Container.order (Tables Extension)

### What ERTE Currently Uses (Quill 1)

```javascript
// erte-table/index.js, lines 26-29
Container.order = [
  'list', 'contain',
  'td', 'tr', 'table'
];
```

### What Changed in Quill 2

Container blots in Parchment v3 may have changed ordering mechanisms. This is an internal API.

### Migration Impact: UNKNOWN - NEEDS INVESTIGATION

The `Container.order` property may or may not exist in Parchment v3. This needs testing.

---

## 7. Keyboard Bindings: Key Specification

### What ERTE Currently Uses (Quill 1)

The ERTE uses NUMERIC KEY CODES extensively for keyboard bindings:

```javascript
// vcf-enhanced-rich-text-editor.js
const DELETE_KEY = 46;
const BACKSPACE_KEY = 8;
const TAB_KEY = 9;
const ARROW_UP = 38;
const ARROW_DOWN = 40;
const Z_KEY = 90;
const V_KEY = 86;

// Binding by numeric keyCode:
keyboard.bindings[TAB_KEY] = [tabStopBinding, ...originalBindings, moveFocusBinding];
keyboard.bindings[13] = [softBreakBinding, hardBreakBinding, ...enterBindings];
keyboard.bindings[BACKSPACE_KEY] = [{ key: BACKSPACE_KEY, handler: ... }, ...];
keyboard.bindings[ARROW_UP] = [{ key: ARROW_UP, shiftKey: false, handler: ... }];

// addBinding with numeric codes:
keyboard.addBinding({ key: 121, altKey: true, handler: focusToolbar }); // F10
keyboard.addBinding({ key: 80, shortKey: true }, () => ...);            // Ctrl+P

// addBinding with string key:
keyboard.addBinding({ key: ' ', shiftKey: true }, () => ...);           // Shift+Space
```

The tables extension uses STRING key names:
```javascript
// erte-table/index.js
{ key: 'tab', handler: ... }
{ key: 'backspace', handler: ... }
{ key: 'delete', handler: ... }
{ key: 'z', shortKey: true, handler: ... }
{ key: 'a', shortKey: true, handler: ... }
```

### What Changed in Quill 2

Quill 2's keyboard module does a **dual lookup** for bindings:

```typescript
const bindings = (this.bindings[evt.key] || []).concat(
  this.bindings[evt.which] || []
);
```

This means bindings stored under EITHER the `evt.key` string name (e.g., `"Tab"`, `"Enter"`, `"Backspace"`) OR the `evt.which` numeric code will be found. However:

- **Binding `key` is no longer case insensitive** -- `'b'` and `'B'` are different
- **Array support** -- `key` can be an array: `key: ['b', 'B']`
- **Native keyboard event** is now passed to handlers as an additional argument
- The `normalize()` function accepts both string and number keys

### Migration Impact: MODERATE

The dual lookup means existing NUMERIC key bindings will still be found via `evt.which`. However:

1. **Direct bindings array manipulation is fragile.** The ERTE heavily manipulates `keyboard.bindings[keyCode]` arrays directly (prepending, filtering, replacing). If Quill 2 uses string keys as the primary storage key (e.g., `keyboard.bindings["Tab"]` instead of `keyboard.bindings[9]`), all direct array access breaks.

2. **The main risk:** When Quill 2 registers its DEFAULT bindings, they are stored under STRING keys (e.g., `keyboard.bindings["Tab"]`). The ERTE code that reads `keyboard.bindings[TAB_KEY]` (where `TAB_KEY = 9`) will get `undefined` instead of the existing Quill bindings. The code then replaces these bindings with its own, but the Quill default bindings under the string key remain and also fire.

3. **Handler signature changed:** v2 handlers receive `(range, context, binding)` -- the third parameter is the binding object itself. This is additive and shouldn't break existing handlers that only use 2 params.

**Affected locations (HIGH RISK):**
- `vcf-enhanced-rich-text-editor.js` lines 1685-1871: ALL keyboard binding manipulation
- `vcf-enhanced-rich-text-editor.js` lines 1874, 1877, 1883: `addBinding` with numeric keys
- `vcf-enhanced-rich-text-editor.js` lines 2580-2663: custom button keyboard shortcuts

**Required changes:**
- All `keyboard.bindings[numericKeyCode]` access must change to string key names
- All binding objects with `key: numericCode` should change to `key: 'KeyName'`
- Key name mapping: `9` -> `'Tab'`, `13` -> `'Enter'`, `8` -> `'Backspace'`, `46` -> `'Delete'`, `38` -> `'ArrowUp'`, `40` -> `'ArrowDown'`, `90` -> `'z'`, `86` -> `'v'`, `121` -> `'F10'`, `80` -> `'p'`

---

## 8. Keyboard Binding Direct Array Manipulation

### What ERTE Currently Uses (Quill 1)

The ERTE heavily manipulates the internal `keyboard.bindings` object, prepending custom bindings before Quill's defaults:

```javascript
// Pattern used repeatedly throughout vcf-enhanced-rich-text-editor.js
const keyboard = this._editor.getModule('keyboard');
const bindings = keyboard.bindings[TAB_KEY];
keyboard.bindings[TAB_KEY] = [customBinding, ...originalBindings, anotherBinding];

keyboard.bindings[13] = [softBreakBinding, hardBreakBinding, ...enterBindings];
keyboard.bindings[BACKSPACE_KEY] = [customBinding, ...backspaceKeyBindings];
```

This pattern:
1. Gets existing bindings array for a key
2. Creates a new array with custom bindings prepended (higher priority)
3. Replaces the entire bindings array

### What Changed in Quill 2

The `keyboard.bindings` object still exists and is directly accessible. However, bindings are now stored under `evt.key` string names rather than numeric keyCode values. The dual lookup (`evt.key` and `evt.which`) means both old and new bindings can coexist but may cause double-firing.

Quill 2 recommends using configuration-time binding registration rather than post-initialization manipulation:
> "Quill's default handlers are added at initialization, the only way to prevent them is to add yours in the configuration"

### Migration Impact: HIGH

This is the single most impactful change. The entire binding manipulation pattern must be updated to use string key names. Additionally, care must be taken to avoid double-firing when bindings exist under both numeric and string keys.

---

## 9. Clipboard Matchers

### What ERTE Currently Uses (Quill 1)

```javascript
// vcf-enhanced-rich-text-editor.js, lines 1200-1216
this._editor.clipboard.addMatcher('TAB', (node, delta) => {
  const Delta = Quill.imports.delta;
  return new Delta().insert({ tab: true });
});
this._editor.clipboard.addMatcher('PRE-TAB', (node, delta) => { ... });
this._editor.clipboard.addMatcher('TABS-CONT', (node, delta) => { return delta; });
this._editor.clipboard.addMatcher('LINE-PART', (node, delta) => { return delta; });
this._editor.clipboard.addMatcher('.ql-placeholder', (node, delta) => { ... });

// Tables extension, lines 59-75
clipboard.addMatcher('TABLE', function (node, delta) { ... });
clipboard.addMatcher('TR', function (node, delta) { ... });
clipboard.addMatcher('TD, TH', function (node, delta) { ... });
```

### What Changed in Quill 2

- `addMatcher(selector, callback)` API is **unchanged** -- same signature, same behavior
- `convert()` method signature changed: now accepts both HTML and text
- `matchVisual` option removed (only semantic whitespace interpretation now)
- `pasteHTML` removed, use `dangerouslyPasteHTML` instead

### Migration Impact: LOW

The `addMatcher` API is unchanged. The main ERTE clipboard usage should work as-is. Only concern is that the matchers for custom HTML tags (`TAB`, `PRE-TAB`, `TABS-CONT`, `LINE-PART`) must still produce valid Quill 2 deltas, which they do since the delta format itself hasn't changed.

---

## 10. History Module Internals

### What ERTE Currently Uses (Quill 1)

The ERTE directly accesses history module internals:

```javascript
// vcf-enhanced-rich-text-editor.js, lines 1951-1960
const historyStack = this._editor.history.stack;
const undo = historyStack.undo[historyStack.undo.length - 1] || [];
if (undo && undo.undo) this._emitPlaceholderHistoryEvents(undo.undo.ops);

const redo = historyStack.redo[historyStack.redo.length - 1] || [];
if (redo && redo.redo) this._emitPlaceholderHistoryEvents(redo.redo.ops);

// Direct method calls
this._editor.history.undo();
this._editor.history.redo();
```

The tables extension has even deeper access:

```javascript
// erte-table/js/TableHistory.js
quill.history.ignoreChange = true;
quill.history.lastRecorded = 0;
quill.history.tableStack[id] = TableHistory.changes;  // custom property on history
quill.history.stack.undo.push({ type: 'tableHistory', id: id });
quill.history.stack.redo.pop();
quill.history.stack.undo.push(historyEntry);

// erte-table/index.js
quill.history.tableStack = {};
quill.history.cutoff();
```

### What Changed in Quill 2

**Stack item structure changed fundamentally:**

Quill 1 StackItem:
```javascript
{ undo: Delta, redo: Delta }
// Accessed as: entry.undo.ops, entry.redo.ops
```

Quill 2 StackItem:
```javascript
{ delta: Delta, range: Range | null }
// The delta is the INVERSE of the change (for undo)
```

In v1, each stack entry stored both the forward and reverse deltas. In v2, only the inverse delta is stored (plus cursor range), and the forward delta is computed by inverting again.

**Other internal changes:**
- `history.ignoreChange` still exists as a boolean flag
- `history.lastRecorded` still exists as a timestamp
- `history.cutoff()` still exists
- `history.undo()` and `history.redo()` still exist
- `history.stack.undo` and `history.stack.redo` are still arrays

### Migration Impact: HIGH

1. **ERTE placeholder history code** accesses `entry.undo.ops` and `entry.redo.ops` -- these properties no longer exist. Must change to `entry.delta.ops`.

2. **Tables extension** pushes custom entries onto the history stack: `{ type: 'tableHistory', id: id }`. This custom structure is incompatible with v2's expected `{ delta: Delta, range: Range }` format. The entire TableHistory system needs redesign.

3. **Tables extension** manipulates `ignoreChange` and `lastRecorded` directly -- these still exist but their semantics may have subtle differences.

**Affected locations:**
- `vcf-enhanced-rich-text-editor.js` lines 1950-1961: `_undoPlaceholderEvents`, `_redoPlaceholderEvents`
- `erte-table/js/TableHistory.js`: entire file
- `erte-table/js/TableTrick.js` lines 717-735: history stack access
- `erte-table/index.js` line 42: `quill.history.tableStack = {}`

---

## 11. `__quill` Property on Container Element

### What ERTE Currently Uses (Quill 1)

```javascript
// vcf-enhanced-rich-text-editor-blots.js, lines 55-58
const containerEl = node.closest('.ql-container');
if (!containerEl || !containerEl.__quill) return;
const quill = containerEl.__quill;
```

### What Changed in Quill 2

Quill 2 uses a `WeakMap` (or `Map`) called `instances` instead of setting `__quill` directly on the DOM element:

```typescript
// Quill v2 source
instances.set(this.container, this);
```

The `Quill.find(domNode)` static method still works and is the official way to retrieve the Quill instance for a DOM node.

### Migration Impact: MODERATE

The `__quill` property access in TabBlot must be replaced with `Quill.find(containerEl)`:

```javascript
// Before (v1)
const quill = containerEl.__quill;

// After (v2)
const quill = Quill.find(containerEl);
```

**Affected locations:**
- `vcf-enhanced-rich-text-editor-blots.js` lines 55-58

---

## 12. `__blot` Property on DOM Nodes

### What ERTE Currently Uses (Quill 1)

```javascript
// erte-table/connector.js, line 101
const firstRow = selectedTable.__blot?.blot?.children?.head;
```

In Parchment v1, each DOM node has a `__blot` property containing `{ blot: BlotInstance }`.

### What Changed in Quill 2

Parchment v3 uses a Registry `DATA_KEY` property on DOM nodes instead of `__blot`. The recommended way to get a blot from a DOM node is `Quill.find(domNode)` or `Registry.find(domNode)`.

### Migration Impact: MODERATE

Replace `domNode.__blot.blot` with `Quill.find(domNode)` or the Parchment registry's find method.

**Affected locations:**
- `erte-table/connector.js` line 101

---

## 13. `scroll.descendant()` Method

### What ERTE Currently Uses (Quill 1)

```javascript
// vcf-enhanced-rich-text-editor.js, lines 1990, 1999
const [readOnlySection] = this._editor.scroll.descendant(ReadOnlyBlot, range.index);
const [link, offset] = this._editor.scroll.descendant(LinkBlot, range.index);
```

### What Changed in Quill 2

The `scroll.descendant()` method may still exist in Parchment v3 ScrollBlot. Quill 2 provides `quill.getLeaf()` and `quill.getLine()` as public API alternatives.

### Migration Impact: LOW-MODERATE (NEEDS INVESTIGATION)

Test whether `scroll.descendant()` still works in Parchment v3. If not, replace with a combination of `quill.getLeaf()` and manual blot traversal.

**Affected locations:**
- `vcf-enhanced-rich-text-editor.js` lines 1990, 1999

---

## 14. `Quill.imports['modules/toolbar']` Access

### What ERTE Currently Uses (Quill 1)

```javascript
// vcf-enhanced-rich-text-editor.js, line 1431
const clean = Quill.imports['modules/toolbar'].DEFAULTS.handlers.clean;
```

### What Changed in Quill 2

`Quill.imports` still exists as a static object, and `Quill.import('modules/toolbar')` returns the toolbar module class. The `DEFAULTS` static property on modules may or may not exist in v2.

### Migration Impact: LOW-MODERATE

Replace `Quill.imports['modules/toolbar']` with `Quill.import('modules/toolbar')` and verify that `.DEFAULTS.handlers.clean` still exists.

**Affected locations:**
- `vcf-enhanced-rich-text-editor.js` line 1431

---

## 15. `Quill.imports['formats/link']` Access

### What ERTE Currently Uses (Quill 1)

```javascript
// vcf-enhanced-rich-text-editor.js, line 1998
const LinkBlot = Quill.imports['formats/link'];
```

### What Changed in Quill 2

Same as above -- replace with `Quill.import('formats/link')`.

### Migration Impact: LOW

**Affected locations:**
- `vcf-enhanced-rich-text-editor.js` line 1998

---

## 16. Quill.sources Constants

### What ERTE Currently Uses (Quill 1)

```javascript
// Used throughout both main JS and blots
Quill.sources.USER
Quill.sources.API
Quill.sources.SILENT
```

### What Changed in Quill 2

`Quill.sources` still exists and still has `USER`, `API`, `SILENT` values. Defined as `static sources = Emitter.sources`.

### Migration Impact: NONE (drop-in compatible)

---

## 17. Quill.find() Static Method

### What ERTE Currently Uses (Quill 1)

```javascript
// vcf-enhanced-rich-text-editor-blots.js, line 60
const blot = Quill.find(node);

// erte-table/index.js, lines 301, 304
nextBlot = Quill.find(line.parent.domNode.closest('table').nextSibling);
blot = Quill.find(line.parent.domNode);
```

### What Changed in Quill 2

`Quill.find(domNode, bubble?)` still exists with the same signature.

### Migration Impact: NONE (drop-in compatible)

---

## 18. Embed Blot Guard Nodes

### What ERTE Currently Uses (Quill 1)

Quill 1's Embed blot is relatively simple -- the DOM node is the embed itself.

```javascript
// TabBlot create()
const node = super.create();
node.setAttribute('contenteditable', 'false');
node.innerText = '\u200B'; // Zero-width space
```

### What Changed in Quill 2

Quill 2's Embed blot wraps content in a `contentNode` span and adds left/right guard text nodes (`\uFEFF`):

```
[guard-left: \uFEFF] [contentNode: <span contenteditable="false">...</span>] [guard-right: \uFEFF]
```

The constructor adds this structure automatically. The `create()` static method creates the outer node, then the constructor wraps children in `contentNode`.

### Migration Impact: HIGH (for custom Embeds)

The TabBlot currently sets `node.innerText = '\u200B'` and `contenteditable = false` on the node returned by `super.create()`. In Quill 2, this content will be moved into the `contentNode` wrapper by the constructor. The guard nodes will surround it. This changes:

1. **DOM structure** -- CSS selectors targeting `.ql-tab` directly may need updating
2. **Click handlers** -- The mousedown handler on the node may interact differently with guard nodes
3. **Width calculations** -- Tab width measurement via `getBoundingClientRect()` may include guard nodes
4. **The `\u200B` character** may conflict with the guard `\uFEFF` characters

The SoftBreakBlot sets `node.innerHTML = '<br>'` which will also be affected by the wrapper.

**Affected locations:**
- `vcf-enhanced-rich-text-editor-blots.js` lines 44-77 (TabBlot)
- `vcf-enhanced-rich-text-editor-blots.js` lines 101-107 (SoftBreakBlot)
- `vcf-enhanced-rich-text-editor-blots.js` lines 138-251 (PlaceholderBlot)
- All tab width calculation code in `vcf-enhanced-rich-text-editor.js`

---

## 19. Blot Traversal APIs

### What ERTE Currently Uses (Quill 1)

```javascript
// Blot tree traversal (Parchment v1)
line.children.head           // first child blot
currentBlot.next             // next sibling blot
currentBlot.statics.blotName // blot name via statics
currentBlot.length()         // blot length
blot.offset(quill.scroll)    // offset from scroll root
line.parent.domNode          // parent blot's DOM node
```

### What Changed in Quill 2

These Parchment linked-list traversal APIs (`children.head`, `.next`, `.prev`, `.parent`, `.statics`) still exist in Parchment v3.

### Migration Impact: NONE (drop-in compatible)

The linked-list blot traversal API is stable across Parchment versions.

---

## 20. List Markup Change

### What ERTE Currently Uses (Quill 1)

Quill 1 renders unordered lists as `<ul>` and ordered lists as `<ol>`.

### What Changed in Quill 2

All lists now use `<ol>` with a `data-list` attribute to differentiate between ordered and unordered. This changes how list HTML is generated and parsed.

### Migration Impact: MODERATE (if ERTE processes list HTML)

Any code or CSS that targets `<ul>` elements for bullet lists will break. Clipboard matchers that process list HTML need updating.

---

## 21. Code Block Markup Change

### What ERTE Currently Uses (Quill 1)

Code blocks render as `<pre>` elements.

### What Changed in Quill 2

Code blocks now use `<div>` elements (to support syntax highlighting).

### Migration Impact: LOW

Any CSS targeting `pre` for code blocks needs updating. The ERTE toolbar has code-block support but likely relies on Quill's internal rendering.

---

## 22. Removed Options

### scrollingContainer

Quill 1 supported `scrollingContainer` option. Quill 2 automatically detects the scrollable ancestor.

### strict

The `strict` option is removed.

### matchVisual (Clipboard)

The `matchVisual` clipboard option is removed. Only semantic whitespace interpretation is used.

### Migration Impact: LOW

Check if ERTE passes any of these options to the Quill constructor.

---

## Summary: Migration Impact by Severity

### HIGH IMPACT (requires significant rewrite)

| Area | Issue | Files |
|------|-------|-------|
| Keyboard bindings | Numeric keyCode -> string key names, direct array manipulation | `vcf-enhanced-rich-text-editor.js` (30+ lines) |
| History stack structure | `{undo: Delta, redo: Delta}` -> `{delta: Delta, range: Range}` | `vcf-enhanced-rich-text-editor.js`, `TableHistory.js`, `TableTrick.js` |
| Embed blot DOM structure | Guard nodes, contentNode wrapper | All custom Embed blots, tab width calculations |
| Blot constructor signature | New `scroll` parameter required | `PlaceholderBlot` constructor |

### MODERATE IMPACT (needs targeted changes)

| Area | Issue | Files |
|------|-------|-------|
| `__quill` property | Replaced with Map lookup | `vcf-enhanced-rich-text-editor-blots.js` |
| `__blot` property | Replaced with registry find | `erte-table/connector.js` |
| Delta constructor access | `Quill.imports.delta` -> `Quill.import('delta')` | `vcf-enhanced-rich-text-editor.js` (4 locations) |
| Global import pattern | `window.Quill` may not be available | All JS files |
| List markup | `<ul>` -> `<ol>` with data attributes | CSS, clipboard matchers |
| `scroll.descendant()` | May need replacement | `vcf-enhanced-rich-text-editor.js` (2 locations) |

### LOW IMPACT (drop-in or minor changes)

| Area | Issue | Files |
|------|-------|-------|
| `Quill.sources` | Unchanged | N/A |
| `Quill.find()` | Unchanged | N/A |
| Blot traversal | Unchanged | N/A |
| `addMatcher` clipboard API | Unchanged | N/A |
| `Quill.register()` | Unchanged | N/A |
| `Inline.order` | Likely unchanged | N/A |
| Toolbar module | Unchanged | N/A |

---

## Sources

- [Quill v2 Upgrading Guide](https://quilljs.com/docs/upgrading-to-2-0)
- [Quill v2 API](https://quilljs.com/docs/api)
- [Quill v2 Keyboard Module](https://quilljs.com/docs/modules/keyboard)
- [Quill v2 Clipboard Module](https://quilljs.com/docs/modules/clipboard)
- [Quill v2 Toolbar Module](https://quilljs.com/docs/modules/toolbar)
- [Quill v2 Delta Format](https://quilljs.com/docs/delta)
- [Quill v2 Modules](https://quilljs.com/docs/modules)
- [Quill v2 Customization](https://quilljs.com/docs/customization)
- [Quill v2 Installation](https://quilljs.com/docs/installation)
- [Parchment v3 README](https://github.com/slab/parchment/blob/main/README.md)
- [Parchment Releases](https://github.com/slab/parchment/releases)
- [Quill v2 Keyboard Source (TypeScript)](https://github.com/slab/quill/blob/main/packages/quill/src/modules/keyboard.ts)
- [Quill v2 History Source (TypeScript)](https://github.com/slab/quill/blob/main/packages/quill/src/modules/history.ts)
- [Quill v2 Core Source (TypeScript)](https://github.com/slab/quill/blob/main/packages/quill/src/core/quill.ts)
- [Quill v2 Embed Blot Source (TypeScript)](https://github.com/slab/quill/blob/main/packages/quill/src/blots/embed.ts)
- [Quill GitHub Issue #1091: Migrate to event.key](https://github.com/slab/quill/issues/1091)
- [Quill GitHub Issue #4032: Keybindings not executed](https://github.com/slab/quill/issues/4032)
