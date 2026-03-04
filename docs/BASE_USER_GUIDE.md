# Enhanced Rich Text Editor -- User Guide

This guide walks you through all ERTE v6.x features for Vaadin 25 — from basic setup to advanced customization. Each section includes working code examples you can drop into your project.

If you're upgrading from v5.x, start with the [Upgrade Guide](BASE_UPGRADE_GUIDE.md) first.

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

The toolbar supports adding custom components to named slots, controlling button visibility, binding keyboard shortcuts, and replacing button icons.

#### Adding Components

ERTE provides several `ToolbarSlot` positions: `START`/`END`, `BEFORE_GROUP_*/AFTER_GROUP_*` (two per button-group), and `GROUP_CUSTOM`. See the `ToolbarSlot` enum Javadoc for the complete list of positions.

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
> **Styling note:** All components added via `addToolbarComponents()` automatically receive `part="toolbar-custom-component"`. This enables consistent styling through ERTE's shadow DOM, including hover, focus, and active/pressed states for buttons. See [Section 2.10.2](#2102-erte-shadow-parts) for styling details.

**Removing components:**

```java
editor.removeToolbarComponent(ToolbarSlot.START, startBtn);
// or by ID:
editor.removeToolbarComponent(ToolbarSlot.START, "slot-start-btn");
```

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

When all buttons in a group are hidden, the group container auto-hides. See the `ToolbarButton` enum Javadoc for the complete list.

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


#### Ready-to-use toolbar components

ERTE provides ready-to-use toolbar components for integrating custom functionality.

##### Toolbar Switch 

The `ToolbarSwitch` is a toggle button for toolbar components:

```java
ToolbarSwitch toolbarSwitch = new ToolbarSwitch("SW");
toolbarSwitch.addActiveChangedListener(e ->
    System.out.println("Switch active: " + e.isActive()));
editor.addCustomToolbarComponents(toolbarSwitch);
```

##### Toolbar Popovers

`ToolbarPopover` anchors a popover to a `ToolbarSwitch` and syncs open/close state. Like Vaadin's `Popover` and `Dialog`, toolbar overlays self-attach to the page — no explicit `add()` call needed:

```java
ToolbarSwitch colorSwitch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
TextField colorField = new TextField("Color");
Button applyBtn = new Button("Apply");

ToolbarPopover popover = ToolbarPopover.vertical(colorSwitch, colorField, applyBtn);
popover.setFocusOnOpenTarget(colorField);

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, colorSwitch);
```

Besides the constructor, there are also factory methods for common layouts: `vertical(switch, components...)`, `horizontal(switch, components...)`.

##### Toolbar Select Popups

`ToolbarSelectPopup` opens a context menu anchored to a `ToolbarSwitch`:

```java
ToolbarSwitch insertSwitch = new ToolbarSwitch(VaadinIcon.PLUS);
ToolbarSelectPopup menu = new ToolbarSelectPopup(insertSwitch);
menu.addItem("Horizontal Rule", e -> { /* insert HR */ });
menu.addItem("Page Break", e -> { /* insert page break */ });
menu.addSeparator();
menu.addItem("Special Character...", e -> { /* open dialog */ });

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, insertSwitch);
```

It uses standard Vaadin `ContextMenu` API.

##### Toolbar Dialogs

`ToolbarDialog` opens a non-modal dialog controlled by a `ToolbarSwitch`. The dialog comes with default configuration (non-modal, resizable, draggable, closes on ESC):

```java
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
ToolbarDialog dialog = new ToolbarDialog(settingsSwitch, true); // openAtSwitch
dialog.add(new Checkbox("Show rulers"), new Checkbox("Show whitespace"));
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
```

Besides the constructor, there are also factory methods for common layouts: `vertical(switch, components...)`, `horizontal(switch, components...)`.

---

### 2.2 Placeholders

Placeholders are embedded tokens for mail merge, templates, and dynamic fields. They support full lifecycle events, formatting (normal and alternative), and keyboard insertion.

#### Configuration

Placeholder text uses the format `"ID=Display Text"` — the part before the `=` is the placeholder ID (e.g., `N-1`) used for programmatic access, and the part after is the display text shown in the editor (e.g., `Company Name`).

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

**Alt appearance pattern** -- regex that controls which part of a placeholder's text to show in alt mode. When a placeholder's text matches, the matched substring becomes the "alt text" displayed with `altFormat` styling. This regex matches everything after the `=` sign — so `N-1=Company Name` shows only `Company Name` in alt mode:

```java
// Define which substring to extract for alt display
editor.setPlaceholderAltAppearancePattern("(?<=\\=).*$");

// Switch ALL placeholders to alt appearance at once
editor.setPlaceholderAltAppearance(true);
```

The pattern and the toggle work together: the **pattern** defines *what* to extract as alt text (and makes the toolbar toggle button visible), while `setPlaceholderAltAppearance(true)` activates alt mode (showing the extracted text with `altFormat` styling). Without a pattern, the toolbar toggle button is hidden — but `setPlaceholderAltAppearance(true)` still works programmatically (placeholders without a regex match are hidden in alt mode).

#### Inserting Placeholders

Users insert via: **Toolbar button** (dialog with combo-box), **Keyboard shortcut** (Ctrl+P / Cmd+P), or **Programmatic insertion** (`PlaceholderButtonClickedEvent.insert()`).

#### Event Lifecycle

**Default behavior** 

ERTE handles placeholders automatically: clicking the toolbar button (or pressing Ctrl+P / Cmd+P) opens a dialog where the user selects a placeholder, and pressing Delete/Backspace on a placeholder removes it. No Java code needed for basic usage.

**Customize Behavior** 

If you need to customize the handling of placeholders, you can do that via lifecycle events.

There are two types of events:

- **Gate events** (`ButtonClicked`, `BeforeInsert`, `BeforeRemove`): These *intercept* the default action. When you register a listener, the default action is suppressed — your listener **must** call `event.insert()` or `event.remove()` to proceed, or do nothing to cancel.
- **Notification events** (`Inserted`, `Removed`, `Selected`, `Leave`, `AppearanceChanged`): These fire *after* the action. Use them for logging, UI updates, etc. They don't affect the operation.

**Insert flow:**

```
User clicks toolbar button / Ctrl+P
  → ButtonClicked (gate) ─── listener registered? ──→ YES: dialog suppressed,
  │                                                          listener decides
  │                                                    NO:  built-in dialog opens
  ↓
User confirms in dialog (or listener calls event.insert())
  → BeforeInsert (gate) ─── listener registered? ──→ YES: must call event.insert()
  │                                                    NO:  insert proceeds automatically
  ↓
Placeholder appears in editor
  → Inserted (notification)
```

**Remove flow:**

```
User presses Delete/Backspace on a placeholder
  → BeforeRemove (gate) ─── listener registered? ──→ YES: must call event.remove()
  │                                                    NO:  removal proceeds automatically
  ↓
Placeholder removed from editor
  → Removed (notification)
```

##### Typical usage: just log insertions (no dialog override)

```java
// No ButtonClicked listener → dialog works normally.
// No BeforeInsert listener → insertion proceeds automatically.
// Just track what was inserted:
editor.addPlaceholderInsertedListener(event ->
    event.getPlaceholders().forEach(p ->
        log.info("Inserted: {}", p.getText())));
```

##### Skip the dialog and insert directly

Registering a `ButtonClickedListener` suppresses the built-in dialog. Your listener takes over:

```java
// Always insert the first placeholder, no dialog
editor.addPlaceholderButtonClickedListener(event ->
    event.insert(placeholders.get(0)));
```

##### Validate before insert, protect certain placeholders from deletion

```java
// Gate: only allow insert if quota not exceeded
editor.addPlaceholderBeforeInsertListener(event -> {
    if (currentCount < MAX_PLACEHOLDERS) {
        event.insert();  // REQUIRED — without this, nothing happens
    } else {
        Notification.show("Maximum placeholders reached");
    }
});

// Gate: prevent deletion of required placeholders
editor.addPlaceholderBeforeRemoveListener(event -> {
    boolean anyProtected = event.getPlaceholders().stream()
            .anyMatch(p -> p.getText().startsWith("REQUIRED-"));
    if (!anyProtected) {
        event.remove();  // REQUIRED — without this, nothing happens
    } else {
        Notification.show("Cannot remove required placeholders");
    }
});
```

##### Selection tracking (show details panel)

```java
editor.addPlaceholderSelectedListener(event ->
    detailsPanel.show(event.getPlaceholders().get(0)));

editor.addPlaceholderLeaveListener(event ->
    detailsPanel.hide());
```

##### Appearance change tracking

```java
editor.addPlaceholderAppearanceChangedListener(event -> {
    boolean isAlt = event.getAltAppearance();
    statusLabel.setText(isAlt ? "Showing values" : "Showing names");
});
```

#### Formatting (format / altFormat)

Each placeholder has two visual appearances that you can switch between:

- **`format`** — the default appearance (e.g., italic placeholder name)
- **`altFormat`** — the alternative appearance (e.g., bold resolved value)

Both are maps of Quill inline format attributes applied to the placeholder token in the editor. The user switches between them via the toolbar toggle button, `setPlaceholderAltAppearance(true/false)`, or the `placeholderAltAppearancePattern` regex.

**Typical use case — mail merge preview:** Show field names in italic by default, switch to bold resolved values for preview:

```java
Placeholder company = new Placeholder("N-1=Acme Corp");
company.getFormat().put("italic", true);       // name appearance: italic
company.getAltFormat().put("bold", true);       // value appearance: bold
company.getAltFormat().put("color", "#006600"); // value appearance: green

Placeholder date = new Placeholder("D-1=2024-01-15");
date.getFormat().put("italic", true);
date.getAltFormat().put("font", "monospace");   // value appearance: monospace

editor.setPlaceholders(List.of(company, date));

// Auto-switch to altFormat when text after "=" is present
editor.setPlaceholderAltAppearancePattern("(?<=\\=).*$");
```

When `altAppearance` is off, placeholders render with `format` (italic field names). When toggled on, they render with `altFormat` (bold/green values, monospace dates).

**Supported format attributes:**

| Attribute | Type | Example |
|-----------|------|---------|
| `bold` | boolean | `format.put("bold", true)` |
| `italic` | boolean | `format.put("italic", true)` |
| `color` | String | `format.put("color", "red")` |
| `background` | String | `format.put("background", "#ffcc00")` |
| `font` | String | `format.put("font", "monospace")` |
| `link` | String | `format.put("link", "https://example.com")` |
| `script` | String | `format.put("script", "super")` |

---

### 2.3 Tabstops and Rulers

Tabstops provide document-style columnar alignment for invoices, forms, or structured layouts. Pressing Tab inserts an embedded tab character whose width is calculated from the defined tabstop positions.

#### Tabstops

You can define tabstops programmatically (see below) or interactively — clicking the horizontal ruler cycles through directions (LEFT → RIGHT → MIDDLE → remove) at the clicked position.

Define tabstops as a list of positions (in CSS pixels from the left edge of the editor's content area, inside any padding) with an alignment direction:

```java
editor.setTabStops(List.of(
    new TabStop(TabStop.Direction.LEFT, 150),
    new TabStop(TabStop.Direction.RIGHT, 350),
    new TabStop(TabStop.Direction.MIDDLE, 550)
));

// Read current tabstops
List<TabStop> current = editor.getTabStops();
```

**How alignment works:** The direction controls how text *after* the tab aligns relative to the tabstop position — like in a word processor:

- **`LEFT`** — text starts at the tabstop and flows to the right (most common)
- **`RIGHT`** — text ends at the tabstop, growing to the left (useful for right-aligned numbers)
- **`MIDDLE`** — text is centered on the tabstop position

#### Rulers

Rulers provide a visual reference for tabstop positions. The horizontal ruler shows pixel positions and lets users click to cycle through directions (LEFT → RIGHT → MIDDLE → remove). The vertical ruler shows a line-height reference.

Hiding rulers does not disable tabstops — Tab key insertion and alignment still work, only the visual ruler bar is hidden.

```java
editor.setNoRulers(true);   // Hide rulers (tabstops still work)
editor.setNoRulers(false);  // Show rulers (default)
```

---

### 2.4 Readonly Sections

Readonly sections let you protect specific parts of the text from editing — useful for legal clauses, template boilerplate, or any content users should see but not modify. The rest of the editor remains fully editable.

**Interactively:** Select text and click the lock icon in the toolbar to toggle readonly on/off.

**Programmatically:** Use Delta attributes to set readonly content from the server (see [Section 3.1](#31-value-formats-html-vs-delta) for details on Delta format):

```java
editor.asDelta().setValue("["
    + "{\"insert\":\"Editable text. \"},"
    + "{\"insert\":\"This part is protected.\","
    +   "\"attributes\":{\"readonly\":true}},"
    + "{\"insert\":\" Editable again.\\n\"}"
    + "]");
```

Readonly sections are visually marked with a gray background and are delete-protected — if a user tries to delete into a readonly span, the change is automatically reverted. You can still apply formatting (bold, italic, etc.) within a readonly span.

> **Whole-editor readonly** 
> 
> If you want to set the whole editor readonly, you can use the normal readonly API that all Vaadin input fields provide:
> ```java
> editor.setReadOnly(true);   // entire editor locked
> editor.setReadOnly(false);  // back to normal
> ```

---

### 2.5 Whitespace Indicators

Whitespace indicators overlay visual markers on invisible characters — tabs, soft-breaks, paragraph ends, and non-breaking spaces — so you can see the exact structure of the document:

- → Tab
- ↵ Soft-break
- ¶ Paragraph end
- · Non-breaking space

```java
editor.setShowWhitespace(true);
boolean showing = editor.isShowWhitespace();
```

Users can also toggle this from the toolbar (the whitespace button).

> **Note:** Regular whitespace does not have indicators. Due to how HTML handles whitespace natively, adding visual markers for regular spaces would cause performance issues and could break browser rendering.

---

### 2.6 Non-Breaking Space

Press **Shift+Space** to insert a non-breaking space. Unlike regular spaces, it won't be collapsed by the browser, prevents automatic line breaks at that position, and is preserved in the HTML output. This is a built-in keyboard behavior — no Java API needed.

---

### 2.7 Soft-Break and Tab Copying

**Soft-Break:** Press **Shift+Enter** to insert a line break *within* a paragraph (no new `<p>` tag). This is useful whenever you need a visual line break without starting a new block — addresses, poetry, multi-line labels, etc.

**Tab Copying:** When you press Shift+Enter on a line that starts with tabs, the new line automatically gets the same leading tabs. This keeps your columnar alignment intact across multiple lines without re-pressing Tab on each new line. The number of copied tabs is limited by the number of defined tabstops — additional Tab presses beyond the last tabstop position have no effect. Tab copying only applies to soft-break lines (Shift+Enter) — it does not occur on hard paragraph breaks (Enter) or when text auto-wraps at the editor border.

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

Justify distributes text evenly across the full width of the line, creating straight edges on both left and right sides. ERTE adds a justify alignment button to the toolbar, next to the existing left/center/right buttons. It's visible by default — hide it if you don't need it:

```java
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.ALIGN_JUSTIFY, false
));
```

---

### 2.10 Styling and Theming

ERTE builds on the Vaadin Lumo theme and adds its own CSS custom properties, shadow parts, and content classes. You have three layers to work with:

- **CSS custom properties** (`--vaadin-erte-*`) — control colors, sizes, and spacing of ERTE-specific elements
- **Shadow parts** — target toolbar buttons, rulers, and custom components from external CSS
- **Content classes** — style editor content elements (tabs, placeholders, readonly spans)

For the standard RTE properties (`--vaadin-rich-text-editor-*`), see the [Vaadin RTE Styling docs](https://vaadin.com/docs/v25/components/rich-text-editor/styling).

> Please note that 6.0 does not yet support the new Aura theme.

#### 2.10.1 ERTE Custom Properties

ERTE provides CSS custom properties (`--vaadin-erte-*`) that you can override on the host element to customize readonly sections, placeholders, whitespace indicators, and rulers. For example:

```css
/* global for all ERTE instances */
html {
    --vaadin-erte-readonly-background: lightgray;
    --vaadin-erte-placeholder-background: #e0f0ff;
}

/* only for instances with a certain CSS class */
vcf-enhanced-rich-text-editor.some-css-class {
    --vaadin-erte-readonly-background: yellow;
    --vaadin-erte-placeholder-background: #e0f0ff;
}
```

**Readonly sections:**

| Property | Description | Default |
|----------|-------------|---------|
| `--vaadin-erte-readonly-color` | Text color | `--vaadin-text-color-secondary` |
| `--vaadin-erte-readonly-background` | Background color | `--vaadin-background-container` |
| `--vaadin-erte-readonly-border-color` | Outline color | `--vaadin-border-color-secondary` |
| `--vaadin-erte-readonly-border-width` | Outline width | `1px` |
| `--vaadin-erte-readonly-border-radius` | Corner radius | `--lumo-border-radius-s` |
| `--vaadin-erte-readonly-padding` | Inline padding | `--vaadin-padding-xs / 2` |

**Placeholders:**

| Property | Description | Default |
|----------|-------------|---------|
| `--vaadin-erte-placeholder-color` | Text color | `inherit` |
| `--vaadin-erte-placeholder-background` | Background color | `--lumo-primary-color-10pct` |
| `--vaadin-erte-placeholder-border-color` | Outline color | `transparent` |
| `--vaadin-erte-placeholder-border-width` | Outline width | `0` |
| `--vaadin-erte-placeholder-border-radius` | Corner radius | `--lumo-border-radius-s` |
| `--vaadin-erte-placeholder-padding` | Inline padding | `--vaadin-padding-xs / 2` |

**Whitespace indicators:**

| Property | Description | Default |
|----------|-------------|---------|
| `--vaadin-erte-whitespace-indicator-color` | Color of tab/soft-break/NBSP markers | `--lumo-contrast-40pct` |
| `--vaadin-erte-whitespace-paragraph-indicator-color` | Color of paragraph markers | `--lumo-contrast-30pct` |
| `--vaadin-erte-whitespace-indicator-spacing` | Spacing around indicators | `--vaadin-padding-xs / 2` |

**Rulers:**

| Property | Description | Default |
|----------|-------------|---------|
| `--vaadin-erte-ruler-height` | Height of the horizontal ruler | `0.9375rem` |
| `--vaadin-erte-ruler-border-color` | Ruler border color | `--vaadin-border-color` |
| `--vaadin-erte-ruler-background` | Horizontal ruler background (tick-mark image) | base64 PNG |
| `--vaadin-erte-ruler-marker-size` | Size of tabstop direction markers | `0.9375rem` |
| `--vaadin-erte-ruler-marker-color` | Color of tabstop direction markers | `inherit` |
| `--vaadin-erte-ruler-vertical-width` | Width of the vertical ruler | `0.9375rem` |
| `--vaadin-erte-ruler-vertical-background` | Vertical ruler background (line-height image) | base64 PNG |

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

These CSS selectors let you style your custom toolbar buttons in different states (hover, active, disabled). The `[on]` attribute is present when a toggle button is active, and `part~=` matches elements whose `part` attribute contains that value.

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

These classes are applied to elements inside the editor content area (`.ql-editor`). You can use them in your own CSS to style specific content types — for example, to highlight all placeholders or change the background of readonly spans.

| Class | Element | Purpose |
|-------|---------|---------|
| `.ql-tab` | `<span>` | Tab embed (calculated width) |
| `.ql-placeholder` | `<span>` | Placeholder embed |
| `.ql-readonly` | `<span>` | Readonly section (`contenteditable="false"`) |
| `.ql-soft-break` | `<span>` | Soft-break embed (contains `<br>`) |
| `.ql-nbsp` | `<span>` | Non-breaking space |

Standard Quill classes (`ql-align-*`, `ql-indent-*`) are provided by the base RTE 2 component.

---

### 2.11 Built-in Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Tab | Insert tab embed (if tabstops configured) |
| Shift+Enter | Insert soft-break |
| Shift+Space | Insert non-breaking space |
| Ctrl+P / Cmd+P | Open placeholder dialog (if placeholders configured) |

> **Note:** Some browsers can intercept Ctrl+P to open the print dialog. If that happens, use the toolbar button as a reliable alternative.

---

## 3. Advanced Features

### 3.1 Value Formats (HTML vs Delta)

With Vaadin 25, the Rich Text Editor uses **HTML as its primary format**. `getValue()` returns HTML, `setValue()` expects HTML. This is the format you'll use for storage and form binding.

```java
// HTML — the default and recommended format
editor.setValue("<p>Hello <strong>world</strong></p>");
String html = editor.getValue();
```

If you need to work with Quill's Delta JSON — for example, to set readonly attributes or insert placeholders programmatically — use the `asDelta()` wrapper. Both `setValue()` and `getValue()` work with JSON strings:

```java
// Delta — for structured content manipulation (JSON string)
editor.asDelta().setValue("[{\"insert\":\"Hello \"},{\"insert\":\"world\",\"attributes\":{\"bold\":true}}]");
String deltaJson = editor.asDelta().getValue();
```

---

### 3.2 Internationalization (I18n)

All toolbar button labels and dialog texts can be localized. ERTE extends the standard RTE 2 i18n object with labels for its own buttons (readonly, whitespace, placeholder, etc.) and the placeholder dialog.

```java
EnhancedRichTextEditor.EnhancedRichTextEditorI18n i18n =
    new EnhancedRichTextEditor.EnhancedRichTextEditorI18n();

// Standard RTE 2 labels
i18n.setBold("Fett");
i18n.setItalic("Kursiv");
i18n.setUndo("Rückgängig");

// ERTE-specific labels
i18n.setReadonly("Schreibschutz");
i18n.setWhitespace("Leerzeichen anzeigen");
i18n.setPlaceholder("Platzhalter");
i18n.setPlaceholderAppearance("Platzhalter-Darstellung");
i18n.setPlaceholderDialogTitle("Platzhalter");
i18n.setPlaceholderComboBoxLabel("Platzhalter wählen");
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

For the standard RTE 2 labels (bold, italic, headings, etc.), see the [Vaadin RTE i18n docs](https://vaadin.com/docs/v25/components/rich-text-editor#internationalization-i18n).

---

### 3.3 Sanitization

ERTE includes a server-side HTML sanitizer that runs automatically whenever content is received from the client. No configuration needed — it protects against XSS while preserving all ERTE-specific content.

#### What gets preserved

- **HTML tags:** Standard formatting (`<p>`, `<strong>`, `<em>`, `<a>`, `<ul>`, `<ol>`, `<li>`, `<h1>`–`<h3>`, `<blockquote>`, `<img>`, etc.) plus table elements (`<table>`, `<tr>`, `<td>`, `<th>`, `<colgroup>`, `<col>`)
- **ERTE classes:** `ql-readonly`, `ql-tab`, `ql-soft-break`, `ql-placeholder`, `ql-nbsp`
- **Quill classes:** `ql-align-*`, `ql-indent-*`
- **Safe CSS properties:** Text styling (color, font, text-align, etc.), spacing (margin, padding), borders, sizing, and layout properties. Only safe CSS functions are allowed (`rgb()`, `rgba()`, `hsl()`, `hsla()`, `calc()`)
- **Image sources:** `http:`, `https:`, and `data:` URLs (restricted to image MIME types — SVG is excluded because it can contain scripts)

#### What gets stripped

- `<script>` tags and event handler attributes (`onclick`, `onerror`, etc.)
- Unknown CSS classes (anything not in the lists above)
- Dangerous CSS: `@import`, `expression()`, `url()`, `behavior()`, and other non-whitelisted CSS functions
- `data:` URLs with non-image MIME types (e.g., `data:text/html`)
- `contenteditable="true"` and `contenteditable=""` (only `"false"` is preserved, for readonly spans)

#### Extending the allowlist

If you use custom CSS classes in your content (e.g., from the Tables addon or your own blots), you can register them with the sanitizer so they don't get stripped:

```java
// Register custom classes
editor.addAllowedHtmlClasses("my-highlight", "invoice-header");

// Remove them later if needed
editor.removeAllowedHtmlClasses("my-highlight");

// See what's currently registered
Set<String> registered = editor.getAllowedHtmlClasses();
```

Class names must match `[A-Za-z][A-Za-z0-9-]*` (start with a letter, contain only letters, digits, and hyphens) and must not start with `ql-` (reserved for Quill internals).

---

## 4. Getting Help

Further resources:

- **Upgrade from v5.x:** [Upgrade Guide](BASE_UPGRADE_GUIDE.md)
- **Extension hooks and custom blots:** [EXTENDING.md](dev/EXTENDING.md) — for building your own blots, registering Quill extensions, or hooking into the editor lifecycle
- **Working examples:** Run the demo module (`enhanced-rich-text-editor-demo/`) to see all features in action — see the [Developer Guide](dev/DEVELOPER_GUIDE.md) for setup instructions
- **Issues and questions:** [GitHub](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/issues)
