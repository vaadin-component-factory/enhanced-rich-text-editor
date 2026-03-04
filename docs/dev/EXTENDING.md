# Extending ERTE V25

How to add your own content types, toolbar components, keyboard shortcuts, and styling to ERTE. For understanding the internals first, see [`ARCHITECTURE.md`](./ARCHITECTURE.md).

## The Extension Pattern

ERTE is designed so that extensions add functionality **without subclassing** the Java component. The Table addon is the reference implementation — it extends ERTE with table support using only public APIs: toolbar helpers, extension hooks, and `executeJs()`.

The key benefit: you never need to modify ERTE source code. Extensions work entirely through ERTE's public API — Java methods, toolbar slots, JS hooks, and sanitizer registration. Your extension is a separate module with its own `@JsModule` connector, its own Java class, and its own tests. When ERTE is updated, your extension keeps working as long as the public API stays stable.

Multiple extensions can be active on the same editor simultaneously — for example, Tables and a hypothetical Footnotes extension. Each extension manages its own blots, toolbar buttons, events, and styles independently:

```java
EnhancedRichTextEditor editor = new EnhancedRichTextEditor();

// Each extension enables itself on the same editor instance
EnhancedRichTextEditorTables tables =
    EnhancedRichTextEditorTables.enable(editor);
MyFootnotesExtension footnotes =
    MyFootnotesExtension.enable(editor);
```

This works because every layer of the architecture supports parallel extensions:

| Layer | How extensions coexist |
|-------|----------------------|
| **JS hooks** | `extendQuill` and `extendEditor` are arrays — each extension pushes its callbacks, all execute in order |
| **Blot registration** | Each extension registers its own blots under unique format names (`formats/td`, `formats/footnote`) |
| **Toolbar** | Each extension places components in different slots — or shares a slot without conflict |
| **CSS** | Each extension injects a `<style>` element with a unique ID into the shadow root |
| **Events** | Each extension dispatches its own CustomEvents (`table-selected`, `footnote-clicked`) and registers its own Java-side listeners |
| **Sanitizer** | Each extension registers its CSS classes and attributes via `addAllowedHtmlClasses()` |
| **Connector namespace** | Each extension lives under its own key (`extensions.tables`, `extensions.footnotes`) |

The rest of this guide walks through each of these layers.

---

## Extension Hooks

ERTE provides two JavaScript hooks for extensions that need to register Quill blots or interact with the editor instance. These hooks run at specific lifecycle points — getting the timing right matters.

### `extendQuill` — Before Editor Creation

Called with the Quill class before `super.ready()`. Use this to register custom blots:

```javascript
const extNs = window.Vaadin.Flow.vcfEnhancedRichTextEditor;
extNs.extendQuill = extNs.extendQuill || [];
extNs.extendQuill.push((Quill) => {
  const Inline = Quill.import('blots/inline');
  class HighlightBlot extends Inline { /* ... */ }
  Quill.register('formats/highlight', HighlightBlot, true);
});
```

The Table addon uses this to register its four blots (ContainBlot, TableCell, TableRow, Table):

```javascript
// From connector.js — tables register their blots in extendQuill
extNs.extendQuill.push(function(Quill) {
  const Container = Quill.import('blots/container');
  Container.order = ['list', 'contain', 'td', 'tr', 'table'];

  Quill.register('formats/contain', ContainBlot, true);
  Quill.register('formats/td', TableCell, true);
  Quill.register('formats/tr', TableRow, true);
  Quill.register('formats/table', Table, true);
});
```

### `extendEditor` — After Editor Creation

Called with the Quill instance after `super.ready()`. Use this to add event handlers, keyboard bindings, or DOM listeners:

```javascript
const extNs = window.Vaadin.Flow.vcfEnhancedRichTextEditor;
extNs.extendEditor = extNs.extendEditor || [];
extNs.extendEditor.push((editor, Quill) => {
  editor.on('text-change', (delta, oldDelta, source) => {
    console.log('Editor changed:', delta);
  });
});
```

The Table addon uses this to initialize the table module, wire up mouse events for cell selection, and set up keyboard handlers:

```javascript
// From connector.js — tables init their module in extendEditor
extNs.extendEditor.push(function(editor, Quill) {
  initTableModule(editor, Quill);
  const container = editor.container;
  container.addEventListener('mousedown', e => TableSelection.mouseDown(editor, e));
  // ... more event wiring
});
```

### Loading Extensions

Load your extension connector via `@JsModule` on your view or layout:

```java
@JsModule("./src/erte-table/connector.js")  // Table addon
public class EnhancedRichTextEditorTables { ... }
```

Both hook arrays accept multiple callbacks — they execute in push order. Extensions can safely push to the arrays before or after ERTE core loads (the namespace is initialized defensively).

> **Note:** `extendOptions` is deprecated in V25. Use `extendQuill` and `extendEditor` instead.

---

## Custom Blots

To add your own content types — things like special tokens, badges, or inline widgets — you need a custom Quill blot.

### Embed Blot Lifecycle

Three things to get right:

1. **`static create(value)`** runs before the constructor. Initialize outer DOM here. In Quill 2, `create()` must return the created DOM node.
2. **Constructor** — `contentNode` is created by the Embed base class. Set text and configure the visual appearance here.
3. **Guard nodes** — Quill 2 places invisible zero-width characters inside Embed domNodes so the cursor can sit next to them. Never set `contenteditable="false"` on the outer domNode — the inner `contentNode` already has it.

### Inline Blot vs Embed Blot

- **Inline** (like ReadOnlyBlot): wraps existing text content, applies formatting. Override `static formats(domNode)` and `format(name, value)`.
- **Embed** (like TabBlot, PlaceholderBlot): discrete non-text element. Override `static create(value)`, `static value(domNode)`, and the constructor.

---

## Extending the Toolbar

### Custom Components

Add any Java component to any toolbar slot:

```java
Button customButton = new Button("My Button");
customButton.addClickListener(e -> { /* handle click */ });
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, customButton);
```

**Available slots:** `START`, `END`, `BEFORE_GROUP_*`, `AFTER_GROUP_*`, `GROUP_CUSTOM`. Custom components automatically inherit the native RTE 2 toolbar styling via the `toolbar-custom-component` part — hover effects, focus rings, active state, and disabled state work out of the box.

### Toolbar Helper Classes

ERTE ships helper classes for common toolbar UI patterns. The Table addon uses all of them:

**ToolbarSwitch** — Toggle button with active/inactive state:

```java
// Table addon: "Add Table" button
addTableButton = new ToolbarSwitch(VaadinIcon.TABLE, VaadinIcon.PLUS);
addTableButton.setTooltipText("Add new table");
```

**ToolbarPopover** — Dropdown panel that opens when the switch is activated:

```java
// Table addon: table size input (rows × cols)
addTablePopup = ToolbarPopover.horizontal(addTableButton,
    Alignment.BASELINE, rows, new Span("x"), cols, add);
addTablePopup.setAutofocus(false);
addTablePopup.setFocusOnOpenTarget(rows);
```

**ToolbarSelectPopup** — Context menu for actions:

```java
// Table addon: "Modify Table" menu (add/remove rows/cols, merge, etc.)
modifyTableSelectPopup = new ToolbarSelectPopup(modifyTableButton);
modifyTableSelectPopup.addItem("Add row below", e -> executeAction("append-row-below"));
modifyTableSelectPopup.addItem("Remove row", e -> executeAction("remove-row"));
```

**ToolbarDialog** — Resizable non-modal dialog:

```java
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
ToolbarDialog dialog = ToolbarDialog.vertical(settingsSwitch,
    new Checkbox("Show rulers"));
dialog.openAtSwitch();
```

### Keyboard Shortcuts for Standard Buttons

```java
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.BOLD, Key.KEY_B, KeyModifier.CONTROL);
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.H1, Key.DIGIT_1, KeyModifier.CONTROL, KeyModifier.SHIFT);
```

`KeyModifier.CONTROL` maps to Ctrl on Win/Linux and Cmd on Mac.

### Focus Toolbar

Let users jump to the toolbar with a shortcut — useful for keyboard-only workflows:

```java
editor.addToolbarFocusShortcut(Key.F10, KeyModifier.SHIFT);
```

### Replacing Toolbar Button Icons

```java
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.ALIGN_LEFT, VaadinIcon.ARROW_LEFT.create());
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.BOLD, null);  // Restore default
```

---

## Custom Keyboard Shortcuts

Beyond binding shortcuts to existing toolbar buttons, you can create new keyboard actions via the Quill keyboard API:

```java
editor.getElement().executeJs(
    "this._editor.keyboard.addBinding({ " +
    "  key: 'Tab', " +
    "  handler: function() { " +
    "    const index = this.quill.getSelection()?.index ?? 0; " +
    "    this.quill.insertText(index, '\\t', {'custom': true}, 'user'); " +
    "    return true; " +
    "  } " +
    "})");
```

Use `this.quill` (binding context), string key names (`"Tab"`, `"Enter"`), return `true` to prevent default.

---

## Sanitizer Integration

ERTE sanitizes all HTML on both client and server to prevent XSS. If your extension uses custom CSS classes, HTML attributes, or inline styles, you need to add them to the allowlists — otherwise they'll silently disappear on the next round-trip.

This is a dual-layer system. Both layers must agree on what's allowed.

### Client Side — Class Preservation

In `vcf-enhanced-rich-text-editor.js`, the `ERTE_PRESERVED_CLASSES` array controls which CSS classes survive the Quill → HTML → server cycle. Add your classes here:

```javascript
const ERTE_PRESERVED_CLASSES = ['ql-readonly', 'ql-tab', ..., 'your-class'];
```

### Server Side — HTML Allowlist

In `EnhancedRichTextEditor.java`:

**CSS classes (ERTE core contributors)** — add to `ALLOWED_ERTE_CLASSES` in `EnhancedRichTextEditor.java`:
```java
private static final Set<String> ALLOWED_ERTE_CLASSES = Set.of(
    "ql-readonly", "ql-tab", ..., "your-class");
```

**CSS classes (extension authors)** — use the public API from your extension code:
```java
rte.addAllowedHtmlClasses("my-extension-class", "my-other-class");
```

**HTML attributes** — there is currently no public API for extensions to register custom HTML attributes. If your extension needs custom attributes preserved through sanitization, modify `erteSanitize()` in ERTE core (`EnhancedRichTextEditor.java`).

**CSS properties** — add to `ALLOWED_CSS_PROPERTIES` if your blot uses inline styles.

The Table addon handles this differently — it registers its allowed classes dynamically via `addAllowedHtmlClasses()` on the editor, and its table-specific attributes (`table_id`, `row_id`, `cell_id`, etc.) are part of the core sanitizer allowlist because tables use `<td>`, `<tr>`, and `<table>` elements.

### What the Sanitizer Strips

All tags not in the allowed list (including `<script>`, `<iframe>`, `<object>`), event handler attributes (`onclick`, `onerror`), and CSS functions except `rgb()`, `rgba()`, `hsl()`, `hsla()`, `calc()`. The full reference — allowed tags, attributes, classes, properties, and image MIME types — is in the [User Guide — Sanitization](../BASE_USER_GUIDE.md#33-sanitization).

---

## Styling

### CSS Custom Properties

Override on the host element:

```css
vcf-enhanced-rich-text-editor {
  --vaadin-erte-readonly-background: #fff3cd;
  --vaadin-erte-placeholder-color: #004085;
  --vaadin-erte-ruler-height: 1.5rem;
}
```

See the [User Guide — Custom Properties](../BASE_USER_GUIDE.md#2101-erte-custom-properties) for the complete list with defaults.

### Blot Styles

When styling your own blots, use `--vaadin-*` custom properties that fall back to `--lumo-*` tokens. This ensures correct appearance in both light and dark mode.

**ERTE core contributors** can add styles directly in `static get styles()` of the web component class:

```javascript
static get styles() {
  return css`
    .ql-myformat {
      color: var(--vaadin-my-format-color, var(--lumo-primary-color));
    }
  `;
}
```

**Extension authors** cannot use `static get styles()` — extensions don't subclass the web component. Instead, inject CSS into the shadow root at runtime (see [Injecting CSS into Shadow DOM](#injecting-css-into-shadow-dom) below). The Table addon demonstrates this pattern.

---

## Injecting CSS into Shadow DOM

Extensions that add visual elements inside the editor need their styles inside the shadow root. The Table addon demonstrates the pattern:

```javascript
// From connector.js — inside the extension's namespace object
// (e.g., window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions.tables)
init(rte) {
  const shadowRoot = rte.shadowRoot;
  if (!shadowRoot.querySelector('#erte-table-base')) {
    const baseStyle = document.createElement('style');
    baseStyle.id = 'erte-table-base';
    baseStyle.textContent = tableCss;  // imported from .css file
    shadowRoot.append(baseStyle);
  }
}
```

Use a unique ID on the `<style>` element to prevent duplicate injection on re-attach.

---

## Building a Java Extension

An extension is a regular Java class in its own Maven module. It holds a reference to the editor and interacts with it exclusively through public API — `getElement()`, `addToolbarComponents()`, `addAllowedHtmlClasses()`, `executeJs()`, and DOM event listeners. No subclassing, no access to ERTE internals.

The Table addon is the reference (simplified — see the actual source for the full implementation):

```java
@JsModule("./src/erte-table/connector.js")
public class EnhancedRichTextEditorTables {

    private final EnhancedRichTextEditor rte;

    // Factory method — the extension pattern entry point
    public static EnhancedRichTextEditorTables enable(EnhancedRichTextEditor rte) {
        EnhancedRichTextEditorTables tables = new EnhancedRichTextEditorTables(rte, new TablesI18n());
        tables.initToolbarTable();
        return tables;
    }

    protected EnhancedRichTextEditorTables(EnhancedRichTextEditor rte, TablesI18n i18n) {
        this.rte = rte;

        // Initialize JS connector
        rte.getElement().executeJs(
            "window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions.tables.init(this)");

        // Re-init on re-attach (e.g. after a view navigation round-trip)
        rte.addAttachListener(event -> {
            if (!event.isInitialAttach()) {
                rte.getElement().executeJs(
                    "window.Vaadin.Flow.vcfEnhancedRichTextEditor"
                    + ".extensions.tables.init(this)");
            }
        });

        // Listen to custom DOM events dispatched by the JS connector
        rte.getElement().addEventListener("table-selected", event -> {
            JsonNode data = event.getEventData();
            boolean selected = data.get("event.detail.selected").asBoolean();
            boolean cellSelectionActive = data.get("event.detail.cellSelectionActive").asBoolean();
            String template = data.has("event.detail.template")
                ? data.get("event.detail.template").asText() : null;
            fireEvent(new TableSelectedEvent(this, true, selected, cellSelectionActive, template));
        }).addEventData("event.detail.selected")
          .addEventData("event.detail.cellSelectionActive")
          .addEventData("event.detail.template");
    }

    // This class is NOT a Component — fire events through the host RTE
    private void fireEvent(EnhancedRichTextEditorTablesComponentEvent event) {
        ComponentUtil.fireEvent(rte, event);
    }
}
```

### What the extension uses from ERTE

| ERTE API | What extensions use it for |
|----------|--------------------------|
| `getElement().executeJs(...)` | Call JS connector functions |
| `getElement().addEventListener(...)` | Listen to custom DOM events from JS |
| `addToolbarComponents(slot, ...)` | Place buttons and controls in the toolbar |
| `addAllowedHtmlClasses(...)` | Register CSS classes with the sanitizer |
| `addAttachListener(...)` | Re-initialize connector on re-attach |

That's it. No protected methods, no package-private access, no reflection. The extension is completely decoupled from ERTE internals, which means ERTE can evolve its internal implementation without breaking extensions.

### JS Connector Namespace

Each extension should use its own key under `extensions` to avoid collisions:

```javascript
// Tables: extensions.tables.*
window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions.tables = {
  init(rte) { ... },
  insert(rte, rows, cols) { ... }
};

// Your extension: extensions.footnotes.*
window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions.footnotes = {
  init(rte) { ... },
  insert(rte, text) { ... }
};
```

Both connectors load independently via their own `@JsModule`, both push to the shared `extendQuill`/`extendEditor` arrays, and both manage their own DOM listeners with proper cleanup on `disconnectedCallback`. Since connectors are not web component subclasses, patch the host element's `disconnectedCallback` directly:

```javascript
// In your connector's init function:
const origDisconnected = host.disconnectedCallback?.bind(host);
host.disconnectedCallback = function() {
  container.removeEventListener('mousedown', onMouseDown);
  onMouseDown = null;
  if (origDisconnected) origDisconnected.call(this);
};
```

---

**See also:** [`ARCHITECTURE.md`](./ARCHITECTURE.md) for internals, [User Guide](../BASE_USER_GUIDE.md) for features, [Quill 2](https://quilljs.com/), [Parchment 3](https://github.com/quilljs/parchment)
