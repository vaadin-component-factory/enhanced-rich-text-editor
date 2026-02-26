# Enhanced Rich Text Editor -- User Guide

This guide covers all features of the Enhanced Rich Text Editor (ERTE) v6.x for Vaadin 25. It is aimed at Java developers integrating ERTE into their Vaadin applications.

**Scope:** Feature overview, working code examples, best practices, and troubleshooting. For the complete API surface, see [API Reference](API_REFERENCE.md). For configuration patterns, see [Configuration Guide](CONFIGURATION.md). For migrating from v5.x, see [Upgrade Guide](UPGRADE_GUIDE.md).

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
- [3. Advanced Features](#3-advanced-features)
  - [3.1 Value Formats (HTML vs Delta)](#31-value-formats-html-vs-delta)
  - [3.2 Extension Hooks](#32-extension-hooks)
  - [3.3 Internationalization (I18n)](#33-internationalization-i18n)
  - [3.4 Sanitization](#34-sanitization)
- [4. Best Practices](#4-best-practices)
- [5. Troubleshooting](#5-troubleshooting)

---

## 1. Getting Started

### 1.1 Installation

ERTE v6.x requires Vaadin 25.0.x and Java 21+. Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-v25</artifactId>
    <version>6.0.0</version>
</dependency>
```

> **Note:** Vaadin 25 moved the Rich Text Editor to the commercial `vaadin` artifact (not `vaadin-core`). A Vaadin Pro subscription or higher is required for production use.

> **Theme:** ERTE v6.x uses the Vaadin Lumo theme. Ensure your application loads Lumo (the default for Vaadin 25). For theme setup details, see [Upgrade Guide -- Section 2.5](UPGRADE_GUIDE.md).

### 1.2 Basic Usage

Create an editor, get/set its value, and listen for changes:

```java
EnhancedRichTextEditor editor = new EnhancedRichTextEditor();

// Set HTML content
editor.setValue("<p>Hello, world!</p>");

// Get HTML content
String html = editor.getValue();

// Listen for value changes (fires on blur)
editor.addValueChangeListener(event -> {
    String newValue = event.getValue();
    Notification.show("Content changed");
});
```

### 1.3 First Steps

A typical setup includes configuring toolbar visibility, adding custom toolbar components, and handling events:

```java
import com.vaadin.componentfactory.EnhancedRichTextEditor;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;

// Create editor with custom toolbar
EnhancedRichTextEditor editor = new EnhancedRichTextEditor();

// Hide buttons you don't need
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.IMAGE, false,
    EnhancedRichTextEditor.ToolbarButton.CODE_BLOCK, false
));

// Add a custom button to the toolbar
Button saveBtn = new Button("Save");
saveBtn.addClickListener(e -> save(editor.getValue()));
editor.addToolbarComponents(ToolbarSlot.END, saveBtn);

// Add tabstops for document formatting
editor.setTabStops(List.of(
    new TabStop(TabStop.Direction.LEFT, 150),
    new TabStop(TabStop.Direction.RIGHT, 350)
));
```

### 1.4 Common Use Cases

**Form field with value binding:**
```java
EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
editor.setValueChangeMode(ValueChangeMode.EAGER);
editor.addValueChangeListener(e -> binder.validate());
```

**Read-only content display:**
```java
EnhancedRichTextEditor viewer = new EnhancedRichTextEditor();
viewer.setReadOnly(true);
viewer.setValue(savedHtml);
```

**Programmatic text insertion:**
```java
editor.addText("PREFIX", 0);        // Insert at beginning
editor.addText("INSERTED");          // Insert at cursor
editor.getTextLength(length ->        // Get text length (async)
    Notification.show("Length: " + length));
```

---

## 2. Core Features

### 2.1 Toolbar Customization

The toolbar is ERTE's most flexible feature. It supports adding custom components to 27 named slots, controlling button visibility, binding keyboard shortcuts, and replacing button icons.

#### Adding Components to Toolbar Slots

ERTE provides 27 `ToolbarSlot` positions where you can place custom Vaadin components. Slots are organized around the toolbar's 11 button groups:

```
[START] [BH|Undo|Redo|AH] [BE|Bold|Italic|...|AE] ... [GROUP_CUSTOM] [END]
```

- `START` / `END` -- toolbar edges
- `BEFORE_GROUP_X` / `AFTER_GROUP_X` -- around each button group (22 slots)
- `BEFORE_GROUP_CUSTOM` / `GROUP_CUSTOM` / `AFTER_GROUP_CUSTOM` -- custom area

**Example: Adding buttons to multiple slots**

```java
// Source: ErteToolbarTestView.java

// Button at the start of the toolbar
Button startBtn = new Button("S");
startBtn.setId("slot-start-btn");
editor.addToolbarComponents(ToolbarSlot.START, startBtn);

// Button at the end of the toolbar
Button endBtn = new Button("E");
editor.addToolbarComponents(ToolbarSlot.END, endBtn);

// Button before the history group (undo/redo)
Button beforeHistoryBtn = new Button("BH");
editor.addToolbarComponents(ToolbarSlot.BEFORE_GROUP_HISTORY, beforeHistoryBtn);

// Button after the emphasis group (bold/italic/underline/strike)
Button afterEmphasisBtn = new Button("AE");
editor.addToolbarComponents(ToolbarSlot.AFTER_GROUP_EMPHASIS, afterEmphasisBtn);

// Convenience: add to the custom group
Button customBtn = new Button("C");
editor.addCustomToolbarComponents(customBtn);
```

**ToolbarSwitch** -- a toggle button for toolbar use:

```java
// Source: ErteToolbarTestView.java
ToolbarSwitch toolbarSwitch = new ToolbarSwitch("SW");
toolbarSwitch.addActiveChangedListener(e ->
    System.out.println("Switch active: " + e.isActive()));
editor.addCustomToolbarComponents(toolbarSwitch);
```

#### Toolbar Popovers

`ToolbarPopover` opens a popover panel anchored to a `ToolbarSwitch`. It syncs open/close state with the switch and supports custom focus targets.

```java
// Source: V25DemoView.java

// Color picker popover with a text field and apply button
ToolbarSwitch colorSwitch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
TextField colorField = new TextField("Color");
colorField.setPlaceholder("#000000");
Button applyBtn = new Button("Apply");

ToolbarPopover popover = ToolbarPopover.vertical(colorSwitch, colorField, applyBtn);
popover.setFocusOnOpenTarget(colorField);  // Focus the text field on open

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, colorSwitch);
```

Factory methods: `vertical(switch, components...)`, `horizontal(switch, components...)`, `horizontal(switch, alignment, components...)`.

> See [API Reference -- ToolbarPopover](API_REFERENCE.md#7-toolbarpopover) for the complete API.

#### Toolbar Select Popups

`ToolbarSelectPopup` opens a context menu on left-click (not right-click) anchored to a `ToolbarSwitch`. It syncs open/close state with the switch.

```java
// Source: V25DemoView.java

// Insert menu with multiple options
ToolbarSwitch insertSwitch = new ToolbarSwitch(VaadinIcon.PLUS);
ToolbarSelectPopup menu = new ToolbarSelectPopup(insertSwitch);
menu.addItem("Horizontal Rule", e -> { /* insert HR */ });
menu.addItem("Page Break", e -> { /* insert page break */ });
menu.addComponent(new Hr());
menu.addItem("Special Character...", e -> { /* open dialog */ });

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, insertSwitch);
```

Inherits the standard Vaadin `ContextMenu` API (`addItem()`, `addComponent()`, etc.).

> See [API Reference -- ToolbarSelectPopup](API_REFERENCE.md#8-toolbarselectpopup) for the complete API.

#### Toolbar Dialogs

`ToolbarDialog` opens a non-modal dialog controlled by a `ToolbarSwitch`. Defaults: non-modal, resizable, draggable, no padding, closes on ESC. Can be positioned at the switch button or centered.

```java
// Source: V25DemoView.java

// Settings dialog positioned at the toolbar switch
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
Checkbox showRulers = new Checkbox("Show rulers");
Checkbox showWhitespace = new Checkbox("Show whitespace");
Checkbox autoSave = new Checkbox("Auto-save");

ToolbarDialog.vertical(settingsSwitch, showRulers, showWhitespace, autoSave)
    .openAtSwitch();  // Position at the switch instead of centering

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
```

Factory methods: `vertical(switch, components...)`, `horizontal(switch, components...)`, `horizontal(switch, alignment, components...)`.

> See [API Reference -- ToolbarDialog](API_REFERENCE.md#9-toolbardialog) for the complete API.

**Removing components:**

```java
editor.removeToolbarComponent(ToolbarSlot.START, startBtn);
// or by ID:
editor.removeToolbarComponent(ToolbarSlot.START, "slot-start-btn");
```

> **Styling note:** All components added via `addToolbarComponents()` automatically receive `part="toolbar-custom-component"`. This enables consistent styling through ERTE's shadow DOM (hover, focus, active/pressed states for buttons). See [Configuration Guide -- Shadow Parts](CONFIGURATION.md#42-shadow-parts) for styling details.

#### Toolbar Button Visibility

Hide or show any of the 30 standard toolbar buttons:

```java
// Source: ErteToolbarTestView.java

// Hide specific buttons
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.CLEAN, false,
    EnhancedRichTextEditor.ToolbarButton.BLOCKQUOTE, false
));

// Hide all buttons in a group (group auto-hides)
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.BLOCKQUOTE, false,
    EnhancedRichTextEditor.ToolbarButton.CODE_BLOCK, false
));

// Reset all buttons to visible
editor.setToolbarButtonsVisibility(null);
```

The 30 toolbar buttons include 25 standard RTE buttons (UNDO, REDO, BOLD, ITALIC, UNDERLINE, STRIKE, COLOR, BACKGROUND, H1, H2, H3, SUBSCRIPT, SUPERSCRIPT, LIST_ORDERED, LIST_BULLET, OUTDENT, INDENT, ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT, IMAGE, LINK, BLOCKQUOTE, CODE_BLOCK, CLEAN) and 5 ERTE-specific buttons (READONLY, PLACEHOLDER, PLACEHOLDER_APPEARANCE, WHITESPACE, ALIGN_JUSTIFY).

> **Note:** When all buttons in a group are hidden, the group container is automatically hidden too.

#### Custom Keyboard Shortcuts

Bind keyboard shortcuts to toolbar buttons or move focus to the toolbar:

```java
// Source: ErteToolbarTestView.java

// Ctrl+Shift+B toggles Bold
editor.addStandardToolbarButtonShortcut(
    EnhancedRichTextEditor.ToolbarButton.BOLD,
    "b", true, true, false);  // shortKey=Ctrl, shiftKey=true, altKey=false

// Shift+F9 triggers Align Center
editor.addStandardToolbarButtonShortcut(
    EnhancedRichTextEditor.ToolbarButton.ALIGN_CENTER,
    "F9", false, true, false);

// Shift+F10 moves focus from editor to toolbar
editor.addToolbarFocusShortcut("F10", false, true, false);
```

Parameters: `key` is a Quill 2 key name (e.g., `"b"`, `"F9"`, `"Enter"`). `shortKey` maps to Ctrl on Windows/Linux and Cmd on Mac.

#### Replacing Toolbar Button Icons

Replace any standard toolbar button's icon with a Vaadin icon:

```java
// Source: ErteReplaceIconTestView.java

// Replace Bold icon with a star
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.BOLD, new Icon(VaadinIcon.STAR));

// Replace multiple icons
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.ITALIC, new Icon(VaadinIcon.FLAG));
editor.replaceStandardToolbarButtonIcon(
    ToolbarButton.UNDO, new Icon(VaadinIcon.ARROW_LEFT));

// Restore original icon
editor.replaceStandardToolbarButtonIcon(ToolbarButton.BOLD, null);
```

---

### 2.2 Placeholders

Placeholders are embedded tokens in editor content -- used for mail merge, document templates, and dynamic fields. They support a complete lifecycle with cancel/confirm semantics, formatting with normal and alternative appearances, and keyboard insertion.

#### Configuration

Define placeholders with text, optional format, and optional alt format:

```java
// Source: ErtePlaceholderTestView.java

List<Placeholder> placeholders = new ArrayList<>();

// Placeholder with italic default and bold alt format
Placeholder p1 = new Placeholder();
p1.setText("N-1=Company Name");
p1.getFormat().put("italic", true);
p1.getAltFormat().put("bold", true);
placeholders.add(p1);

// Placeholder with link in alt format
Placeholder p2 = new Placeholder();
p2.setText("A-1=Street Address");
p2.getAltFormat().put("link", "https://example.com");
placeholders.add(p2);

// Plain placeholder (no custom formatting)
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

Users insert placeholders through:
1. **Toolbar button** -- opens a dialog with a combo-box for selection
2. **Keyboard shortcut** -- Ctrl+P (Windows/Linux) or Cmd+P (Mac) opens the dialog
3. **Programmatic insertion** -- via the `PlaceholderButtonClickedEvent.insert()` method

#### Event Lifecycle

Placeholders have a complete event lifecycle with cancel/confirm semantics. The default action is always prevented -- the Java listener **must** call `event.insert()` or `event.remove()` for the operation to proceed.

**All 8 events:**

```java
// Source: ErtePlaceholderTestView.java

// 1. Toolbar button clicked -- open custom dialog or insert directly
editor.addPlaceholderButtonClickedListener(event -> {
    int position = event.getPosition();
    // Insert directly (bypasses built-in dialog):
    event.insert(placeholders.get(0));
    // Or insert at specific position:
    event.insert(placeholders.get(0), position);
});

// 2. Before insert -- validate, then MUST call insert() to proceed
editor.addPlaceholderBeforeInsertListener(event -> {
    List<Placeholder> toInsert = event.getPlaceholders();
    if (isValid(toInsert)) {
        event.insert();  // REQUIRED -- without this, insertion is cancelled
    }
    // Do nothing to cancel the insertion
});

// 3. After insert -- notification only
editor.addPlaceholderInsertedListener(event -> {
    event.getPlaceholders().forEach(p ->
        Notification.show("Inserted: " + p.getText()));
});

// 4. Before remove -- validate, then MUST call remove() to proceed
editor.addPlaceholderBeforeRemoveListener(event -> {
    if (!isProtected(event.getPlaceholders())) {
        event.remove();  // REQUIRED -- without this, removal is cancelled
    }
    // Do nothing to prevent removal
});

// 5. After remove -- notification only
editor.addPlaceholderRemovedListener(event -> {
    event.getPlaceholders().forEach(p ->
        Notification.show("Removed: " + p.getText()));
});

// 6. Placeholder selected (click or keyboard navigation)
editor.addPlaceholderSelectedListener(event -> {
    Placeholder selected = event.getPlaceholders().get(0);
    showDetails(selected);
});

// 7. Cursor leaves placeholder
editor.addPlaceholderLeaveListener(event -> {
    clearDetails();
});

// 8. Appearance changed (format/altFormat switch)
editor.addPlaceholderAppearanceChangedListener(event -> {
    boolean isAlt = event.getAltAppearance();
    String label = event.getAppearanceLabel();
});
```

> **Important:** `PlaceholderBeforeInsertEvent` and `PlaceholderBeforeRemoveEvent` are cancellable. You **must** call `event.insert()` or `event.remove()` for the operation to complete. This is by design -- it allows validation, confirmation dialogs, and protection logic.

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

Tabstops provide document-style tab alignment. When the user presses Tab, a tab embed is inserted. Tab width is calculated based on defined tabstop positions and alignment direction.

#### Tabstops

```java
// Source: ErteTabStopTestView.java

// Set 3 tabstops at specific pixel positions
editor.setTabStops(List.of(
    new TabStop(TabStop.Direction.LEFT, 150),
    new TabStop(TabStop.Direction.RIGHT, 350),
    new TabStop(TabStop.Direction.MIDDLE, 550)
));

// Read current tabstops
List<TabStop> current = editor.getTabStops();
```

**Alignment directions:**
- `LEFT` -- text aligns to the right of the tabstop (>text)
- `RIGHT` -- text aligns to the left of the tabstop (text<)
- `MIDDLE` -- text is centered at the tabstop (te|xt)

Position is in pixels from the left edge of the editor content area.

> **Arrow navigation:** ERTE provides custom ArrowUp/ArrowDown handling when tabs are present. Quill 2 renders tabs as inline-block elements, which can confuse the browser's native vertical cursor navigation. ERTE intercepts vertical arrow keys and calculates the correct target line, ensuring smooth navigation through tab-heavy content.

#### Rulers

Rulers provide a visual grid above and beside the editor:
- **Horizontal ruler** -- shows pixel positions, click to add/cycle/remove tabstops
- **Vertical ruler** -- shows line height reference

Clicking the horizontal ruler cycles tabstop direction: LEFT -> RIGHT -> MIDDLE -> remove.

```java
// Hide rulers (tabstops still work, just no visual indicator)
editor.setNoRulers(true);

// Show rulers
editor.setNoRulers(false);
```

---

### 2.4 Readonly Sections

Readonly sections protect inline content from editing. Selected text wrapped as readonly becomes non-editable while the surrounding content remains editable.

#### Using Readonly

1. **Toolbar button** -- select text and click the lock icon to toggle readonly
2. **Via Delta** -- set content with readonly attributes programmatically

```java
// Source: ErteReadonlyTestView.java

// Set readonly content via Delta JSON
editor.asDelta().setValue("["
    + "{\"insert\":\"Editable text before. \"},"
    + "{\"insert\":\"This is readonly content.\","
    +   "\"attributes\":{\"readonly\":true}},"
    + "{\"insert\":\" Editable text after.\\n\"}"
    + "]");
```

**Features:**
- Delete protection: attempting to delete readonly content is automatically reverted
- Gray background styling (default)
- Supports additional formatting (bold, italic, etc.) within readonly spans
- Readonly format attribute: `{"attributes": {"readonly": true}}`

**Whole-editor readonly** (inherited from RTE 2):
```java
editor.setReadOnly(true);   // Entire editor becomes non-editable
editor.setReadOnly(false);  // Restore editing
```

---

### 2.5 Whitespace Indicators

Whitespace indicators display special characters for invisible formatting:

| Indicator | Meaning |
|-----------|---------|
| `→` | Tab |
| `↵` | Soft-break (Shift+Enter) |
| `¶` | Paragraph end |
| `·` | Non-breaking space |

```java
// Enable whitespace indicators
editor.setShowWhitespace(true);

// Check current state
boolean showing = editor.isShowWhitespace();
```

The whitespace toolbar button (WHITESPACE) also toggles this feature.

---

### 2.6 Non-Breaking Space

Press **Shift+Space** to insert a non-breaking space. Unlike regular spaces, non-breaking spaces:
- Are not collapsed by the browser
- Prevent line breaks between words
- Are preserved in the HTML output

No Java API is needed -- this is a built-in keyboard behavior.

---

### 2.7 Soft-Break and Tab Copying

#### Soft-Break

Press **Shift+Enter** to insert a line break within a paragraph (without creating a new `<p>` tag). This is useful for formatting addresses, poetry, or any content where line breaks should not create new paragraphs.

#### Tab Copying

After a soft-break, tabs from the beginning of the current visual line up to the cursor position are automatically copied to the new line. The number of copied tabs is limited to the count of defined tabstops. This enables creating indented multi-line content with consistent tab alignment.

---

### 2.8 Programmatic Text Insertion

Insert text programmatically and query editor content length:

```java
// Source: ErteFeatureTestView.java

// Insert at the current cursor position
editor.addText("INSERTED");

// Insert at a specific position (0 = beginning)
editor.addText("PREFIX", 0);

// Get text length (async callback pattern)
editor.getTextLength(length ->
    Notification.show("Text length: " + length));
```

> **Note:** `getTextLength()` uses an async callback pattern (like `WebStorage.getItem()`). The callback fires once the length is available from the browser. Quill's trailing newline is excluded from the count.

**Behavior notes:**
- `addText(text, position)` clamps out-of-bounds positions to the valid range
- No insertion occurs if the editor is disabled
- `addText(text)` (no position) requires the editor to be focused with an active selection

---

### 2.9 Align Justify

ERTE adds a justify alignment option to the standard left/center/right alignment buttons:

```java
// The ALIGN_JUSTIFY toolbar button is included by default.
// Hide it if not needed:
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.ALIGN_JUSTIFY, false
));
```

The justify button appears in the alignment group alongside left, center, and right. It uses the standard Quill justify format.

---

## 3. Advanced Features

### 3.1 Value Formats (HTML vs Delta)

ERTE v6.x uses **HTML as its primary value format** (matching Vaadin RTE 2). Delta JSON is available as an optional secondary format.

#### HTML (Default)

```java
// Set and get HTML
editor.setValue("<p>Hello <strong>world</strong></p>");
String html = editor.getValue();

// Listen for HTML changes (fires on blur)
editor.addValueChangeListener(e -> {
    String newHtml = e.getValue();
});
```

#### Delta (Optional)

```java
// Set and get Delta JSON
editor.asDelta().setValue(
    "[{\"insert\":\"Hello \"},{\"insert\":\"world\",\"attributes\":{\"bold\":true}},{\"insert\":\"\\n\"}]"
);
String deltaJson = editor.asDelta().getValue();

// Listen for Delta changes (fires on blur)
editor.asDelta().addValueChangeListener(e -> {
    String newDelta = e.getValue();
});
```

**When to use Delta:**
- Setting readonly sections programmatically (readonly is a Delta attribute)
- Batch updates where Delta is faster than HTML parsing
- Precise format control (bold, italic, links, etc.)

**When to use HTML:**
- Form field binding
- Database storage
- Displaying user-visible content

**Delta structure examples:**

```json
// Plain text
[{"insert": "Hello World\n"}]

// Text with tab embed
[{"insert": "Col1"}, {"insert": {"tab": true}}, {"insert": "Col2\n"}]

// Readonly section
[{"insert": "Protected", "attributes": {"readonly": true}}, {"insert": "\n"}]

// Placeholder embed
[{"insert": {"placeholder": {"text": "Company Name"}}}, {"insert": "\n"}]
```

---

### 3.2 Extension Hooks

For power users who need to register custom Quill formats or modify editor configuration before initialization.

#### Overview

ERTE provides two hooks via a JavaScript extension connector:
- `extendQuill` -- register custom Quill formats/modules before the editor is created
- `extendEditor` -- modify the Quill editor instance after creation

#### Usage Pattern

1. Create a JS connector module:

```javascript
// Source: sampleEditorExtensionConnector.js

(function () {
    window.Vaadin ??= {};
    window.Vaadin.Flow ??= {};
    window.Vaadin.Flow.vcfEnhancedRichTextEditor ??= {};
    const ns = window.Vaadin.Flow.vcfEnhancedRichTextEditor;

    // extendQuill: register a custom highlight format
    ns.extendQuill ??= [];
    ns.extendQuill.push((Quill) => {
        const Inline = Quill.import('blots/inline');
        class HighlightBlot extends Inline {
            static blotName = 'highlight';
            static tagName = 'MARK';
        }
        Quill.register(HighlightBlot);
    });

    // extendEditor: access the editor instance after creation
    ns.extendEditor ??= [];
    ns.extendEditor.push((editor, Quill) => {
        editor.root.dataset.extendEditorCalled = 'true';
    });
}());
```

2. Load the connector from your Java view:

```java
// Source: ErteExtendOptionsTestView.java

@Route("my-view")
@JsModule("./src/sampleEditorExtensionConnector.js")
public class MyView extends VerticalLayout {
    public MyView() {
        EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
        add(editor);
    }
}
```

**Use cases:**
- Custom Quill formats (e.g., highlight, mention, footnote)
- Custom Quill modules (e.g., auto-complete, spell-check)
- Editor configuration tweaks

---

### 3.3 Internationalization (I18n)

ERTE extends Vaadin RTE 2's i18n with labels for ERTE-specific buttons and the placeholder dialog.

```java
// Source: ErteFeatureTestView.java

EnhancedRichTextEditor.EnhancedRichTextEditorI18n i18n =
    new EnhancedRichTextEditor.EnhancedRichTextEditorI18n();

// Standard RTE 2 labels (inherited)
i18n.setBold("Fett");
i18n.setItalic("Kursiv");
i18n.setUnderline("Unterstreichen");
i18n.setUndo("Ruckgangig");
i18n.setRedo("Wiederholen");

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

| Method | Default | Purpose |
|--------|---------|---------|
| `setReadonly()` | "Readonly" | Lock toolbar button tooltip |
| `setWhitespace()` | "Show whitespace" | Whitespace indicator button tooltip |
| `setPlaceholder()` | "Placeholder" | Placeholder toolbar button tooltip |
| `setPlaceholderAppearance()` | "Toggle placeholder appearance" | Appearance toggle button tooltip |
| `setPlaceholderDialogTitle()` | "Placeholders" | Dialog title |
| `setPlaceholderComboBoxLabel()` | "Select a placeholder" | Combo-box label in dialog |
| `setPlaceholderAppearanceLabel1()` | "Plain" | First appearance label in dialog |
| `setPlaceholderAppearanceLabel2()` | "Value" | Second appearance label in dialog |
| `setAlignJustify()` | "Justify" | Justify alignment button tooltip |

**Inherited RTE 2 labels:** `setUndo()`, `setRedo()`, `setBold()`, `setItalic()`, `setUnderline()`, `setStrike()`, `setColor()`, `setBackground()`, `setH1()`, `setH2()`, `setH3()`, `setSubscript()`, `setSuperscript()`, `setListOrdered()`, `setListBullet()`, `setOutdent()`, `setIndent()`, `setAlignLeft()`, `setAlignCenter()`, `setAlignRight()`, `setImage()`, `setLink()`, `setBlockquote()`, `setCodeBlock()`, `setClean()`.

---

### 3.4 Sanitization

ERTE includes a server-side HTML sanitizer that prevents XSS attacks while preserving ERTE-specific content.

**What the sanitizer preserves:**
- Standard HTML tags: `p`, `br`, `strong`, `em`, `u`, `s`, `h1`-`h3`, `img`, `a`, `ol`, `ul`, `li`, `blockquote`, `pre`
- ERTE CSS classes: `ql-readonly`, `ql-tab`, `ql-soft-break`, `ql-placeholder`, `ql-nbsp`
- Standard Quill classes: `ql-align-*`, `ql-indent-*`
- `contenteditable="false"` on spans (for readonly sections)
- Safe CSS properties (color, font-size, text-align, etc.)
- Safe image data URLs (PNG, JPEG, GIF, WebP, BMP, ICO)

**What the sanitizer strips:**
- `<script>`, `<iframe>`, `<object>`, `<embed>` tags
- JavaScript event handlers (`onclick`, `onerror`, etc.)
- Dangerous CSS functions (except `rgb()`, `rgba()`, `hsl()`, `hsla()`, `calc()`)
- `@import` directives in CSS
- SVG data URLs (can contain scripts)
- Unknown/dangerous CSS classes
- `contenteditable="true"` (only `"false"` is preserved)

> **Note:** For full security details, see `SECURITY.md` in the repository root.

---

## 4. Best Practices

### 4.1 Security

- Always validate placeholder content in `PlaceholderBeforeInsertEvent` before calling `event.insert()`
- Use readonly sections for protected content that users should not modify
- Do not insert unsanitized HTML from external sources via `setValue()` -- the sanitizer handles output, but input validation is your responsibility
- Review `SECURITY.md` for known XSS vectors and mitigations

### 4.2 Performance

- Use Delta format for batch updates (faster than HTML parsing)
- Limit tabstop count -- each tab triggers width calculation
- Avoid excessive placeholder count (100+ placeholders in a single editor may impact performance)
- Value change listeners fire on blur by default -- use `ValueChangeMode.EAGER` only when needed

### 4.3 Accessibility

- Provide `aria-label` or `title` on custom toolbar buttons for screen reader support
- Use `addToolbarFocusShortcut()` to enable keyboard access to the toolbar
- ERTE toolbar buttons support keyboard navigation: Tab/Shift+Tab moves focus in/out of the toolbar, Left/Right arrow keys navigate between buttons within the toolbar, and Escape returns focus to the editor content
- Test with screen readers to verify custom toolbar components are announced correctly

### 4.4 Testing

The `enhanced-rich-text-editor-it/` module contains 306 Playwright tests covering all ERTE features. For setup, commands, test architecture, and debugging tips, see [CONTRIBUTING.md — Test Architecture](CONTRIBUTING.md#test-architecture).

---

## 5. Troubleshooting

### 5.1 Common Issues

**Toolbar button not visible**
- Check `setToolbarButtonsVisibility()` configuration
- Verify the button is not in a fully hidden group (all buttons in group hidden = group hidden)
- Ensure no keyboard shortcut conflicts

**Placeholder not inserting**
- Verify `PlaceholderBeforeInsertEvent` listener calls `event.insert()`
- Check that the placeholder is in the master list set via `setPlaceholders()`
- Confirm placeholder text is not null

**Tab width calculation wrong**
- Verify tabstop positions are in pixels
- Check tab direction (LEFT, RIGHT, MIDDLE)
- Tabs on wrapped lines use fixed width (expected behavior)

**Value change listener not firing**
- `addValueChangeListener()` fires on **blur**, not on every keystroke
- For immediate updates, use `ValueChangeMode.EAGER` or read Delta directly via client-side JS
- Check if the editor is disabled (`setEnabled(false)` prevents events)

**Programmatic focus not working**
- V25's `focus()` method delegates to Quill's internal focus mechanism, which is reliable
- If focus doesn't work, ensure the editor is attached to the DOM and enabled
- The inherited `HTMLElement.focus()` only focuses the web component element -- ERTE overrides this to properly focus the Quill editor

**I18n labels not updating**
- Call `setI18n()` with a complete `EnhancedRichTextEditorI18n` object
- ERTE-specific labels (readonly, whitespace, placeholder, etc.) are separate from inherited RTE labels
- Both can be set on the same `EnhancedRichTextEditorI18n` instance via fluent chaining

### 5.2 Known Limitations

These limitations are inherent to Quill 2 and documented in the test suite:

| Issue | Description | Workaround |
|-------|-------------|------------|
| Readonly undo | Undo removes readonly formatting | Re-apply readonly after undo |
| Tab format toggles | Bold/Italic at tab embed boundary may not work | Use keyboard shortcuts instead of clicking at boundary |
| Placeholder copy-paste | Embed does not survive HTML-to-Delta clipboard roundtrip | Use programmatic insertion instead of paste |
| Placeholder undo | Quill history does not restore removed embed blots | Manual re-insertion after undo |

### 5.3 Getting Help

- **Upgrade Guide:** [docs/UPGRADE_GUIDE.md](UPGRADE_GUIDE.md) for migration from v5.x
- **API Reference:** [docs/API_REFERENCE.md](API_REFERENCE.md) for complete API surface
- **Configuration:** [docs/CONFIGURATION.md](CONFIGURATION.md) for toolbar, i18n, theming patterns
- **Test examples:** See `enhanced-rich-text-editor-it/src/main/java/com/vaadin/componentfactory/` for working test views
- **Issue tracker:** GitHub issues on the repository
- **Commercial support:** [vaadin.com](https://vaadin.com)
