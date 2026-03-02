# Enhanced Rich Text Editor -- User Guide

This guide covers all ERTE v6.x features for Vaadin 25, including code examples, best practices, and troubleshooting. For migration from v5.x, see [Upgrade Guide](BASE_UPGRADE_GUIDE.md).

---

## Table of Contents

- [1. Getting Started](#1-getting-started)
- [2. Core Features](#2-core-features)
  - [2.1 Toolbar Customization](#21-toolbar-customization)
  - [2.2 Placeholders](#22-placeholders)
  - [2.3 Tabstops and Rulers](#23-tabstops-and-rulers)
  - [2.4 Readonly Sections](#24-readonly-sections)
  - [2.5 Whitespace Indicators](#25-whitespace-indicators)
  - [2.6 Non-Breaking Space](#26-non-breaking-space)
  - [2.7 Soft-Break and Tab Copying](#27-soft-break-and-tab-copying)
  - [2.8 Programmatic Text Insertion](#28-programmatic-text-insertion)
  - [2.9 Align Justify](#29-align-justify)
  - [2.10 Styling and Theming](#210-styling-and-theming)
  - [2.11 Built-in Keyboard Shortcuts](#211-built-in-keyboard-shortcuts)
- [3. Advanced Features](#3-advanced-features)
  - [3.1 Value Formats (HTML vs Delta)](#31-value-formats-html-vs-delta)
  - [3.2 Internationalization (I18n)](#32-internationalization-i18n)
  - [3.3 Sanitization](#33-sanitization)
- [4. Getting Help](#4-getting-help)

---

## 1. Getting Started

### 1.1 Installation

ERTE v6.x requires Vaadin 25.0.x. Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor</artifactId>
    <version>6.0.0</version>
</dependency>
```

> **Note:** The base Rich Text Editor is part of the `vaadin` artifact (not `vaadin-core`). Make sure your project uses `vaadin` as dependency.

### 1.2 Basic Usage

```java
EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
editor.setValue("<p>Hello, world!</p>");

// Listen for changes (fires on blur)
editor.addValueChangeListener(event -> {
    Notification.show("Saved: " + event.getValue());
});

// Hide buttons
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.IMAGE, false,
    EnhancedRichTextEditor.ToolbarButton.CODE_BLOCK, false
));

// Add custom button to toolbar
Button saveBtn = new Button("Save");
editor.addToolbarComponents(ToolbarSlot.END, saveBtn);

// Configure tabstops
editor.setTabStops(List.of(
    new TabStop(TabStop.Direction.LEFT, 150),
    new TabStop(TabStop.Direction.RIGHT, 350)
));

// Read-only viewer
EnhancedRichTextEditor viewer = new EnhancedRichTextEditor();
viewer.setReadOnly(true);
viewer.setValue(savedHtml);

// Programmatic text insertion
editor.addText("PREFIX", 0);  // at beginning
editor.addText("INSERTED");   // at cursor
editor.getTextLength(len -> Notification.show("Length: " + len));
```

---

## 2. Core Features

### 2.1 Toolbar Customization

The toolbar is ERTE's most flexible feature. It supports adding custom components to 27 named slots, controlling button visibility, binding keyboard shortcuts, and replacing button icons.

#### Adding Components to Toolbar Slots

ERTE provides 27 `ToolbarSlot` positions: `START`/`END`, `BEFORE_GROUP_*/AFTER_GROUP_*` (22 positions per group), and `GROUP_CUSTOM`.

```java
Button startBtn = new Button("S");
editor.addToolbarComponents(ToolbarSlot.START, startBtn);

Button endBtn = new Button("E");
editor.addToolbarComponents(ToolbarSlot.END, endBtn);

Button beforeHistoryBtn = new Button("BH");
editor.addToolbarComponents(ToolbarSlot.BEFORE_GROUP_HISTORY, beforeHistoryBtn);

// Or add to the custom group
editor.addCustomToolbarComponents(new Button("Custom"));
```

**ToolbarSwitch** -- a toggle button for toolbar components:

```java
ToolbarSwitch toolbarSwitch = new ToolbarSwitch("SW");
toolbarSwitch.addActiveChangedListener(e ->
    System.out.println("Switch active: " + e.isActive()));
editor.addCustomToolbarComponents(toolbarSwitch);
```

#### Toolbar Popovers

`ToolbarPopover` anchors a popover to a `ToolbarSwitch` and syncs open/close state:

```java
ToolbarSwitch colorSwitch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
TextField colorField = new TextField("Color");
Button applyBtn = new Button("Apply");

ToolbarPopover popover = ToolbarPopover.vertical(colorSwitch, colorField, applyBtn);
popover.setFocusOnOpenTarget(colorField);

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, colorSwitch);
```

Factory methods: `vertical(switch, components...)`, `horizontal(switch, components...)`.

#### Toolbar Select Popups

`ToolbarSelectPopup` opens a context menu anchored to a `ToolbarSwitch`:

```java
ToolbarSwitch insertSwitch = new ToolbarSwitch(VaadinIcon.PLUS);
ToolbarSelectPopup menu = new ToolbarSelectPopup(insertSwitch);
menu.addItem("Horizontal Rule", e -> { /* insert HR */ });
menu.addItem("Page Break", e -> { /* insert page break */ });
menu.addComponent(new Hr());
menu.addItem("Special Character...", e -> { /* open dialog */ });

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, insertSwitch);
```

Uses standard Vaadin `ContextMenu` API.

#### Toolbar Dialogs

`ToolbarDialog` opens a non-modal dialog controlled by a `ToolbarSwitch` (non-modal, resizable, draggable, closes on ESC):

```java
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
ToolbarDialog dialog = new ToolbarDialog(settingsSwitch, true); // openAtSwitch
dialog.add(new Checkbox("Show rulers"), new Checkbox("Show whitespace"));
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
```

Factory methods: `vertical(switch, components...)`, `horizontal(switch, components...)`.

**Removing components:**

```java
editor.removeToolbarComponent(ToolbarSlot.START, startBtn);
// or by ID:
editor.removeToolbarComponent(ToolbarSlot.START, "slot-start-btn");
```

> **Styling note:** All components added via `addToolbarComponents()` automatically receive `part="toolbar-custom-component"`. This enables consistent styling through ERTE's shadow DOM (hover, focus, active/pressed states for buttons). See [Section 2.10.2](#2102-erte-shadow-parts) for styling details.

#### Toolbar Button Visibility

```java
// Hide specific buttons
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.CLEAN, false,
    EnhancedRichTextEditor.ToolbarButton.BLOCKQUOTE, false
));

// Reset all buttons to visible
editor.setToolbarButtonsVisibility(null);
```

When all buttons in a group are hidden, the group container auto-hides. See Javadoc for the complete enum list.

#### Custom Keyboard Shortcuts

```java
// Ctrl+Shift+B toggles Bold
editor.addStandardToolbarButtonShortcut(
    EnhancedRichTextEditor.ToolbarButton.BOLD,
    Key.KEY_B, KeyModifier.CONTROL, KeyModifier.SHIFT);

// Shift+F9 triggers Align Center
editor.addStandardToolbarButtonShortcut(
    EnhancedRichTextEditor.ToolbarButton.ALIGN_CENTER,
    Key.F9, KeyModifier.SHIFT);

// F9 without modifiers triggers Image
editor.addStandardToolbarButtonShortcut(
    EnhancedRichTextEditor.ToolbarButton.IMAGE, Key.F9);

// Move focus from editor to toolbar
editor.addToolbarFocusShortcut(Key.F10, KeyModifier.SHIFT);
```

Uses Vaadin's `Key` constants (`Key.KEY_B`, `Key.F9`, `Key.ENTER`, etc.) and `KeyModifier` varargs. `KeyModifier.CONTROL` maps to Ctrl (Win/Linux) or Cmd (Mac).

#### Replacing Toolbar Button Icons

```java
// Replace Bold with a star
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.BOLD, VaadinIcon.STAR.create());

// Replace Undo with arrow
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.UNDO, VaadinIcon.ARROW_LEFT.create());

// Restore original
editor.replaceStandardToolbarButtonIcon(ToolbarButton.BOLD, null);
```

---

### 2.2 Placeholders

Placeholders are embedded tokens for mail merge, templates, and dynamic fields. They support full lifecycle events, formatting (normal and alternative), and keyboard insertion.

#### Configuration

```java
List<Placeholder> placeholders = new ArrayList<>();

// Placeholder with italic default and bold alt format
Placeholder p1 = new Placeholder();
p1.setText("N-1=Company Name");
p1.getFormat().put("italic", true);
p1.getAltFormat().put("bold", true);
placeholders.add(p1);

// With link in alt format
Placeholder p2 = new Placeholder();
p2.setText("A-1=Street Address");
p2.getAltFormat().put("link", "https://example.com");
placeholders.add(p2);

// Plain placeholder
Placeholder p3 = new Placeholder();
p3.setText("D-1=2024-01-01");
placeholders.add(p3);

editor.setPlaceholders(placeholders);
```

**Display tags** -- wrap placeholder text with markers:

```java
// Display as @N-1=Company Name (start tag only)
editor.setPlaceholderTags("@", "");

// Display as {{Company Name}} (start + end tags)
editor.setPlaceholderTags("{{", "}}");
```

**Alt appearance pattern** -- regex to auto-switch formatting:

```java
// Switch to altFormat when text after "=" is matched
editor.setPlaceholderAltAppearancePattern("(?<=\\=).*$");

// Toggle alt appearance programmatically
editor.setPlaceholderAltAppearance(true);
```

#### Inserting Placeholders

Users insert via: **Toolbar button** (dialog with combo-box), **Keyboard shortcut** (Ctrl+P / Cmd+P), or **Programmatic insertion** (`PlaceholderButtonClickedEvent.insert()`).

#### Event Lifecycle

Placeholder events have cancel/confirm semantics. Default actions are prevented—listeners **must** call `event.insert()` or `event.remove()` to proceed.

```java
// 1. Toolbar button clicked
editor.addPlaceholderButtonClickedListener(event -> {
    event.insert(placeholders.get(0));
    // or at specific position:
    event.insert(placeholders.get(0), event.getPosition());
});

// 2. Before insert (cancellable)
editor.addPlaceholderBeforeInsertListener(event -> {
    if (isValid(event.getPlaceholders())) {
        event.insert();  // REQUIRED to proceed
    }
});

// 3. After insert (notification)
editor.addPlaceholderInsertedListener(event -> {
    event.getPlaceholders().forEach(p ->
        Notification.show("Inserted: " + p.getText()));
});

// 4. Before remove (cancellable)
editor.addPlaceholderBeforeRemoveListener(event -> {
    if (!isProtected(event.getPlaceholders())) {
        event.remove();  // REQUIRED to proceed
    }
});

// 5. After remove (notification)
editor.addPlaceholderRemovedListener(event -> {
    event.getPlaceholders().forEach(p ->
        Notification.show("Removed: " + p.getText()));
});

// 6. Placeholder selected
editor.addPlaceholderSelectedListener(event -> {
    showDetails(event.getPlaceholders().get(0));
});

// 7. Cursor leaves placeholder
editor.addPlaceholderLeaveListener(event -> clearDetails());

// 8. Appearance changed
editor.addPlaceholderAppearanceChangedListener(event -> {
    boolean isAlt = event.getAltAppearance();
});
```

#### Formatting

Placeholders support Quill inline format attributes:

| Attribute | Type | Example |
|-----------|------|---------|
| `bold` | boolean | `format.put("bold", true)` |
| `italic` | boolean | `format.put("italic", true)` |
| `color` | String | `format.put("color", "red")` |
| `background` | String | `format.put("background", "#ffcc00")` |
| `font` | String | `format.put("font", "monospace")` |
| `link` | String | `format.put("link", "https://example.com")` |
| `script` | String | `format.put("script", "super")` |

Set `format` for the default appearance and `altFormat` for the alternative appearance (triggered by the alt appearance pattern or toggle button).

---

### 2.3 Tabstops and Rulers

Tabstops enable document-style tab alignment. Pressing Tab inserts an embedded tab with width calculated from the defined tabstop positions and alignment direction.

#### Tabstops

```java
// Set 3 tabstops at specific pixel positions
editor.setTabStops(List.of(
    new TabStop(TabStop.Direction.LEFT, 150),
    new TabStop(TabStop.Direction.RIGHT, 350),
    new TabStop(TabStop.Direction.MIDDLE, 550)
));

// Read current tabstops
List<TabStop> current = editor.getTabStops();
```

**Alignment:** `LEFT` (text right-aligned to tabstop), `RIGHT` (left-aligned), `MIDDLE` (centered). Positions in pixels from content left edge.

Note: ERTE handles vertical arrow navigation through tab-heavy content by intercepting ArrowUp/ArrowDown and calculating correct target lines.

#### Rulers

Rulers show a visual grid: horizontal ruler displays pixel positions and cycles tabstop direction (LEFT → RIGHT → MIDDLE → remove) on click. Vertical ruler shows line-height reference.

```java
editor.setNoRulers(true);   // Hide rulers (tabstops still work)
editor.setNoRulers(false);  // Show rulers
```

---

### 2.4 Readonly Sections

Readonly sections protect inline content from editing. Select text and click the lock icon to toggle, or use Delta attributes.

```java
// Set readonly content via Delta
editor.asDelta().setValue("["
    + "{\"insert\":\"Editable text. \"},"
    + "{\"insert\":\"Readonly.\","
    +   "\"attributes\":{\"readonly\":true}},"
    + "{\"insert\":\" Editable.\\n\"}"
    + "]");

// Whole-editor readonly
editor.setReadOnly(true);
editor.setReadOnly(false);
```

**Features:** Delete protection (auto-reverted), gray background, supports additional formatting within readonly spans.

---

### 2.5 Whitespace Indicators

Display special characters for invisible formatting (→ Tab, ↵ Soft-break, ¶ Paragraph, · NBSP):

```java
editor.setShowWhitespace(true);
boolean showing = editor.isShowWhitespace();
```

The WHITESPACE toolbar button also toggles this.

---

### 2.6 Non-Breaking Space

Press **Shift+Space** to insert a non-breaking space (not collapsed by browser, prevents line breaks, preserved in HTML output). Built-in keyboard behavior—no Java API needed.

---

### 2.7 Soft-Break and Tab Copying

**Soft-Break:** Press **Shift+Enter** to insert a line break within a paragraph (no new `<p>` tag). Useful for addresses, poetry, etc.

**Tab Copying:** After soft-break, tabs from the line start to cursor are copied to the new line (limited by defined tabstop count). Enables indented multi-line content with consistent alignment.

---

### 2.8 Programmatic Text Insertion

You can insert text at the current cursor position or at a specific index. This is useful for inserting boilerplate, auto-complete values, or text from external sources.

```java
// Insert at cursor position
editor.addText("INSERTED");

// Insert at specific position (0 = beginning)
editor.addText("PREFIX", 0);

// Get text length (async callback)
editor.getTextLength(length ->
    Notification.show("Length: " + length));
```

`addText(text, position)` clamps out-of-bounds positions. The no-position form requires a focused editor with an active selection. `getTextLength()` is asynchronous and returns the length via a callback.

---

### 2.9 Align Justify

ERTE adds a justify alignment button to the toolbar, next to the existing left/center/right buttons. It's visible by default — hide it if you don't need it:

```java
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.ALIGN_JUSTIFY, false
));
```

---

### 2.10 Styling and Theming

ERTE uses the Vaadin Lumo theme. It provides 22 CSS custom properties for ERTE-specific visual elements, shadow parts for toolbar targeting, and content classes for editor styling.

For standard RTE 2 properties (`--vaadin-rich-text-editor-*`), see the [Vaadin RTE Styling docs](https://vaadin.com/docs/v25/components/rich-text-editor/styling).

#### 2.10.1 ERTE Custom Properties

ERTE provides 22 CSS custom properties for readonly sections, placeholders, whitespace indicators, and rulers. See [ARCHITECTURE.md](dev/ARCHITECTURE.md#custom-properties) for the complete reference.

#### 2.10.2 ERTE Shadow Parts

ERTE adds these shadow parts on top of the standard RTE 2 parts. Standard toolbar parts (bold, italic, etc.) and groups are provided by the Vaadin RTE 2 component -- see [Vaadin RTE Styling docs](https://vaadin.com/docs/v25/components/rich-text-editor/styling).

**ERTE toolbar buttons:**

| Part | Element |
|------|---------|
| `toolbar-button-readonly` | Readonly toggle button |
| `toolbar-button-placeholder` | Placeholder insert button |
| `toolbar-button-placeholder-display` | Placeholder appearance toggle |
| `toolbar-button-whitespace` | Whitespace indicators toggle |
| `toolbar-button-align-justify` | Justify alignment button |

**Custom group and slotted components:**

| Part | Element |
|------|---------|
| `toolbar-group-custom` | Custom button group container |
| `toolbar-custom-component` | All components added via `addToolbarComponents()` |

Slotted component state selectors:

| State | Selector |
|-------|----------|
| Default | `::slotted([part~='toolbar-custom-component'])` |
| Hover | `::slotted(button[part~='toolbar-custom-component']:not([on]):hover)` |
| Focus | `::slotted(button[part~='toolbar-custom-component']:focus-visible)` |
| Pressed | `::slotted([part~='toolbar-custom-component'][on])` |
| Disabled | `::slotted([part~='toolbar-custom-component'][disabled])` |

**Ruler parts:**

| Part | Purpose |
|------|---------|
| `ruler-wrapper` | Wrapper for the entire ruler system |
| `ruler-corner` | Corner element (intersection of rulers) |
| `horizontalRuler` | Horizontal ruler bar |
| `verticalRuler` | Vertical ruler bar |
| `content-wrapper` | Wrapper around editor content |

```css
vcf-enhanced-rich-text-editor::part(toolbar-button-readonly) {
    color: red;
}
vcf-enhanced-rich-text-editor::part(horizontalRuler) {
    background-color: var(--lumo-contrast-10pct);
}
```

#### 2.10.3 Content Classes

ERTE-specific classes applied inside `.ql-editor` (editor content area). Standard Quill classes (`ql-align-*`, `ql-indent-*`) are provided by the base RTE 2 component.

| Class | Element | Purpose |
|-------|---------|---------|
| `.ql-tab` | `<span>` | Tab embed (calculated width) |
| `.ql-placeholder` | `<span>` | Placeholder embed |
| `.ql-readonly` | `<span>` | Readonly section (`contenteditable="false"`) |
| `.ql-soft-break` | `<span>` | Soft-break embed (contains `<br>`) |
| `.ql-nbsp` | `<span>` | Non-breaking space |

---

### 2.11 Built-in Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Tab | Insert tab embed (if tabstops configured) |
| Shift+Enter | Insert soft-break |
| Shift+Space | Insert non-breaking space |
| Ctrl+P / Cmd+P | Open placeholder dialog (if placeholders configured) |

> **Note:** Browsers may intercept Ctrl+P. Use the toolbar button as alternative.

---

## 3. Advanced Features

### 3.1 Value Formats (HTML vs Delta)

With Vaadin 25, the Rich Text Editor uses **HTML as primary format**. If you need to work with Quill's Delta JSON (e.g., for programmatic readonly sections or batch updates), use the `asDelta()` wrapper.

```java
// HTML (primary)
editor.setValue("<p>Hello <strong>world</strong></p>");
String html = editor.getValue();

// Delta (secondary)
editor.asDelta().setValue("[{\"insert\":\"Hello \"},{\"insert\":\"world\",\"attributes\":{\"bold\":true}}]");
String deltaJson = editor.asDelta().getValue();
```

**When to use:** HTML for storage/forms. Delta for programmatic readonly sections and batch updates.

---

### 3.2 Internationalization (I18n)

ERTE extends RTE 2's i18n with labels for ERTE-specific buttons and dialogs:

```java
EnhancedRichTextEditor.EnhancedRichTextEditorI18n i18n =
    new EnhancedRichTextEditor.EnhancedRichTextEditorI18n();

// Standard RTE 2 labels
i18n.setBold("Fett");
i18n.setItalic("Kursiv");
i18n.setUndo("Ruckgangig");

// ERTE-specific labels
i18n.setReadonly("Schreibschutz");
i18n.setWhitespace("Leerzeichen anzeigen");
i18n.setPlaceholder("Platzhalter");
i18n.setPlaceholderAppearance("Platzhalter-Darstellung");
i18n.setPlaceholderDialogTitle("Platzhalter");
i18n.setPlaceholderComboBoxLabel("Platzhalter wahlen");
i18n.setPlaceholderAppearanceLabel1("Normal");
i18n.setPlaceholderAppearanceLabel2("Wert");
i18n.setAlignJustify("Blocksatz");

editor.setI18n(i18n);
```

**ERTE-specific labels:**

| Method | Default |
|--------|---------|
| `setReadonly()` | "Readonly" |
| `setWhitespace()` | "Show whitespace" |
| `setPlaceholder()` | "Placeholder" |
| `setPlaceholderAppearance()` | "Toggle placeholder appearance" |
| `setPlaceholderDialogTitle()` | "Placeholders" |
| `setPlaceholderComboBoxLabel()` | "Select a placeholder" |
| `setPlaceholderAppearanceLabel1()` | "Plain" |
| `setPlaceholderAppearanceLabel2()` | "Value" |
| `setAlignJustify()` | "Justify" |

Standard RTE 2 i18n (inherited): bold, italic, underline, strike, color, background, headings, subscript, superscript, lists, indent/outdent, alignment, link, image, blockquote, code block, clean.

---

### 3.3 Sanitization

ERTE includes a server-side HTML sanitizer that runs automatically when content is received from the client. It prevents XSS attacks while preserving all ERTE-specific content like readonly sections, tabstops, placeholders, and soft-breaks.

You don't need to configure anything — the sanitizer works out of the box. It preserves standard HTML tags, ERTE classes (`ql-readonly`, `ql-tab`, `ql-soft-break`, `ql-placeholder`, `ql-nbsp`), Quill classes (`ql-align-*`, `ql-indent-*`), and safe attributes. It strips script tags, event handlers, dangerous CSS, and unknown classes.

See `SECURITY.md` for the full allowlist and security details.

---

---

## 4. Getting Help

- **Upgrade from v5.x:** [Upgrade Guide](BASE_UPGRADE_GUIDE.md)
- **Extension hooks and custom blots:** [EXTENDING.md](dev/EXTENDING.md)
- **Demo application:** Run the demo module (`enhanced-rich-text-editor-demo/`) for working examples of all features
- **Issues:** [GitHub](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/issues)
