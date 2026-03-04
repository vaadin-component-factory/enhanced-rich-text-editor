# Extending ERTE V25

Everything you need to add your own content types, toolbar components, keyboard shortcuts, and styling to ERTE. If you want to understand the internals first, start with [`ARCHITECTURE.md`](./ARCHITECTURE.md).

## Custom Blots

To add your own content types to the editor — things like special tokens, badges, or inline widgets — you'll need a custom Quill blot. ERTE's own `TabBlot`, `PlaceholderBlot`, and `ReadOnlyBlot` are all built this way.

### Embed Blot Patterns

An embed blot is a discrete, non-text element inside the editor. There are three patterns you need to get right — miss any one of them and things will break in subtle ways:

1. **Lifecycle:** `static create()` runs before constructor. Initialize outer DOM in `create()`, configure inner `contentNode` in `constructor()`.

2. **Guard nodes:** Quill 2 places zero-width text inside domNode. Never set `contenteditable="false"` on outer domNode — keep guard nodes editable. Inner `contentNode` already has it.

3. **Cursor:** Override `position(index, inclusive)` if needed (default usually sufficient).

### Sanitization Integration

This one catches people off-guard: ERTE sanitizes all HTML on the server to prevent XSS. If your custom blot uses its own CSS class or HTML attributes, you need to add them to the allowlists on **both** client and server — otherwise they'll silently disappear on the next round-trip and you'll wonder what happened.

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

The toolbar is one of ERTE's most flexible parts — you can bind keyboard shortcuts to existing buttons, drop in your own components at any position, and even replace the built-in icons.

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

A nice touch for accessibility — let users jump directly to the toolbar with a shortcut:

```java
editor.addToolbarFocusShortcut(Key.F10);                    // F10 (no modifiers)
editor.addToolbarFocusShortcut(Key.F10, KeyModifier.SHIFT); // Shift+F10
```

Especially useful for screen readers and keyboard-only workflows.

### Custom Toolbar Components

You can add any Java component (buttons, text fields, popups) to any toolbar slot:

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

**Available slots:** `START`, `END`, `BEFORE_GROUP_*`, `AFTER_GROUP_*`, `GROUP_CUSTOM`. Components get part name `toolbar-custom-component` for CSS styling.

### Toolbar Helper Classes

ERTE ships several helper classes for common toolbar UI patterns — toggle state, popover panels, and context menus.

#### ToolbarSwitch

A toggle button with an active/inactive state — the building block for everything below:

```java
ToolbarSwitch mySwitch = new ToolbarSwitch(VaadinIcon.COG);
mySwitch.addActiveChangedListener(e -> { /* handle */ });
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, mySwitch);
```

#### ToolbarPopover

A dropdown panel that opens when the switch is activated. The open/close state stays in sync automatically:

```java
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
ToolbarPopover popover = ToolbarPopover.vertical(settingsSwitch,
    new TextField("Setting 1"), new TextField("Setting 2"));
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
```

#### ToolbarDialog

Like `ToolbarPopover`, but opens a resizable, non-modal dialog instead — good for larger tool panels:

```java
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
ToolbarDialog dialog = ToolbarDialog.vertical(settingsSwitch,
    new Checkbox("Show rulers"));
dialog.openAtSwitch();
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
```

#### ToolbarSelectPopup

A context menu that appears when the switch is clicked — perfect for format selection or quick actions:

```java
ToolbarSwitch formatSwitch = new ToolbarSwitch(VaadinIcon.COGS);
ToolbarSelectPopup popup = new ToolbarSelectPopup(formatSwitch);
popup.addItem("Bold", e -> editor.getElement().executeJs(
    "this._editor.format('bold', true, 'user')"));
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, formatSwitch);
```

### Dynamic Button Injection

If you're injecting buttons after Quill has already initialized, you'll need to manually bind the editor actions — the toolbar's automatic wiring only happens during init:

```java
Button alignJustify = new Button("Justify");
alignJustify.addClickListener(e ->
    editor.getElement().executeJs("this._editor.format('align', 'justify', 'user')"));
editor.addToolbarComponents(ToolbarSlot.AFTER_GROUP_ALIGNMENT, alignJustify);
```

Used internally for align-justify button.

## Custom Keyboard Shortcuts

Beyond binding shortcuts to existing toolbar buttons, you can also create entirely new keyboard actions by talking to the Quill keyboard API directly. This gives you full control over what happens when the user presses a key combination.

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

For deeper customization, ERTE provides two hooks that let you extend Quill directly from JavaScript. These aren't Java APIs — you register callback functions via the global namespace and load them with `@JsModule`.

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

ERTE looks good out of the box with Lumo, but you can customize just about every visual detail. CSS custom properties let you change colors, sizes, and spacing without touching component source code.

### CSS Custom Properties

Override on the host element:

```css
vcf-enhanced-rich-text-editor {
  --vaadin-erte-readonly-background: #fff3cd;
  --vaadin-erte-placeholder-color: #004085;
  --vaadin-erte-ruler-height: 1.5rem;
}
```

See the [User Guide — Custom Properties](../BASE_USER_GUIDE.md#2101-erte-custom-properties) for the complete reference with defaults.

### Styling Custom Toolbar Components

Custom toolbar components automatically inherit the native RTE 2 toolbar styling via the `toolbar-custom-component` part — hover effects, focus rings, active state, and disabled state all work out of the box.

### Replacing Toolbar Button Icons

```java
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.ALIGN_LEFT, VaadinIcon.ARROW_LEFT.create());
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.BOLD, null);  // Restore default
```

### Inline Blot CSS

When styling your own blots, use `--vaadin-*` custom properties that fall back to `--lumo-*` tokens. This ensures correct appearance in both light and dark mode:

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

Here's a complete example of a custom embed blot — a "tag" element that displays like `<tagname>` inside the editor. It covers the full lifecycle: JavaScript blot definition, Java insertion API, CSS styling, and sanitizer integration.

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

This is the full reference of what the server-side sanitizer allows through. You'll need this when adding custom blots or attributes — check what's already allowed so you know what to add. For a user-facing summary, see the [User Guide — Sanitization](../BASE_USER_GUIDE.md#33-sanitization).

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
