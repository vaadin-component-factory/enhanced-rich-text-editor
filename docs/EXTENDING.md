# Extending ERTE V25

This document describes how to extend the Enhanced Rich Text Editor (ERTE) with custom functionality. It includes practical patterns for creating custom blots, toolbar components, keyboard shortcuts, and styling.

**Audience:** Java developers and JavaScript developers extending ERTE for application-specific features

**Related:** [`ARCHITECTURE.md`](./ARCHITECTURE.md) for internal design, [`API_REFERENCE.md`](./API_REFERENCE.md) for public API reference.

## Creating Custom Blots

A blot in Quill 2 represents a format or embed — either an inline style (like bold or readonly) or a discrete element (like a tab or image). ERTE includes examples of both types.

For comprehensive Quill 2 blot development fundamentals, see [quilljs.com](https://quilljs.com/) and [Parchment 3](https://github.com/quilljs/parchment). This section covers only ERTE-specific patterns.

### Embed Blot Gotchas (like TabBlot or PlaceholderBlot)

An embed is a discrete element (not applied to existing text). Example: `TabBlot` is an embed that represents a tabstop position. Embeds can have arbitrary content and lifecycle.

**Three ERTE-critical patterns:**

1. **Lifecycle timing:** `static create()` runs **before** the constructor. The `contentNode` is created by Embed's constructor, so do not try to access it in `create()` — initialize your outer DOM structure there, then configure `contentNode` in `constructor()`.

2. **Guard nodes (Quill 2 only):** Quill 2 places zero-width guard text (`\uFEFF`) **inside** the Embed's domNode to mark logical boundaries. Never set `contenteditable="false"` on the outer domNode — this makes guard nodes non-editable, breaking cursor placement adjacent to the embed. The inner `contentNode` already has `contenteditable="false"`, which is sufficient.

3. **Cursor placement:** Override `position(index, inclusive)` if the embed's cursor placement logic needs customization (e.g., always place after the embed). Default behavior is often sufficient.

### Server-Side Sanitization Integration

When your blot's HTML is sent to the server, it will be sanitized by `RteExtensionBase.erteSanitize()`. To ensure your blot classes survive:

1. **Add to client-side preservable list** — `vcf-enhanced-rich-text-editor.js`:
   ```javascript
   const ERTE_PRESERVED_CLASSES = ['ql-myembed', ...]; // line ~385
   ```

2. **Add to server-side whitelist** — `RteExtensionBase.java`:
   ```java
   private static final Set<String> ALLOWED_ERTE_CLASSES = Set.of(
       "ql-readonly", "ql-tab", ..., "ql-myembed"); // line ~49
   ```

3. **If using attributes beyond `class`** — Extend the jsoup Safelist in `erteSanitize()`:
   ```java
   Safelist safelist = Safelist.basic()
       .addAttributes("span", "data-myattr");
   ```

4. **If using `style` attribute** — Add allowed CSS properties to `ALLOWED_CSS_PROPERTIES`:
   ```java
   private static final Set<String> ALLOWED_CSS_PROPERTIES = Set.of(
       ..., "my-custom-property");
   ```

## Extending the Toolbar

### Standard Toolbar Button Shortcuts

Bind keyboard shortcuts to built-in toolbar buttons (toggle formats, open dialogs, undo/redo):

```java
EnhancedRichTextEditor editor = new EnhancedRichTextEditor();

// Ctrl+B (or Cmd+B on Mac) → toggle bold
editor.addStandardToolbarButtonShortcut(
    EnhancedRichTextEditor.ToolbarButton.BOLD,
    "b",              // key name
    true,             // shortKey (Ctrl/Cmd)
    false,            // shiftKey
    false);           // altKey

// Ctrl+Shift+1 → apply Heading 1
editor.addStandardToolbarButtonShortcut(
    EnhancedRichTextEditor.ToolbarButton.H1,
    "1",              // key name
    true,             // shortKey
    true,             // shiftKey
    false);           // altKey
```

**Key names:** Use Quill 2 key names: `"a"–"z"`, `"0"–"9"`, `"F1"–"F12"`, `"Enter"`, `"Escape"`, `"Tab"`, `"ArrowUp"`, etc. Not numeric keyCodes (e.g., not `32` for Space).

### Toolbar Focus Shortcut

Move focus from the editor to the toolbar:

```java
// F10 → Focus toolbar
editor.addToolbarFocusShortcut("F10", false, false, false);
```

This is useful for screen readers and keyboard-only workflows.

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

**Available slots** — `ToolbarSlot` enum provides 27 values. Common ones:
- `START`, `END` — first/last positions in toolbar
- `BEFORE_GROUP_EMPHASIS`, `BEFORE_GROUP_HEADING`, `BEFORE_GROUP_ALIGNMENT` — popular insertion points
- `GROUP_CUSTOM` — end of toolbar (legacy name "toolbar")

Custom components are assigned the part name `toolbar-custom-component`, allowing uniform styling via `::slotted()` CSS rules in `vcf-enhanced-rich-text-editor-styles.css`.

### Helper Classes for Toolbar Interactions

ERTE provides helper classes for common toolbar component patterns:

#### ToolbarSwitch

A toggle button (extends `Button`) that shows active state via an `on` HTML attribute:

```java
// Create a toggle switch with an icon
ToolbarSwitch mySwitch = new ToolbarSwitch(VaadinIcon.COG);

// Listen for active state changes
mySwitch.addActiveChangedListener(e -> {
    if (e.isActive()) {
        // Toggle on
        editor.getElement().executeJs("this.enableMyFeature()");
    } else {
        // Toggle off
        editor.getElement().executeJs("this.disableMyFeature()");
    }
});
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, mySwitch);

// Programmatic control:
mySwitch.setActive(true);   // Set state
mySwitch.toggle();          // Toggle state
mySwitch.isActive();        // Query state
```

#### ToolbarPopover

A dropdown panel (extends `Popover`) that syncs with a `ToolbarSwitch`. Requires a `ToolbarSwitch` as its target:

```java
// Create a switch and popover
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
ToolbarPopover popover = ToolbarPopover.vertical(settingsSwitch,
    new TextField("Setting 1"),
    new TextField("Setting 2"));

// Or create manually for more control:
ToolbarPopover popover = new ToolbarPopover(settingsSwitch);
popover.add(new TextField("Color"));
popover.setFocusOnOpenTarget(colorField);  // Custom focus target

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
// Popover auto-opens/closes when switch is toggled
```

#### ToolbarDialog

A non-modal dialog (extends `Dialog`) that syncs with a `ToolbarSwitch`. Resizable, draggable, closes on ESC:

```java
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);

// Factory method with vertical layout:
ToolbarDialog dialog = ToolbarDialog.vertical(settingsSwitch,
    new Checkbox("Show rulers"),
    new Checkbox("Show whitespace"),
    new Button("Close", e -> dialog.close()));

// Position at the switch instead of center:
dialog.openAtSwitch();

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
// Dialog auto-opens/closes when switch is toggled
```

#### ToolbarSelectPopup

A context menu (extends `ContextMenu`) that opens on left-click and syncs with a `ToolbarSwitch`:

```java
ToolbarSwitch formatSwitch = new ToolbarSwitch(VaadinIcon.COGS);
ToolbarSelectPopup popup = new ToolbarSelectPopup(formatSwitch);
popup.addItem("Bold", e -> editor.getElement().executeJs(
    "this._editor.format('bold', true, 'user')"));
popup.addItem("Italic", e -> editor.getElement().executeJs(
    "this._editor.format('italic', true, 'user')"));

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, formatSwitch);
// Menu auto-opens on switch click, auto-syncs active state
```

### Injecting Buttons After Quill Init

If you need to inject a button after Quill has initialized (e.g., dynamically adding a feature), you must manually bind events:

```java
Button alignJustify = new Button("Justify");
alignJustify.addClickListener(e -> {
    // Manually format when button is clicked
    editor.getElement().executeJs("this._editor.format('align', 'justify', 'user')");
});

// Also listen to selection changes to update active state
editor.getElement().executeJs(
    "this._editor.on('selection-change', () => { " +
    "  const fmt = this._editor.getFormat(); " +
    "  document.querySelector('button').setAttribute('active', fmt.align === 'justify'); " +
    "})");

editor.addToolbarComponents(ToolbarSlot.AFTER_GROUP_ALIGNMENT, alignJustify);
```

This pattern is used internally by ERTE for the align-justify button.

## Custom Keyboard Shortcuts

### Binding a Custom Action

Override the Quill keyboard handler to insert custom content or execute logic:

```java
editor.getElement().executeJs(
    "this._editor.keyboard.addBinding({ " +
    "  key: 'Tab', " +
    "  handler: function() { " +
    "    const index = this.quill.getSelection()?.index ?? 0; " +
    "    this.quill.insertText(index, '\\t', {'custom': true}, 'user'); " +
    "    return true; " +  // Handled
    "  } " +
    "})");
```

**Important:** Quill 2 keyboard bindings use:
- `this.quill` (not `this._editor`) — the Quill instance from the binding handler context
- String key names (`"Tab"`, `"Enter"`, etc.), not numeric keyCodes
- Return `true` to prevent default behavior, `false` to allow it

### Extension Hooks (JavaScript-Only)

ERTE provides two JavaScript-only hooks for customizing the Quill editor. These are **not** Java API methods — they are registered via a global JavaScript namespace before the component initializes.

**`extendQuill`** — Called **before** `super.ready()` with the Quill class. Use this to register custom blots or modules:

```javascript
// Register via a JavaScript connector file (e.g., my-extension-connector.js)
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.vcfEnhancedRichTextEditor =
    window.Vaadin.Flow.vcfEnhancedRichTextEditor || {};

// extendQuill: array of callbacks, each receives the Quill class
window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendQuill = [
  (Quill) => {
    // Register a custom blot before Quill initializes
    const Inline = Quill.import('blots/inline');
    class HighlightBlot extends Inline { /* ... */ }
    Quill.register('formats/highlight', HighlightBlot, true);
  }
];
```

**`extendEditor`** — Called **after** `super.ready()` with the Quill instance and Quill class. Use this to add custom event handlers:

```javascript
// extendEditor: array of callbacks, each receives (quillInstance, QuillClass)
window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendEditor = [
  (quill, Quill) => {
    quill.on('text-change', (delta, oldDelta, source) => {
      console.log('Editor changed:', delta);
    });
  }
];
```

**Loading from Java:** Use `@JsModule` on your view or component to load the connector file:

```java
@JsModule("./my-extension-connector.js")
@Route("my-view")
public class MyView extends VerticalLayout { /* ... */ }
```

> **Note:** `extendOptions` is deprecated in V25. Use `extendQuill` (pre-init) and `extendEditor` (post-init) instead.

## Styling & Theming

### CSS Custom Properties

All ERTE styles use CSS custom properties for easy theming:

```css
vcf-enhanced-rich-text-editor {
  --vaadin-erte-readonly-background: #fff3cd;  /* Light yellow */
  --vaadin-erte-placeholder-color: #004085;    /* Dark blue */
  --vaadin-erte-ruler-height: 1.5rem;          /* Taller ruler */
}
```

**Available properties** — See `ARCHITECTURE.md` or `vcf-enhanced-rich-text-editor-styles.css` for the complete list (20 properties across 4 categories: readonly, placeholder, whitespace, ruler).

### Styling Custom Toolbar Components

Custom components automatically inherit Vaadin's RTE 2 toolbar button styling via the `toolbar-custom-component` part:

```java
Button myButton = new Button("Click me");
myButton.getElement().setAttribute("part", "toolbar-custom-component");
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, myButton);
```

The CSS rules in `vcf-enhanced-rich-text-editor-styles.css` (lines 212–288) apply:
- Base styling with `--vaadin-rich-text-editor-toolbar-button-*` properties
- Hover effects
- Focus ring for keyboard navigation
- Active/pressed state (when component has `[on]` attribute)
- Disabled state

### Custom Toolbar Button Icons

To replace a standard toolbar button icon (e.g., align-left, color picker):

```java
editor.replaceStandardToolbarButtonIcon(
    EnhancedRichTextEditor.ToolbarButton.ALIGN_LEFT,
    new Icon(VaadinIcon.ARROW_LEFT));

editor.replaceStandardToolbarButtonIcon(
    EnhancedRichTextEditor.ToolbarButton.COLOR,
    new Icon(VaadinIcon.PALETTE));

// Pass null to restore default
editor.replaceStandardToolbarButtonIcon(
    EnhancedRichTextEditor.ToolbarButton.BOLD,
    null);
```

### Inline Blot CSS & Lumo Compatibility

Blot styles go in `static get styles()` (Lit CSS) in your JS blot class. Use `--vaadin-*` custom properties referencing `--lumo-*` tokens to ensure light/dark theme compatibility:

```javascript
static get styles() {
  return css`
    .ql-myformat {
      color: var(--vaadin-my-format-color, var(--lumo-primary-color));
      background-color: var(--vaadin-my-format-bg, var(--lumo-primary-color-10pct));
    }
  `;
}
```

## Example: Custom Tag Embed

Here's a complete example of a custom embed:

```javascript
// JavaScript: vcf-enhanced-rich-text-editor.js

class TagBlot extends Embed {
  static blotName = 'tag';
  static tagName = 'span';
  static className = 'ql-tag';

  static create(value) {
    const node = super.create();
    node.dataset.tag = value || 'tag';
    return node;
  }

  static value(node) {
    return node.dataset.tag;
  }

  constructor(scroll, node) {
    super(scroll, node);
    const tag = node.dataset.tag || 'tag';
    if (this.contentNode) {
      this.contentNode.textContent = `<${tag}>`;
    }
  }
}

Quill.register('formats/tag', TagBlot, true);

// Preserve in ERTE_PRESERVED_CLASSES (line ~385)
const ERTE_PRESERVED_CLASSES = ['ql-readonly', 'ql-tag', ...];
```

```java
// Java: Insertion method

public void insertTag(String tagName) {
    editor.getElement().executeJs(
        "this._editor.insertEmbed(this._editor.getSelection().index, 'tag', $0, 'user')",
        tagName);
}
```

```css
/* CSS: static get styles() in JS */

.ql-tag {
  color: var(--vaadin-tag-color, var(--lumo-primary-color));
  font-weight: bold;
  font-family: monospace;
}
```

Add `'ql-tag'` to `ALLOWED_ERTE_CLASSES` in `RteExtensionBase.java` (line ~49) for sanitizer support.

---

**See also:**
- [`ARCHITECTURE.md`](./ARCHITECTURE.md) — Internal design and blot registration
- [`API_REFERENCE.md`](./API_REFERENCE.md) — Complete public API
- Quill 2 documentation: https://quilljs.com/ (API reference for blots, modules, keyboard)
- Parchment 3 documentation: https://github.com/quilljs/parchment (blot format specs)
