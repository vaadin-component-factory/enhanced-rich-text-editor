# Extending ERTE V25

Patterns for custom blots, toolbar components, keyboard shortcuts, and styling. See [`ARCHITECTURE.md`](./ARCHITECTURE.md) for design.

## Custom Blots

If you want to add your own content types to the editor (like special tokens, badges, or inline widgets), you'll need a custom Quill blot.

### Embed Blot Patterns

An embed blot is a discrete, non-text element inside the editor (like ERTE's `TabBlot` or `PlaceholderBlot`). There are three critical patterns to get right:

1. **Lifecycle:** `static create()` runs before constructor. Initialize outer DOM in `create()`, configure inner `contentNode` in `constructor()`.

2. **Guard nodes:** Quill 2 places zero-width text inside domNode. Never set `contenteditable="false"` on outer domNode — keep guard nodes editable. Inner `contentNode` already has it.

3. **Cursor:** Override `position(index, inclusive)` if needed (default usually sufficient).

### Sanitization Integration

ERTE sanitizes all HTML on the server to prevent XSS. If your custom blot uses its own CSS class or HTML attributes, you need to add them to the allowlists — otherwise they'll be stripped on the next round-trip.

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

ERTE's toolbar is fully customizable — you can bind keyboard shortcuts, add your own components, and replace icons.

### Keyboard Shortcuts for Standard Buttons

You can bind keyboard shortcuts to any of the built-in toolbar buttons using Vaadin's `Key` constants and `KeyModifier` varargs:

```java
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.BOLD, Key.KEY_B, KeyModifier.CONTROL);           // Ctrl+B (Cmd+B on Mac)
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.H1, Key.DIGIT_1, KeyModifier.CONTROL, KeyModifier.SHIFT); // Ctrl+Shift+1
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.IMAGE, Key.F9);                                   // F9 (no modifiers)
```

**Keys:** Use Vaadin `Key` constants (`Key.KEY_A`–`Key.KEY_Z`, `Key.DIGIT_0`–`Key.DIGIT_9`, `Key.F1`–`Key.F12`, `Key.ENTER`, `Key.ESCAPE`, `Key.TAB`, `Key.ARROW_UP`). `KeyModifier.CONTROL` maps to Ctrl on Win/Linux and Cmd on Mac.

### Focus Toolbar

```java
editor.addToolbarFocusShortcut(Key.F10);                    // F10 (no modifiers)
editor.addToolbarFocusShortcut(Key.F10, KeyModifier.SHIFT); // Shift+F10
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

Beyond binding shortcuts to existing toolbar buttons, you can also create completely custom keyboard actions by talking to the Quill API directly.

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

ERTE provides two hooks for extending Quill from JavaScript. These aren't Java APIs — you register them via the global namespace and load them with `@JsModule`.

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

ERTE exposes CSS custom properties and shadow parts for theming. You can customize colors, sizes, and spacing without touching the component source.

### CSS Custom Properties

Override on the host element:

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

Here's a complete example of a custom embed blot — a "tag" element that displays like `<tagname>` inside the editor. This shows the full lifecycle: JavaScript blot, Java insertion, CSS styling, and sanitizer integration.

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

## Sanitizer Allowlists

The server-side sanitizer (`erteSanitize()`) extends Vaadin RTE 2's safelist. If you're adding custom blots or attributes, you'll need to know what's already allowed. Here are the complete reference lists.

### Allowed Tags

`p`, `br`, `strong`, `b`, `em`, `i`, `u`, `s`, `strike`, `small`, `sub`, `sup`, `cite`, `code`, `q`, `dd`, `dl`, `dt`, `h1`, `h2`, `h3`, `ol`, `ul`, `li`, `a`, `img`, `blockquote`, `pre`, `span`, `table`, `tbody`, `tr`, `td`, `th`, `colgroup`, `col`

### Allowed Attributes

| Element | Attributes |
|---------|-----------|
| All elements | `style`, `class` |
| `img` | `align`, `alt`, `height`, `src`, `title`, `width` |
| `a` | `href` |
| `span` | `contenteditable`, `aria-readonly`, `data-placeholder` |
| `blockquote`, `q` | `cite` |
| `td` | `table_id`, `row_id`, `cell_id`, `merge_id`, `colspan`, `rowspan`, `table-class` |
| `tr` | `row_id` |
| `table` | `table_id` |

### Allowed CSS Classes

- ERTE classes: `ql-readonly`, `ql-tab`, `ql-soft-break`, `ql-placeholder`, `ql-nbsp`
- Quill alignment: `ql-align-left`, `ql-align-center`, `ql-align-right`, `ql-align-justify`
- Quill indentation: `ql-indent-1` through `ql-indent-8`
- Table classes: `ql-editor__table--hideBorder`, `td-q`
- Custom classes added via `addAllowedHtmlClasses()`

### Allowed CSS Properties

`color`, `background-color`, `font-*`, `text-*`, `line-height`, `letter-spacing`, `margin`, `padding`, `border-*`, `display`, `width`, `height`, `position`, and others (complete list in `EnhancedRichTextEditor.java` source code)

### Allowed CSS Functions

`rgb()`, `rgba()`, `hsl()`, `hsla()`, `calc()`

### Allowed Image Data URL MIME Types

`image/png`, `image/jpeg`, `image/jpg`, `image/gif`, `image/webp`, `image/bmp`, `image/x-icon`

### What Gets Stripped

- All tags not in the allowed list (including `<script>`, `<iframe>`, `<object>`, `<embed>`, `<style>`)
- Event handler attributes (`onclick`, `onerror`, etc.)
- All CSS functions except the five listed above (this includes `url()`, `var()`, `expression()`, `linear-gradient()`, etc.)
- `@import` directives in style values
- SVG data URLs
- CSS comments
- `contenteditable="true"` (only `"false"` is allowed)

---

**See also:** [`ARCHITECTURE.md`](./ARCHITECTURE.md), [Quill 2](https://quilljs.com/), [Parchment 3](https://github.com/quilljs/parchment)
