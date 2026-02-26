# Extending ERTE V25

Practical patterns for custom blots, toolbar components, keyboard shortcuts, and styling. See [`ARCHITECTURE.md`](./ARCHITECTURE.md) for design, [`API_REFERENCE.md`](../user/API_REFERENCE.md) for public API.

For Quill 2 fundamentals, see [quilljs.com](https://quilljs.com/) and [Parchment 3](https://github.com/quilljs/parchment).

## Custom Blots

### Embed Blot Patterns

An embed is a discrete element (not applied to existing text), like `TabBlot` or `PlaceholderBlot`.

**Three critical patterns:**

1. **Lifecycle:** `static create()` runs before constructor. `contentNode` created by Embed constructor — initialize outer DOM in `create()`, configure `contentNode` in `constructor()`.

2. **Guard nodes (Quill 2):** Quill 2 places zero-width guard text (`\uFEFF`) inside domNode. Never set `contenteditable="false"` on outer domNode — guard nodes must stay editable. Inner `contentNode` already has it.

3. **Cursor placement:** Override `position(index, inclusive)` if needed. Default usually sufficient.

### Sanitization Integration

Custom blot HTML must pass server sanitization. To preserve your blot:

1. **Client preservable list** — `vcf-enhanced-rich-text-editor.js` (~line 385):
   ```javascript
   const ERTE_PRESERVED_CLASSES = ['ql-readonly', 'ql-tab', ..., 'ql-myembed'];
   ```

2. **Server whitelist** — `EnhancedRichTextEditor.java` (~line 161):
   ```java
   private static final Set<String> ALLOWED_ERTE_CLASSES = Set.of(
       "ql-readonly", "ql-tab", ..., "ql-myembed");
   ```

3. **Custom attributes** — Extend jsoup Safelist (~line 239):
   ```java
   Safelist safelist = Safelist.basic().addAttributes("span", "data-myattr");
   ```

4. **Custom styles** — Add to `ALLOWED_CSS_PROPERTIES` (~line 164)

## Extending the Toolbar

### Keyboard Shortcuts for Standard Buttons

Bind shortcuts to built-in buttons:

```java
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.BOLD, "b", true, false, false);  // Ctrl+B
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.H1, "1", true, true, false);     // Ctrl+Shift+1
```

**Key names:** Use Quill 2 names (`"a"–"z"`, `"0"–"9"`, `"F1"–"F12"`, `"Enter"`, `"Escape"`, `"Tab"`, `"ArrowUp"`), not numeric keyCodes.

### Focus Toolbar

```java
editor.addToolbarFocusShortcut("F10", false, false, false);
```

Useful for screen readers and keyboard-only workflows.

### Custom Toolbar Components

Add Java components (buttons, inputs, popups) to any of the 27 toolbar slots:

```java
// Simple button in the custom group
Button customButton = new Button("My Button");
customButton.addClickListener(e -> {
    // Handle click
});
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, customButton);

// Multiple components in a specific slot
Button before = new Button("A");
Button after = new Button("B");
editor.addToolbarComponents(
    ToolbarSlot.BEFORE_GROUP_EMPHASIS,
    before, after);
```

**Available slots:** `START`, `END`, `BEFORE_GROUP_*`, `AFTER_GROUP_*`, `GROUP_CUSTOM` (27 total). Components get part name `toolbar-custom-component` for CSS styling.

### Toolbar Helper Classes

#### ToolbarSwitch

Toggle button with active state:

```java
ToolbarSwitch mySwitch = new ToolbarSwitch(VaadinIcon.COG);
mySwitch.addActiveChangedListener(e -> { /* handle */ });
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, mySwitch);
```

#### ToolbarPopover

Dropdown panel synced with switch:

```java
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
ToolbarPopover popover = ToolbarPopover.vertical(settingsSwitch,
    new TextField("Setting 1"), new TextField("Setting 2"));
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
```

#### ToolbarDialog

Non-modal resizable dialog synced with switch:

```java
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
ToolbarDialog dialog = ToolbarDialog.vertical(settingsSwitch,
    new Checkbox("Show rulers"));
dialog.openAtSwitch();
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
```

#### ToolbarSelectPopup

Context menu synced with switch:

```java
ToolbarSwitch formatSwitch = new ToolbarSwitch(VaadinIcon.COGS);
ToolbarSelectPopup popup = new ToolbarSelectPopup(formatSwitch);
popup.addItem("Bold", e -> editor.getElement().executeJs(
    "this._editor.format('bold', true, 'user')"));
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, formatSwitch);
```

### Dynamic Button Injection

If injecting after Quill init, manually bind events:

```java
Button alignJustify = new Button("Justify");
alignJustify.addClickListener(e ->
    editor.getElement().executeJs("this._editor.format('align', 'justify', 'user')"));
editor.addToolbarComponents(ToolbarSlot.AFTER_GROUP_ALIGNMENT, alignJustify);
```

Used internally for align-justify button.

## Custom Keyboard Shortcuts

### Binding Custom Actions

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

**Important:** Use `this.quill` (binding context), string key names (`"Tab"`, `"Enter"`), return `true` to prevent default.

### Extension Hooks (JavaScript-Only)

Two hooks for Quill customization (not Java API — register via global JavaScript namespace):

**`extendQuill`** — Called before `super.ready()` with Quill class. Register custom blots:

```javascript
window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendQuill = [
  (Quill) => {
    const Inline = Quill.import('blots/inline');
    class HighlightBlot extends Inline { /* ... */ }
    Quill.register('formats/highlight', HighlightBlot, true);
  }
];
```

**`extendEditor`** — Called after `super.ready()` with Quill instance. Add event handlers:

```javascript
window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendEditor = [
  (quill, Quill) => {
    quill.on('text-change', (delta, oldDelta, source) => {
      console.log('Editor changed:', delta);
    });
  }
];
```

Load via `@JsModule` on your view:

```java
@JsModule("./my-extension-connector.js")
@Route("my-view")
public class MyView extends VerticalLayout { }
```

> **Note:** `extendOptions` deprecated in V25. Use `extendQuill` and `extendEditor` instead.

## Styling & Theming

### CSS Custom Properties

Override at host element:

```css
vcf-enhanced-rich-text-editor {
  --vaadin-erte-readonly-background: #fff3cd;
  --vaadin-erte-placeholder-color: #004085;
  --vaadin-erte-ruler-height: 1.5rem;
}
```

See `ARCHITECTURE.md` for complete list (22 properties across 4 categories).

### Styling Custom Toolbar Components

Components inherit RTE 2 toolbar styling via `toolbar-custom-component` part. Includes hover, focus ring, active state, disabled state.

### Replacing Toolbar Button Icons

```java
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.ALIGN_LEFT, VaadinIcon.ARROW_LEFT.create());
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.BOLD, null);  // Restore default
```

### Inline Blot CSS

Use `--vaadin-*` properties referencing `--lumo-*` tokens for light/dark compatibility:

```javascript
static get styles() {
  return css`
    .ql-myformat {
      color: var(--vaadin-my-format-color, var(--lumo-primary-color));
    }
  `;
}
```

## Example: Custom Tag Embed

Complete custom embed example:

**JavaScript (vcf-enhanced-rich-text-editor.js):**
```javascript
class TagBlot extends Embed {
  static blotName = 'tag';
  static tagName = 'span';
  static className = 'ql-tag';

  static create(value) {
    const node = super.create();
    node.dataset.tag = value || 'tag';
    return node;
  }

  static value(node) { return node.dataset.tag; }

  constructor(scroll, node) {
    super(scroll, node);
    if (this.contentNode) {
      this.contentNode.textContent = `<${node.dataset.tag || 'tag'}>`;
    }
  }
}

Quill.register('formats/tag', TagBlot, true);
const ERTE_PRESERVED_CLASSES = ['ql-readonly', 'ql-tag', ...];
```

**Java:**
```java
public void insertTag(String tagName) {
    editor.getElement().executeJs(
        "this._editor.insertEmbed(this._editor.getSelection().index, 'tag', $0, 'user')",
        tagName);
}
```

**CSS:**
```javascript
static get styles() {
  return css`.ql-tag { color: var(--vaadin-tag-color, var(--lumo-primary-color)); }`;
}
```

Add `'ql-tag'` to `ALLOWED_ERTE_CLASSES` in `EnhancedRichTextEditor.java` for sanitizer support.

---

**See also:** [`ARCHITECTURE.md`](./ARCHITECTURE.md), [`API_REFERENCE.md`](../user/API_REFERENCE.md), [Quill 2](https://quilljs.com/), [Parchment 3](https://github.com/quilljs/parchment)
