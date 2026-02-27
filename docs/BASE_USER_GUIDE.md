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
- [4. Known Limitations](#4-known-limitations)
- [5. Getting Help](#5-getting-help)

---

## 1. Getting Started

### 1.1 Installation

ERTE v6.x requires Vaadin 25.0.x and Java 21+. Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor</artifactId>
    <version>6.0.0</version>
</dependency>
```

> **Note:** Vaadin 25 moved the Rich Text Editor to the commercial `vaadin` artifact (not `vaadin-core`). A Vaadin Pro subscription or higher is required for production use.

> **Theme:** ERTE v6.0 uses the Vaadin Lumo theme. Ensure your application loads Lumo (the default for Vaadin 25). For theme setup details, see [Upgrade Guide -- Section 2.5](BASE_UPGRADE_GUIDE.md).

### 1.2 Basic Usage

```java
EnhancedRichTextEditor editor = new EnhancedRichTextEditor();

// Set and get HTML
editor.setValue("<p>Hello, world!</p>");
String html = editor.getValue();

// Listen for changes (fires on blur)
editor.addValueChangeListener(event -> {
    String newValue = event.getValue();
    Notification.show("Content changed");
});
```

### 1.3 Quick Setup Example

```java
import com.vaadin.componentfactory.EnhancedRichTextEditor;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;

EnhancedRichTextEditor editor = new EnhancedRichTextEditor();

// Hide buttons
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.IMAGE, false,
    EnhancedRichTextEditor.ToolbarButton.CODE_BLOCK, false
));

// Add custom button
Button saveBtn = new Button("Save");
saveBtn.addClickListener(e -> save(editor.getValue()));
editor.addToolbarComponents(ToolbarSlot.END, saveBtn);

// Configure tabstops
editor.setTabStops(List.of(
    new TabStop(TabStop.Direction.LEFT, 150),
    new TabStop(TabStop.Direction.RIGHT, 350)
));
```

### 1.4 Common Use Cases

**Read-only viewer:**
```java
EnhancedRichTextEditor viewer = new EnhancedRichTextEditor();
viewer.setReadOnly(true);
viewer.setValue(savedHtml);
```

**Insert text programmatically:**
```java
editor.addText("PREFIX", 0);  // at beginning
editor.addText("INSERTED");   // at cursor
editor.getTextLength(len -> Notification.show("Length: " + len));
```

---

## 2. Core Features

### 2.1 Toolbar Customization

The toolbar is ERTE's most flexible feature. It supports adding custom components to 27 named slots, controlling button visibility, binding keyboard shortcuts, and replacing button icons.

#### Adding Components to Toolbar Slots

ERTE provides 27 `ToolbarSlot` positions for custom components, organized around 11 button groups:

```
[START] [BH|Undo|Redo|AH] [BE|Bold|Italic|...|AE] ... [GROUP_CUSTOM] [END]
```

Slots: `START`/`END` (edges), `BEFORE_GROUP_X`/`AFTER_GROUP_X` (22 positions), `BEFORE_GROUP_CUSTOM`/`GROUP_CUSTOM`/`AFTER_GROUP_CUSTOM` (custom area).

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
    "b", true, true, false);  // key, shortKey, shiftKey, altKey

// Shift+F9 triggers Align Center
editor.addStandardToolbarButtonShortcut(
    EnhancedRichTextEditor.ToolbarButton.ALIGN_CENTER,
    "F9", false, true, false);

// Move focus from editor to toolbar
editor.addToolbarFocusShortcut("F10", false, true, false);
```

Parameters: `key` is a Quill 2 key name (`"b"`, `"F9"`, `"Enter"`, etc.). `shortKey` maps to Ctrl/Cmd depending on platform.

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

```java
// Insert at cursor position
editor.addText("INSERTED");

// Insert at specific position (0 = beginning)
editor.addText("PREFIX", 0);

// Get text length (async callback)
editor.getTextLength(length ->
    Notification.show("Length: " + length));
```

Notes: `getTextLength()` is async (callback pattern). `addText(text, position)` clamps out-of-bounds positions. No insertion if editor disabled. No-position form requires focused editor with active selection.

---

### 2.9 Align Justify

ERTE adds justify alignment (included by default):

```java
// Hide if not needed
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.ALIGN_JUSTIFY, false
));
```

Uses standard Quill justify format.

---

### 2.10 Styling and Theming

ERTE uses the Vaadin Lumo theme. It provides 22 CSS custom properties for ERTE-specific visual elements, shadow parts for toolbar targeting, and content classes for editor styling.

For standard RTE 2 properties (`--vaadin-rich-text-editor-*`), see the [Vaadin RTE Styling docs](https://vaadin.com/docs/v25/components/rich-text-editor/styling).

#### 2.10.1 ERTE Custom Properties

Override on the host element:

```css
vcf-enhanced-rich-text-editor {
    --vaadin-erte-readonly-background: lightyellow;
    --vaadin-erte-placeholder-background: #e8f4fd;
    --vaadin-erte-ruler-height: 1.25rem;
}
```

**Readonly Sections (6):**

| Property | Default |
|----------|---------|
| `--vaadin-erte-readonly-color` | `var(--vaadin-text-color-secondary)` |
| `--vaadin-erte-readonly-background` | `var(--vaadin-background-container)` |
| `--vaadin-erte-readonly-border-color` | `var(--vaadin-border-color-secondary)` |
| `--vaadin-erte-readonly-border-width` | `1px` |
| `--vaadin-erte-readonly-border-radius` | `var(--lumo-border-radius-s)` |
| `--vaadin-erte-readonly-padding` | `calc(var(--vaadin-padding-xs) / 2)` |

**Placeholders (6):**

| Property | Default |
|----------|---------|
| `--vaadin-erte-placeholder-color` | `inherit` |
| `--vaadin-erte-placeholder-background` | `var(--lumo-primary-color-10pct)` |
| `--vaadin-erte-placeholder-border-color` | `transparent` |
| `--vaadin-erte-placeholder-border-width` | `0` |
| `--vaadin-erte-placeholder-border-radius` | `var(--lumo-border-radius-s)` |
| `--vaadin-erte-placeholder-padding` | `calc(var(--vaadin-padding-xs) / 2)` |

**Whitespace Indicators (3):**

| Property | Default |
|----------|---------|
| `--vaadin-erte-whitespace-indicator-color` | `var(--lumo-contrast-40pct, rgba(0, 0, 0, 0.38))` |
| `--vaadin-erte-whitespace-paragraph-indicator-color` | `var(--lumo-contrast-30pct, rgba(0, 0, 0, 0.26))` |
| `--vaadin-erte-whitespace-indicator-spacing` | `calc(var(--vaadin-padding-xs) / 2)` |

**Ruler (7):**

| Property | Default |
|----------|---------|
| `--vaadin-erte-ruler-height` | `0.9375rem` |
| `--vaadin-erte-ruler-border-color` | `var(--vaadin-border-color, var(--lumo-contrast-20pct))` |
| `--vaadin-erte-ruler-background` | SVG tick image (internal) |
| `--vaadin-erte-ruler-marker-size` | `0.9375rem` |
| `--vaadin-erte-ruler-marker-color` | `inherit` |
| `--vaadin-erte-ruler-vertical-width` | `0.9375rem` |
| `--vaadin-erte-ruler-vertical-background` | Ruler tick image (base64 PNG) |

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

These shortcuts are always active and cannot be removed:

| Shortcut | Action |
|----------|--------|
| Tab | Insert tab embed (if tabstops configured) |
| Shift+Enter | Insert soft-break |
| Shift+Space | Insert non-breaking space |
| Ctrl+P / Cmd+P | Open placeholder dialog (if placeholders configured) |

> **Note:** Ctrl+P / Cmd+P may be intercepted by the browser's print dialog. In Chromium-based browsers, the browser intercepts the shortcut before JavaScript. Users can use the toolbar button as an alternative.

---

## 3. Advanced Features

### 3.1 Value Formats (HTML vs Delta)

ERTE uses **HTML as primary format** (matching RTE 2), with Delta JSON as optional secondary.

#### HTML (Default)

```java
editor.setValue("<p>Hello <strong>world</strong></p>");
String html = editor.getValue();

editor.addValueChangeListener(e -> {
    String newHtml = e.getValue();
});
```

#### Delta (Optional)

```java
editor.asDelta().setValue(
    "[{\"insert\":\"Hello \"},{\"insert\":\"world\",\"attributes\":{\"bold\":true}},{\"insert\":\"\\n\"}]"
);
String deltaJson = editor.asDelta().getValue();

editor.asDelta().addValueChangeListener(e -> {
    String newDelta = e.getValue();
});
```

**Use Delta for:** Programmatic readonly sections, batch updates, precise format control. **Use HTML for:** Form binding, database storage, user-visible content.

**Delta examples:**
```json
[{"insert": "Hello World\n"}]
[{"insert": "Col1"}, {"insert": {"tab": true}}, {"insert": "Col2\n"}]
[{"insert": "Protected", "attributes": {"readonly": true}}, {"insert": "\n"}]
[{"insert": {"placeholder": {"text": "Company Name"}}}, {"insert": "\n"}]
```

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

Inherited RTE 2: `setUndo()`, `setRedo()`, `setBold()`, `setItalic()`, `setUnderline()`, `setStrike()`, `setColor()`, `setBackground()`, `setH1()`, `setH2()`, `setH3()`, `setSubscript()`, `setSuperscript()`, `setListOrdered()`, `setListBullet()`, `setOutdent()`, `setIndent()`, `setAlignLeft()`, `setAlignCenter()`, `setAlignRight()`, `setImage()`, `setLink()`, `setBlockquote()`, `setCodeBlock()`, `setClean()`.

---

### 3.3 Sanitization

Server-side HTML sanitizer prevents XSS while preserving ERTE content.

**Preserves:** Standard HTML tags (`p`, `br`, `strong`, `em`, `u`, `s`, `h1`-`h3`, `img`, `a`, `ol`, `ul`, `li`, `blockquote`, `pre`), ERTE classes (`ql-readonly`, `ql-tab`, `ql-soft-break`, `ql-placeholder`, `ql-nbsp`), Quill classes (`ql-align-*`, `ql-indent-*`), `contenteditable="false"` on spans, safe CSS properties, safe image data URLs.

**Strips:** Script tags, event handlers, dangerous CSS functions, `@import` directives, SVG data URLs, unknown CSS classes, `contenteditable="true"`.

See `SECURITY.md` for full security details.

---

## 4. Known Limitations

These are Quill 2 / Parchment 3 platform constraints, not ERTE bugs:

| Issue | Workaround |
|-------|-----------|
| Readonly undo removes formatting | Re-apply readonly after undo |
| Bold/Italic at tab boundary may not work | Place cursor away from embed, then apply format |
| Placeholder copy-paste doesn't survive clipboard roundtrip | Use programmatic insertion |
| Undo doesn't restore removed embed blots | Manual re-insertion |

---

## 5. Getting Help

- **Upgrade from v5.x:** [Upgrade Guide](BASE_UPGRADE_GUIDE.md)
- **Extension hooks and custom blots:** [EXTENDING.md](dev/EXTENDING.md)
- **Demo application:** Run the demo module (`enhanced-rich-text-editor-demo/`) for working examples of all features
- **Issues:** [GitHub](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/issues)
