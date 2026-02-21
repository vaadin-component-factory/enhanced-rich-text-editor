# Enhanced Rich Text Editor -- Configuration Guide

This guide covers all configuration patterns for the Enhanced Rich Text Editor (ERTE) v6.x: toolbar customization, keyboard shortcuts, internationalization, styling/theming, sanitization, and value format selection.

**Audience:** Developers customizing ERTE for their application.

**Related docs:** [User Guide](USER_GUIDE.md) for feature documentation, [API Reference](API_REFERENCE.md) for complete API surface, [Upgrade Guide](UPGRADE_GUIDE.md) for migration from v5.x.

---

## Table of Contents

- [1. Toolbar Customization](#1-toolbar-customization)
- [2. Keyboard Shortcuts](#2-keyboard-shortcuts)
- [3. Internationalization (I18n)](#3-internationalization-i18n)
- [4. Styling and Theming](#4-styling-and-theming)
- [5. Sanitization](#5-sanitization)
- [6. Value Format](#6-value-format)

---

## 1. Toolbar Customization

### 1.1 Toolbar Slot System

ERTE provides 27 named slots for placing custom Vaadin components in the toolbar. Slots are organized around the toolbar's 11 button groups.

**Toolbar layout:**

```
[START]
  [BEFORE_GROUP_HISTORY] [Undo | Redo] [AFTER_GROUP_HISTORY]
  [BEFORE_GROUP_EMPHASIS] [Bold | Italic | Underline | Strike] [AFTER_GROUP_EMPHASIS]
  [BEFORE_GROUP_STYLE] [Color | Background] [AFTER_GROUP_STYLE]
  [BEFORE_GROUP_HEADING] [H1 | H2 | H3] [AFTER_GROUP_HEADING]
  [BEFORE_GROUP_GLYPH_TRANSFORMATION] [Subscript | Superscript] [AFTER_GROUP_GLYPH_TRANSFORMATION]
  [BEFORE_GROUP_LIST] [Ordered | Bullet] [AFTER_GROUP_LIST]
  [BEFORE_GROUP_INDENT] [Outdent | Indent] [AFTER_GROUP_INDENT]
  [BEFORE_GROUP_ALIGNMENT] [Left | Center | Right] [AFTER_GROUP_ALIGNMENT]
  [BEFORE_GROUP_RICH_TEXT] [Image | Link] [AFTER_GROUP_RICH_TEXT]
  [BEFORE_GROUP_BLOCK] [Blockquote | Code Block] [AFTER_GROUP_BLOCK]
  [BEFORE_GROUP_FORMAT] [Clean] [AFTER_GROUP_FORMAT]
  [BEFORE_GROUP_CUSTOM] [GROUP_CUSTOM] [AFTER_GROUP_CUSTOM]
[END]
```

> **Note:** The Style group (Color, Background) is new in V25 compared to V24.

**Add components to slots:**

```java
import com.vaadin.componentfactory.toolbar.ToolbarSlot;

// Add a button before the history group
Button myButton = new Button("Custom");
editor.addToolbarComponents(ToolbarSlot.BEFORE_GROUP_HISTORY, myButton);

// Add multiple components to the end
Button save = new Button("Save");
Button export = new Button("Export");
editor.addToolbarComponents(ToolbarSlot.END, save, export);

// Add at a specific index within the slot
editor.addToolbarComponentsAtIndex(ToolbarSlot.END, 0, new Button("First"));

// Convenience: add to the custom group
editor.addCustomToolbarComponents(new Button("Custom 1"), new Button("Custom 2"));
```

**Remove components:**

```java
// Remove by component reference
editor.removeToolbarComponent(ToolbarSlot.END, save);

// Remove by ID
editor.removeToolbarComponent(ToolbarSlot.END, "save-btn");
```

**Retrieve components:**

```java
Button btn = editor.getToolbarComponent(ToolbarSlot.END, "save-btn");
```

### 1.2 Button Visibility

Control which of the 30 toolbar buttons are visible:

```java
import com.vaadin.componentfactory.EnhancedRichTextEditor.ToolbarButton;

// Hide specific buttons
editor.setToolbarButtonsVisibility(Map.of(
    ToolbarButton.IMAGE, false,
    ToolbarButton.LINK, false,
    ToolbarButton.CODE_BLOCK, false
));

// Hide an entire group (all buttons hidden = group container hidden)
editor.setToolbarButtonsVisibility(Map.of(
    ToolbarButton.BLOCKQUOTE, false,
    ToolbarButton.CODE_BLOCK, false    // Block group now fully hidden
));

// Hide ERTE-specific buttons
editor.setToolbarButtonsVisibility(Map.of(
    ToolbarButton.PLACEHOLDER, false,
    ToolbarButton.READONLY, false,
    ToolbarButton.WHITESPACE, false,
    ToolbarButton.ALIGN_JUSTIFY, false
));

// Reset to all visible
editor.setToolbarButtonsVisibility(null);
```

**Available buttons (30 total):**

Standard (25): `UNDO`, `REDO`, `BOLD`, `ITALIC`, `UNDERLINE`, `STRIKE`, `COLOR`, `BACKGROUND`, `H1`, `H2`, `H3`, `SUBSCRIPT`, `SUPERSCRIPT`, `LIST_ORDERED`, `LIST_BULLET`, `OUTDENT`, `INDENT`, `ALIGN_LEFT`, `ALIGN_CENTER`, `ALIGN_RIGHT`, `IMAGE`, `LINK`, `BLOCKQUOTE`, `CODE_BLOCK`, `CLEAN`

ERTE-specific (5): `READONLY`, `PLACEHOLDER`, `PLACEHOLDER_APPEARANCE`, `WHITESPACE`, `ALIGN_JUSTIFY`

### 1.3 ToolbarSwitch Component

`ToolbarSwitch` is a toggle button designed for toolbar use. It reflects its active state via an `on` HTML attribute.

```java
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;

// Create a toggle for dark mode
ToolbarSwitch darkMode = new ToolbarSwitch("Dark");
darkMode.setId("dark-mode-toggle");
darkMode.addActiveChangedListener(e -> {
    if (e.isActive()) {
        applyDarkTheme();
    } else {
        applyLightTheme();
    }
});
editor.addToolbarComponents(ToolbarSlot.END, darkMode);

// Programmatic control
darkMode.setActive(true);   // Activate
darkMode.toggle();           // Toggle state
boolean isOn = darkMode.isActive();
```

### 1.4 Replacing Button Icons

Replace the default SVG icons of any standard toolbar button with Vaadin icons:

```java
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

// Replace icons
editor.replaceStandardToolbarButtonIcon(ToolbarButton.BOLD, new Icon(VaadinIcon.STAR));
editor.replaceStandardToolbarButtonIcon(ToolbarButton.UNDO, new Icon(VaadinIcon.ARROW_LEFT));

// Restore default icon
editor.replaceStandardToolbarButtonIcon(ToolbarButton.BOLD, null);

// Low-level: replace by slot name
editor.replaceStandardButtonIcon(new Icon(VaadinIcon.FLAG), "italic");
```

---

## 2. Keyboard Shortcuts

### 2.1 Standard Button Shortcuts

Bind keyboard shortcuts to any toolbar button:

```java
// Ctrl+Shift+B toggles Bold
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.BOLD,
    "b",             // key name
    true,            // shortKey (Ctrl on Win/Linux, Cmd on Mac)
    true,            // shiftKey
    false            // altKey
);

// Shift+F9 triggers Align Center
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.ALIGN_CENTER,
    "F9", false, true, false
);

// Alt+R triggers Readonly toggle
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.READONLY,
    "r", false, false, true
);
```

**Key parameter:** Uses Quill 2 key names. Common values:
- Letters: `"a"` through `"z"` (case-insensitive)
- Function keys: `"F1"` through `"F12"`
- Special: `"Enter"`, `"Tab"`, `"Escape"`, `"Backspace"`, `"Delete"`

**shortKey parameter:** Maps to the platform's primary modifier:
- Windows/Linux: Ctrl
- macOS: Cmd (Meta)

### 2.2 Toolbar Focus Shortcut

Move focus from the editor content area to the toolbar:

```java
// Shift+F10 focuses the first visible toolbar button
editor.addToolbarFocusShortcut("F10", false, true, false);
```

Once focused, toolbar buttons can be navigated with Tab/Shift+Tab and activated with Enter/Space.

### 2.3 Built-in Shortcuts

These shortcuts are always active and cannot be removed:

| Shortcut | Action | Condition |
|----------|--------|-----------|
| Tab | Insert tab embed | Tabstops configured |
| Shift+Enter | Insert soft-break | Always |
| Shift+Space | Insert non-breaking space | Always |
| Ctrl+P / Cmd+P | Open placeholder dialog | Placeholders configured |

> **Note:** Ctrl+P/Cmd+P may be intercepted by the browser's print dialog in some configurations.

---

## 3. Internationalization (I18n)

### 3.1 Setting Labels

```java
EnhancedRichTextEditor.EnhancedRichTextEditorI18n i18n =
    new EnhancedRichTextEditor.EnhancedRichTextEditorI18n();

// Standard labels (inherited from RTE 2)
i18n.setBold("Fett")
    .setItalic("Kursiv")
    .setUnderline("Unterstreichen")
    .setStrike("Durchgestrichen")
    .setUndo("Ruckgangig")
    .setRedo("Wiederholen")
    .setH1("Uberschrift 1")
    .setH2("Uberschrift 2")
    .setH3("Uberschrift 3")
    .setColor("Textfarbe")
    .setBackground("Hintergrund")
    .setSubscript("Tiefgestellt")
    .setSuperscript("Hochgestellt")
    .setListOrdered("Nummerierte Liste")
    .setListBullet("Aufzahlung")
    .setOutdent("Ausrucken")
    .setIndent("Einrucken")
    .setAlignLeft("Links")
    .setAlignCenter("Mitte")
    .setAlignRight("Rechts")
    .setImage("Bild")
    .setLink("Link")
    .setBlockquote("Zitat")
    .setCodeBlock("Quellcode")
    .setClean("Formatierung entfernen");

// ERTE-specific labels
i18n.setReadonly("Schreibschutz")
    .setWhitespace("Leerzeichen anzeigen")
    .setPlaceholder("Platzhalter")
    .setPlaceholderAppearance("Platzhalter-Darstellung")
    .setPlaceholderDialogTitle("Platzhalter")
    .setPlaceholderComboBoxLabel("Platzhalter wahlen")
    .setPlaceholderAppearanceLabel1("Normal")
    .setPlaceholderAppearanceLabel2("Wert")
    .setAlignJustify("Blocksatz");

editor.setI18n(i18n);
```

### 3.2 Placeholder Dialog Labels

The placeholder dialog uses these i18n keys:

| Label | Used For |
|-------|----------|
| `placeholderDialogTitle` | Dialog header text |
| `placeholderComboBoxLabel` | Combo-box label ("Select Placeholder") |
| `placeholderAppearanceLabel1` | First appearance option ("Format") |
| `placeholderAppearanceLabel2` | Second appearance option ("Alt Format") |

### 3.3 Label Behavior

- Labels are applied as `aria-label` attributes on toolbar buttons (not `title`)
- Setting i18n updates all button labels immediately (Lit re-render)
- All setters return `EnhancedRichTextEditorI18n` for fluent chaining
- Inherited labels and ERTE labels can be mixed in the same fluent chain

---

## 4. Styling and Theming

### 4.1 Vaadin Lumo Theme

ERTE uses the Vaadin Lumo theme. Key custom properties:

| Property | Used For |
|----------|----------|
| `--lumo-primary-color` | Toolbar button active/selected state |
| `--lumo-primary-text-color` | Toolbar button hover state |
| `--lumo-contrast-10pct` | Toolbar button borders |
| `--lumo-font-size-xs` | Small text elements |
| `--lumo-space-s` | Spacing |

### 4.2 Shadow Parts

ERTE toolbar buttons and groups expose CSS shadow parts for external styling:

**Individual buttons:**
```css
vcf-enhanced-rich-text-editor::part(toolbar-button-bold) {
    color: red;
}

vcf-enhanced-rich-text-editor::part(toolbar-button-readonly) {
    background: lightyellow;
}
```

**Groups:**
```css
vcf-enhanced-rich-text-editor::part(toolbar-group-history) {
    border-right: 2px solid var(--lumo-primary-color);
}
```

**Toolbar container:**
```css
vcf-enhanced-rich-text-editor::part(toolbar) {
    background: var(--lumo-tint-5pct);
}
```

### 4.3 ERTE-Specific CSS Classes

These classes are used in the editor content area (inside `.ql-editor`):

| Class | Element | Purpose |
|-------|---------|---------|
| `.ql-tab` | `<span>` | Tab embed (inline-block, calculated width) |
| `.ql-placeholder` | `<span>` | Placeholder embed |
| `.ql-readonly` | `<span>` | Readonly section (contenteditable="false") |
| `.ql-soft-break` | `<span>` | Soft-break embed (contains `<br>`) |
| `.ql-nbsp` | `<span>` | Non-breaking space |

**Ruler elements:**

| Selector | Purpose |
|----------|---------|
| `[part~="horizontalRuler"]` | Horizontal ruler container |
| `vaadin-icon` inside ruler | Tabstop markers (LEFT/RIGHT/MIDDLE) |

### 4.4 Custom Content Styling

Override default styles for ERTE content elements:

```css
/* Custom readonly section styling */
.ql-editor .ql-readonly {
    background: lightyellow;
    border: 1px dashed orange;
    padding: 0 2px;
}

/* Custom placeholder styling */
.ql-editor .ql-placeholder {
    border-radius: 4px;
    padding: 1px 4px;
}

/* Custom tab styling */
.ql-editor .ql-tab {
    border-bottom: 1px dotted var(--lumo-contrast-30pct);
}
```

> **Note:** Content styles inside `.ql-editor` are applied via the component's shadow DOM `static get styles()`. External CSS targeting `.ql-editor` descendants works because the editor content is in the shadow DOM's slotted area.

---

## 5. Sanitization

### 5.1 Default Sanitizer

ERTE's server-side sanitizer (`erteSanitize()`) extends Vaadin RTE 2's safelist:

**Allowed HTML tags:** `p`, `br`, `strong`, `em`, `u`, `s`, `sub`, `sup`, `h1`, `h2`, `h3`, `ol`, `ul`, `li`, `a`, `img`, `blockquote`, `pre`, `span`

**Allowed attributes:**
- All elements: `style`, `class`
- `img`: `align`, `alt`, `height`, `src`, `title`, `width`
- `a`: `href`, `target`
- `span`: `contenteditable`, `aria-readonly`, `data-placeholder`

**Allowed CSS classes:**
- ERTE classes: `ql-readonly`, `ql-tab`, `ql-soft-break`, `ql-placeholder`, `ql-nbsp`
- Quill alignment: `ql-align-left`, `ql-align-center`, `ql-align-right`, `ql-align-justify`
- Quill indent: `ql-indent-1` through `ql-indent-8`

**Allowed CSS properties (in `style` attributes):**
- Text: `color`, `background-color`, `background`, `font-size`, `font-family`, `font-weight`, `font-style`
- Layout: `text-align`, `text-indent`, `text-decoration`, `text-decoration-line`, `text-decoration-style`, `text-decoration-color`, `direction`
- Spacing: `line-height`, `letter-spacing`, `word-spacing`
- Box model: `margin`, `padding` (all sub-properties)
- Border: `border` (all sub-properties), `border-collapse`, `border-spacing`
- Display: `display`, `white-space`, `vertical-align`, `visibility`, `opacity`
- Size: `width`, `height`, `min-width`, `max-width`, `min-height`, `max-height`
- Position: `position`, `top`, `right`, `bottom`, `left`, `float`
- Other: `list-style-type`, `overflow`, `overflow-x`, `overflow-y`, `cursor`

**Allowed CSS functions:** `rgb()`, `rgba()`, `hsl()`, `hsla()`, `calc()`

**Allowed data URL MIME types:** `image/png`, `image/jpeg`, `image/jpg`, `image/gif`, `image/webp`, `image/bmp`, `image/x-icon`

### 5.2 What Gets Stripped

- `<script>`, `<iframe>`, `<object>`, `<embed>`, `<form>` tags
- JavaScript event handlers (`onclick`, `onerror`, `onload`, etc.)
- Unknown CSS classes (not in the ERTE or Quill whitelist)
- Dangerous CSS functions (e.g., `url()`, `expression()`, `var()`)
- `@import` directives in CSS
- SVG data URLs (can contain embedded scripts)
- `contenteditable="true"` (only `"false"` is allowed)
- CSS comments (`/* ... */`)

### 5.3 Custom Sanitization

Advanced users can override the sanitizer by extending `RteExtensionBase`:

```java
// Note: This is an advanced customization. The default sanitizer covers
// all standard use cases. Only override if you need to allow additional
// HTML elements or attributes.

// See RteExtensionBase.erteSanitize() for the implementation.
// The sanitizer uses jsoup's Safelist API.
```

> **Warning:** Weakening the sanitizer can introduce XSS vulnerabilities. See `SECURITY.md` for known attack vectors.

---

## 6. Value Format

### 6.1 HTML (Default)

HTML is the primary value format in ERTE v6.x (matching Vaadin RTE 2).

```java
// Set HTML
editor.setValue("<p>Hello <strong>world</strong></p>");

// Get HTML
String html = editor.getValue();

// Listen for changes (fires on blur)
editor.addValueChangeListener(e -> save(e.getValue()));

// Eager mode (fires on every change)
editor.setValueChangeMode(ValueChangeMode.EAGER);
```

**When to use HTML:**
- Standard form field binding
- Database storage (most compact format)
- Server-side processing with jsoup or similar
- When you do not need precise format control

### 6.2 Delta (Optional)

Delta JSON provides precise control over content structure and formatting.

```java
// Set Delta JSON
editor.asDelta().setValue(
    "[{\"insert\":\"Hello \"},{\"insert\":\"world\",\"attributes\":{\"bold\":true}},{\"insert\":\"\\n\"}]"
);

// Get Delta JSON
String delta = editor.asDelta().getValue();

// Listen for changes
editor.asDelta().addValueChangeListener(e -> {
    String newDelta = e.getValue();
});
```

**When to use Delta:**
- Setting readonly content (`{"attributes": {"readonly": true}}`)
- Setting placeholder content (`{"insert": {"placeholder": {"text": "Name"}}}`)
- Setting tab content (`{"insert": {"tab": true}}`)
- Batch updates (Delta parsing is faster than HTML)
- When you need precise Quill format control

### 6.3 Delta Structure Reference

```json
// Plain text
[{"insert": "Hello World\n"}]

// Bold text
[{"insert": "bold text", "attributes": {"bold": true}}, {"insert": "\n"}]

// Tab embed
[{"insert": "before"}, {"insert": {"tab": true}}, {"insert": "after\n"}]

// Readonly section
[{"insert": "protected", "attributes": {"readonly": true}}, {"insert": "\n"}]

// Placeholder embed
[{"insert": {"placeholder": {"text": "Company Name"}}}, {"insert": "\n"}]

// Soft-break
[{"insert": "line1"}, {"insert": {"softBreak": true}}, {"insert": "line2\n"}]

// Mixed content
[
  {"insert": "Dear "},
  {"insert": {"placeholder": {"text": "N-1=Name", "format": {"italic": true}}}},
  {"insert": ",\n"},
  {"insert": "Welcome to "},
  {"insert": "our company", "attributes": {"readonly": true}},
  {"insert": ".\n"}
]
```

### 6.4 Format Selection Guide

| Use Case | Recommended Format | Reason |
|----------|-------------------|--------|
| Form field binding | HTML | Standard Vaadin pattern |
| Database storage | HTML | Compact, query-friendly |
| Readonly sections | Delta | Readonly is a Delta attribute |
| Placeholder content | Delta | Placeholder is a Delta embed |
| Template engine input | HTML | Easy to process server-side |
| Content migration | Delta | Preserves all formatting precisely |
| Unit test assertions | Delta | Deterministic structure |
