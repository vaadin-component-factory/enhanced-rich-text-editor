# Enhanced Rich Text Editor -- API Reference

Complete API reference for the Enhanced Rich Text Editor (ERTE) v6.x. This document covers all public classes, methods, enums, and events.

**Audience:** Java developers integrating ERTE programmatically.

**Related docs:** [User Guide](USER_GUIDE.md) for feature documentation and examples, [Upgrade Guide](UPGRADE_GUIDE.md) for migration from v5.x.

---

## Table of Contents

- [1. EnhancedRichTextEditor](#1-enhancedrichtexteditor)
- [2. Placeholder](#2-placeholder)
- [3. TabStop](#3-tabstop)
- [4. ToolbarButton (Enum)](#4-toolbarbutton-enum)
- [5. ToolbarSlot (Enum)](#5-toolbarslot-enum)
- [6. ToolbarSwitch](#6-toolbarswitch)
- [7. ToolbarPopover](#7-toolbarpopover)
- [8. ToolbarSelectPopup](#8-toolbarselectpopup)
- [9. ToolbarDialog](#9-toolbardialog)
- [10. EnhancedRichTextEditorI18n](#10-enhancedrichtexteditori18n)
- [11. Events](#11-events)
- [12. Inherited from RichTextEditor](#12-inherited-from-richtexteditor)
- [13. CSS Shadow Parts](#13-css-shadow-parts)
- [14. CSS Content Classes](#14-css-content-classes)
- [15. ERTE Custom Properties](#15-erte-custom-properties)
- [16. Sanitization](#16-sanitization)
- [17. Built-in Keyboard Shortcuts](#17-built-in-keyboard-shortcuts)
- [18. Value Format Guide](#18-value-format-guide)

---

## 1. EnhancedRichTextEditor

```java
package com.vaadin.componentfactory;

@Tag("vcf-enhanced-rich-text-editor")
@JsModule("./vcf-enhanced-rich-text-editor.js")
public class EnhancedRichTextEditor extends RteExtensionBase
```

Extends `RteExtensionBase` (which extends Vaadin's `RichTextEditor`). All ERTE-specific logic lives in this class.

### Toolbar Component Management

#### addToolbarComponents

```java
public void addToolbarComponents(ToolbarSlot toolbarSlot, Component... components)
```

Adds components to the given toolbar slot (appended to the end of the slot).

| Parameter | Type | Description |
|-----------|------|-------------|
| `toolbarSlot` | `ToolbarSlot` | The slot position in the toolbar |
| `components` | `Component...` | One or more Vaadin components to add |

**Throws:** `NullPointerException` if any component is null.

---

#### addToolbarComponentsAtIndex

```java
public void addToolbarComponentsAtIndex(ToolbarSlot toolbarSlot, int index, Component... components)
```

Adds components to the given toolbar slot at the specified index.

| Parameter | Type | Description |
|-----------|------|-------------|
| `toolbarSlot` | `ToolbarSlot` | The slot position in the toolbar |
| `index` | `int` | Zero-based insertion index within the slot |
| `components` | `Component...` | One or more Vaadin components to add |

---

#### getToolbarComponent

```java
public <T extends Component> T getToolbarComponent(ToolbarSlot toolbarSlot, String id)
```

Returns a toolbar component with the given ID from the specified slot.

| Parameter | Type | Description |
|-----------|------|-------------|
| `toolbarSlot` | `ToolbarSlot` | The slot to search in |
| `id` | `String` | The component's ID |

**Returns:** The component cast to type `T`, or `null` if not found.

---

#### removeToolbarComponent (by ID)

```java
public void removeToolbarComponent(ToolbarSlot toolbarSlot, String id)
```

Removes a toolbar component by its ID from the specified slot.

---

#### removeToolbarComponent (by reference)

```java
public void removeToolbarComponent(ToolbarSlot toolbarSlot, Component component)
```

Removes a toolbar component by reference from the specified slot.

---

#### addCustomToolbarComponents

```java
public void addCustomToolbarComponents(Component... components)
```

Convenience method: adds components to `ToolbarSlot.GROUP_CUSTOM`.

---

#### addCustomToolbarComponentsAtIndex

```java
public void addCustomToolbarComponentsAtIndex(int index, Component... components)
```

Convenience method: adds components to `ToolbarSlot.GROUP_CUSTOM` at the given index.

---

#### replaceStandardButtonIcon

```java
public void replaceStandardButtonIcon(Icon icon, String iconSlotName)
```

Replaces a standard toolbar button icon via its named slot.

| Parameter | Type | Description |
|-----------|------|-------------|
| `icon` | `Icon` | The replacement icon |
| `iconSlotName` | `String` | The slot name of the button (e.g., `"undo"`, `"bold"`) |

---

#### replaceStandardToolbarButtonIcon

```java
public void replaceStandardToolbarButtonIcon(ToolbarButton button, Icon icon)
```

Replaces a standard toolbar button's icon via its enum constant. Pass `null` as `icon` to clear a previously set icon and restore the default.

| Parameter | Type | Description |
|-----------|------|-------------|
| `button` | `ToolbarButton` | The toolbar button to modify (not null) |
| `icon` | `Icon` | The replacement icon, or `null` to restore default |

**Throws:** `NullPointerException` if `button` is null.

**Example:**
```java
editor.replaceStandardToolbarButtonIcon(ToolbarButton.BOLD, new Icon(VaadinIcon.STAR));
editor.replaceStandardToolbarButtonIcon(ToolbarButton.BOLD, null); // restore
```

---

### Toolbar Button Visibility

#### setToolbarButtonsVisibility

```java
public void setToolbarButtonsVisibility(Map<ToolbarButton, Boolean> visibility)
```

Shows or hides individual toolbar buttons. Pass `false` for a button to hide it; `true` (or omit) to show it. Groups whose all buttons are hidden are auto-hidden. Pass `null` to reset all buttons to visible.

| Parameter | Type | Description |
|-----------|------|-------------|
| `visibility` | `Map<ToolbarButton, Boolean>` | Visibility map, or `null` to reset |

---

#### getToolbarButtonsVisibility

```java
public Map<ToolbarButton, Boolean> getToolbarButtonsVisibility()
```

**Returns:** The current visibility map, or `null` if no visibility has been set.

---

### Keyboard Shortcuts

#### addStandardToolbarButtonShortcut

```java
public void addStandardToolbarButtonShortcut(
    ToolbarButton toolbarButton, String key,
    boolean shortKey, boolean shiftKey, boolean altKey)
```

Binds a keyboard shortcut to a standard toolbar button. When pressed, the button is clicked.

| Parameter | Type | Description |
|-----------|------|-------------|
| `toolbarButton` | `ToolbarButton` | The toolbar button to trigger |
| `key` | `String` | Quill 2 key name (e.g., `"F9"`, `"b"`, `"Enter"`) |
| `shortKey` | `boolean` | `true` for Ctrl (Win/Linux) or Cmd (Mac) |
| `shiftKey` | `boolean` | `true` for Shift modifier |
| `altKey` | `boolean` | `true` for Alt modifier |

---

#### addToolbarFocusShortcut

```java
public void addToolbarFocusShortcut(
    String key, boolean shortKey, boolean shiftKey, boolean altKey)
```

Binds a keyboard shortcut that moves focus from the editor to the toolbar. The first visible toolbar button receives focus.

| Parameter | Type | Description |
|-----------|------|-------------|
| `key` | `String` | Quill 2 key name (e.g., `"F10"`) |
| `shortKey` | `boolean` | `true` for Ctrl (Win/Linux) or Cmd (Mac) |
| `shiftKey` | `boolean` | `true` for Shift modifier |
| `altKey` | `boolean` | `true` for Alt modifier |

---

### Whitespace Indicators

#### setShowWhitespace

```java
public void setShowWhitespace(boolean show)
```

Sets whether whitespace indicators are shown in the editor. When enabled, special characters are displayed for tabs, soft-breaks, paragraph ends, and auto-wrap points.

---

#### isShowWhitespace

```java
public boolean isShowWhitespace()
```

**Returns:** `true` if whitespace indicators are currently shown.

---

### Tabstops and Rulers

#### setTabStops

```java
public void setTabStops(List<TabStop> tabStops)
```

Sets tabstop positions and alignments on the ruler.

| Parameter | Type | Description |
|-----------|------|-------------|
| `tabStops` | `List<TabStop>` | The list of tab stops to set |

---

#### getTabStops

```java
public List<TabStop> getTabStops()
```

**Returns:** The current tabstop configuration, never null (empty list if none set).

---

#### setNoRulers

```java
public void setNoRulers(boolean noRulers)
```

When `true`, the rulers are not visible. Tabstops still function -- users can still press Tab to insert tabs.

---

#### isNoRulers

```java
public boolean isNoRulers()
```

**Returns:** `true` if rulers are hidden.

---

### Placeholders -- Configuration

#### setPlaceholders

```java
public void setPlaceholders(List<Placeholder> placeholders)
```

Sets the master list of available placeholders. These are shown in the placeholder dialog and used for event lookup.

---

#### getPlaceholders

```java
public List<Placeholder> getPlaceholders()
```

**Returns:** An unmodifiable copy of the current placeholder list, never null.

---

#### setPlaceholderTags

```java
public void setPlaceholderTags(String start, String end)
```

Sets the start and end tags displayed around placeholder text.

| Parameter | Type | Description |
|-----------|------|-------------|
| `start` | `String` | Start tag (e.g., `"@"`, `"{{"`, `"["`) |
| `end` | `String` | End tag (e.g., `""`, `"}}"`, `"]"`), may be null |

---

#### setPlaceholderAltAppearancePattern

```java
public void setPlaceholderAltAppearancePattern(String pattern)
```

Sets the regex pattern used to extract alt appearance text from placeholder text.

---

#### setPlaceholderAltAppearance

```java
public void setPlaceholderAltAppearance(boolean alt)
```

Toggles between normal and alternative placeholder appearance.

---

#### isPlaceholderAltAppearance

```java
public boolean isPlaceholderAltAppearance()
```

**Returns:** `true` if alt appearance is currently active.

---

### Placeholders -- Event Listeners

All listener methods return a `Registration` object that can be used to remove the listener.

| Method | Event Type | Section |
|--------|-----------|---------|
| `addPlaceholderButtonClickedListener()` | `PlaceholderButtonClickedEvent` | [11.1](#111-placeholderbuttonclickedevent) |
| `addPlaceholderBeforeInsertListener()` | `PlaceholderBeforeInsertEvent` | [11.2](#112-placeholderbeforeinsertevent) |
| `addPlaceholderInsertedListener()` | `PlaceholderInsertedEvent` | [11.3](#113-placeholderinsertedevent) |
| `addPlaceholderBeforeRemoveListener()` | `PlaceholderBeforeRemoveEvent` | [11.4](#114-placeholderbeforeremoveevent) |
| `addPlaceholderRemovedListener()` | `PlaceholderRemovedEvent` | [11.5](#115-placeholderremovedevent) |
| `addPlaceholderSelectedListener()` | `PlaceholderSelectedEvent` | [11.6](#116-placeholderselectedevent) |
| `addPlaceholderLeaveListener()` | `PlaceholderLeaveEvent` | [11.7](#117-placeholderleaveevent) |
| `addPlaceholderAppearanceChangedListener()` | `PlaceholderAppearanceChangedEvent` | [11.8](#118-placeholderappearancechangedevent) |

---

### Programmatic Text Insertion

#### getTextLength

```java
public void getTextLength(SerializableConsumer<Integer> callback)
```

Asynchronously retrieves the editor's text length. Quill's internal trailing newline is excluded from the count.

| Parameter | Type | Description |
|-----------|------|-------------|
| `callback` | `SerializableConsumer<Integer>` | Consumer that receives the text length |

**Throws:** `NullPointerException` if callback is null.

> **Note:** This is an async callback pattern (V24's synchronous `int getTextLength()` cannot be preserved due to Vaadin's deadlock detection).

---

#### addText (at position)

```java
public void addText(String text, int position)
```

Inserts text at the specified position. Position is clamped to valid range `[0, length-1]` on the client side. No insertion occurs if the editor is disabled.

| Parameter | Type | Description |
|-----------|------|-------------|
| `text` | `String` | Text to insert (not null) |
| `position` | `int` | Zero-based insertion index |

**Throws:** `NullPointerException` if text is null.

---

#### addText (at cursor)

```java
public void addText(String text)
```

Inserts text at the current cursor position. If no selection exists, editor is not focused, or editor is disabled, no insertion occurs.

| Parameter | Type | Description |
|-----------|------|-------------|
| `text` | `String` | Text to insert (not null) |

**Throws:** `NullPointerException` if text is null.

---

### Internationalization

#### setI18n

```java
public void setI18n(EnhancedRichTextEditorI18n i18n)
```

Sets the i18n labels for toolbar buttons and placeholder dialog. Inherited from parent `RichTextEditor.setI18n()` with the extended ERTE i18n class.

---

## 2. Placeholder

```java
package com.vaadin.componentfactory;

public class Placeholder implements Serializable
```

Represents a placeholder token that can be inserted into the editor.

### Constructors

| Constructor | Description |
|-------------|-------------|
| `Placeholder()` | Creates an empty placeholder |
| `Placeholder(String text)` | Creates a placeholder with the given text |
| `Placeholder(JsonNode json)` | Creates a placeholder from a Jackson JSON node (used for event deserialization) |

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getText()` | `String` | The placeholder's display text |
| `setText(String)` | `void` | Sets the display text |
| `getFormat()` | `Map<String, Object>` | Default appearance format (e.g., `{"italic": true}`) |
| `setFormat(Map<String, Object>)` | `void` | Sets the default format |
| `getAltFormat()` | `Map<String, Object>` | Alternative appearance format |
| `setAltFormat(Map<String, Object>)` | `void` | Sets the alt format |
| `getIndex()` | `int` | Last insertion index (populated in events), -1 if not set |
| `setIndex(int)` | `void` | Sets the index |
| `toJson()` | `ObjectNode` | Serializes to Jackson JSON |
| `equals(Object)` | `boolean` | Equality by text |
| `hashCode()` | `int` | Hash by text |

**Supported format attributes:**

| Key | Type | Effect |
|-----|------|--------|
| `"bold"` | `Boolean` | Bold text |
| `"italic"` | `Boolean` | Italic text |
| `"color"` | `String` | Text color (e.g., `"red"`, `"#ff0000"`) |
| `"background"` | `String` | Background color |
| `"font"` | `String` | Font family (e.g., `"monospace"`) |
| `"link"` | `String` | Hyperlink URL |
| `"script"` | `String` | `"super"` or `"sub"` |

---

## 3. TabStop

```java
package com.vaadin.componentfactory;

public class TabStop implements Serializable
```

Defines a tab stop position and alignment on the ruler.

### Constructor

```java
public TabStop(Direction direction, double position)
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `direction` | `TabStop.Direction` | Alignment direction |
| `position` | `double` | Position in pixels from the left edge |

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getDirection()` | `Direction` | The alignment direction |
| `getPosition()` | `double` | Position in pixels |
| `equals(Object)` | `boolean` | Equality by direction and position |
| `hashCode()` | `int` | Hash by direction and position |
| `toString()` | `String` | Debug representation |

### TabStop.Direction (Enum)

| Constant | Description |
|----------|-------------|
| `LEFT` | Text aligns to the right of the tabstop (>text) |
| `RIGHT` | Text aligns to the left of the tabstop (text<) |
| `MIDDLE` | Text is centered at the tabstop (te\|xt) |

---

## 4. ToolbarButton (Enum)

```java
package com.vaadin.componentfactory;

public enum ToolbarButton  // inner enum of EnhancedRichTextEditor
```

30 constants representing toolbar buttons that can be shown/hidden via `setToolbarButtonsVisibility()`.

### Standard RTE 2 Buttons (25)

| Constant | Part Suffix | Group |
|----------|-------------|-------|
| `UNDO` | `"undo"` | History |
| `REDO` | `"redo"` | History |
| `BOLD` | `"bold"` | Emphasis |
| `ITALIC` | `"italic"` | Emphasis |
| `UNDERLINE` | `"underline"` | Emphasis |
| `STRIKE` | `"strike"` | Emphasis |
| `COLOR` | `"color"` | Style |
| `BACKGROUND` | `"background"` | Style |
| `H1` | `"h1"` | Heading |
| `H2` | `"h2"` | Heading |
| `H3` | `"h3"` | Heading |
| `SUBSCRIPT` | `"subscript"` | Glyph Transformation |
| `SUPERSCRIPT` | `"superscript"` | Glyph Transformation |
| `LIST_ORDERED` | `"list-ordered"` | List |
| `LIST_BULLET` | `"list-bullet"` | List |
| `OUTDENT` | `"outdent"` | Indent |
| `INDENT` | `"indent"` | Indent |
| `ALIGN_LEFT` | `"align-left"` | Alignment |
| `ALIGN_CENTER` | `"align-center"` | Alignment |
| `ALIGN_RIGHT` | `"align-right"` | Alignment |
| `IMAGE` | `"image"` | Rich Text |
| `LINK` | `"link"` | Rich Text |
| `BLOCKQUOTE` | `"blockquote"` | Block |
| `CODE_BLOCK` | `"code-block"` | Block |
| `CLEAN` | `"clean"` | Format |

### ERTE-Specific Buttons (5)

| Constant | Part Suffix | Purpose |
|----------|-------------|---------|
| `READONLY` | `"readonly"` | Toggle readonly on selected text |
| `PLACEHOLDER` | `"placeholder"` | Open placeholder insertion dialog |
| `PLACEHOLDER_APPEARANCE` | `"placeholder-display"` | Toggle placeholder format/altFormat |
| `WHITESPACE` | `"whitespace"` | Toggle whitespace indicators |
| `ALIGN_JUSTIFY` | `"align-justify"` | Justify text alignment |

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getPartSuffix()` | `String` | The suffix portion (e.g., `"bold"`) |
| `getPartName()` | `String` | The full part name (e.g., `"toolbar-button-bold"`) |

---

## 5. ToolbarSlot (Enum)

```java
package com.vaadin.componentfactory.toolbar;

public enum ToolbarSlot
```

27 constants representing named positions where custom components can be placed in the toolbar.

### Outer Slots

| Constant | Slot Name | Description |
|----------|-----------|-------------|
| `START` | `"toolbar-start"` | Very first slot in toolbar |
| `END` | `"toolbar-end"` | Very last slot in toolbar |

### Group Slots (22)

Each button group has a `BEFORE_GROUP_*` and `AFTER_GROUP_*` slot:

| Group | Before Slot | After Slot |
|-------|-------------|------------|
| History (undo, redo) | `BEFORE_GROUP_HISTORY` | `AFTER_GROUP_HISTORY` |
| Emphasis (bold, italic, underline, strike) | `BEFORE_GROUP_EMPHASIS` | `AFTER_GROUP_EMPHASIS` |
| Style (color, background) | `BEFORE_GROUP_STYLE` | `AFTER_GROUP_STYLE` |
| Heading (h1, h2, h3) | `BEFORE_GROUP_HEADING` | `AFTER_GROUP_HEADING` |
| Glyph Transformation (subscript, superscript) | `BEFORE_GROUP_GLYPH_TRANSFORMATION` | `AFTER_GROUP_GLYPH_TRANSFORMATION` |
| List (ordered, bullet) | `BEFORE_GROUP_LIST` | `AFTER_GROUP_LIST` |
| Indent (outdent, indent) | `BEFORE_GROUP_INDENT` | `AFTER_GROUP_INDENT` |
| Alignment (left, center, right) | `BEFORE_GROUP_ALIGNMENT` | `AFTER_GROUP_ALIGNMENT` |
| Rich Text (image, link) | `BEFORE_GROUP_RICH_TEXT` | `AFTER_GROUP_RICH_TEXT` |
| Block (blockquote, code-block) | `BEFORE_GROUP_BLOCK` | `AFTER_GROUP_BLOCK` |
| Format (clean) | `BEFORE_GROUP_FORMAT` | `AFTER_GROUP_FORMAT` |

> **Note:** The Style group (color, background) is new in V25. V24 had 10 groups; V25 has 11.

### Custom Group Slots

| Constant | Slot Name | Description |
|----------|-----------|-------------|
| `BEFORE_GROUP_CUSTOM` | `"toolbar-before-group-custom"` | Before the custom group |
| `GROUP_CUSTOM` | `"toolbar"` | The custom group (legacy name for backward compat) |
| `AFTER_GROUP_CUSTOM` | `"toolbar-after-group-custom"` | After the custom group |

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getSlotName()` | `String` | The slot name string used in the DOM `slot` attribute |

---

## 6. ToolbarSwitch

```java
package com.vaadin.componentfactory.toolbar;

public class ToolbarSwitch extends Button
```

A toolbar toggle button. Clicking toggles between active (on) and inactive states. The active state is reflected via an `on` HTML attribute.

### Constructors

| Constructor | Description |
|-------------|-------------|
| `ToolbarSwitch()` | Empty toggle button |
| `ToolbarSwitch(String text)` | Toggle button with text label |
| `ToolbarSwitch(Component icon)` | Toggle button with icon |
| `ToolbarSwitch(VaadinIcon icon)` | Toggle button with Vaadin icon |
| `ToolbarSwitch(String text, Component icon)` | Toggle button with text and icon |

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `toggle()` | `boolean` | Toggles state and returns the new active state |
| `setActive(boolean)` | `void` | Sets active state programmatically |
| `isActive()` | `boolean` | Whether the switch is currently active |
| `addActiveChangedListener(ComponentEventListener<ActiveChangedEvent>)` | `Registration` | Listens for active state changes |

### ActiveChangedEvent

| Method | Returns | Description |
|--------|---------|-------------|
| `isActive()` | `boolean` | The new active state |

---

## 7. ToolbarPopover

```java
package com.vaadin.componentfactory.toolbar;

public class ToolbarPopover extends Popover
```

A specialized `Popover` that integrates with `ToolbarSwitch` for opening and closing. The popover automatically syncs its opened state with the switch's active state.

### Constructor

```java
public ToolbarPopover(ToolbarSwitch referencedSwitch)
```

Creates a popover targeted at the given switch. The popover:
- Opens/closes when the switch is activated/deactivated
- Updates the switch state when opened/closed programmatically
- Autofocus enabled by default (focuses first focusable element)

| Parameter | Type | Description |
|-----------|------|-------------|
| `referencedSwitch` | `ToolbarSwitch` | The switch that controls the popover |

### Static Factory Methods

#### vertical

```java
public static ToolbarPopover vertical(ToolbarSwitch toolbarSwitch, Component... components)
```

Creates a popover with components arranged in a `VerticalLayout`.

---

#### horizontal

```java
public static ToolbarPopover horizontal(ToolbarSwitch toolbarSwitch, Component... components)
```

Creates a popover with components arranged in a `HorizontalLayout` (center-aligned, with padding).

---

#### horizontal (with alignment)

```java
public static ToolbarPopover horizontal(ToolbarSwitch toolbarSwitch, Alignment alignment, Component... components)
```

Creates a popover with components arranged in a `HorizontalLayout` with the specified vertical alignment.

| Parameter | Type | Description |
|-----------|------|-------------|
| `toolbarSwitch` | `ToolbarSwitch` | The switch that controls the popover |
| `alignment` | `FlexComponent.Alignment` | Vertical alignment of components |
| `components` | `Component...` | Content components |

### Methods

#### setFocusOnOpenTarget

```java
public void setFocusOnOpenTarget(Component focusOnOpenTarget)
```

Sets a component to receive focus when the popover opens. Disables default autofocus to avoid conflicts.

| Parameter | Type | Description |
|-----------|------|-------------|
| `focusOnOpenTarget` | `Component` | Component to focus on open |

**Example:**
```java
ToolbarSwitch colorSwitch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
TextField colorField = new TextField("Color");
ToolbarPopover popover = ToolbarPopover.vertical(colorSwitch, colorField);
popover.setFocusOnOpenTarget(colorField);
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, colorSwitch);
```

---

## 8. ToolbarSelectPopup

```java
package com.vaadin.componentfactory.toolbar;

public class ToolbarSelectPopup extends ContextMenu
```

A specialized `ContextMenu` that integrates with `ToolbarSwitch` for opening and closing. Opens on **left-click** (not the default right-click behavior). Inherits the full Vaadin `ContextMenu` API.

### Constructor

```java
public ToolbarSelectPopup(ToolbarSwitch referencedSwitch)
```

Creates a context menu targeted at the given switch. The menu:
- Opens on left-click (via `setOpenOnClick(true)`)
- Syncs the switch's active state with the menu's opened state

| Parameter | Type | Description |
|-----------|------|-------------|
| `referencedSwitch` | `ToolbarSwitch` | The switch that triggers the menu |

### Inherited Methods (from ContextMenu)

| Method | Returns | Description |
|--------|---------|-------------|
| `addItem(String text, ComponentEventListener)` | `MenuItem` | Adds a text menu item |
| `addItem(Component component, ComponentEventListener)` | `MenuItem` | Adds a component menu item |
| `addComponent(Component component)` | `void` | Adds a non-clickable component (e.g., `Hr` separator) |

**Example:**
```java
ToolbarSwitch insertSwitch = new ToolbarSwitch(VaadinIcon.PLUS);
ToolbarSelectPopup menu = new ToolbarSelectPopup(insertSwitch);
menu.addItem("Horizontal Rule", e -> { /* ... */ });
menu.addItem("Page Break", e -> { /* ... */ });
menu.addComponent(new Hr());
menu.addItem("Special Character...", e -> { /* ... */ });
editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, insertSwitch);
```

---

## 9. ToolbarDialog

```java
package com.vaadin.componentfactory.toolbar;

public class ToolbarDialog extends Dialog
```

A specialized `Dialog` that integrates with `ToolbarSwitch` for opening and closing. The dialog automatically syncs its opened state with the switch's active state, and returns focus to the switch when closed.

**Default settings:** Non-modal, resizable, draggable, no padding (`LUMO_NO_PADDING`), closes on ESC, does NOT close on outside click.

### Constructors

```java
public ToolbarDialog(ToolbarSwitch toolbarSwitch)
```

Creates a center-positioned dialog controlled by the given switch.

```java
public ToolbarDialog(ToolbarSwitch toolbarSwitch, boolean openAtSwitch)
```

Creates a dialog controlled by the given switch. When `openAtSwitch` is `true`, the dialog is positioned directly below the switch button.

| Parameter | Type | Description |
|-----------|------|-------------|
| `toolbarSwitch` | `ToolbarSwitch` | The switch that controls the dialog |
| `openAtSwitch` | `boolean` | Position at switch (`true`) or center (`false`) |

### Static Factory Methods

#### vertical

```java
public static ToolbarDialog vertical(ToolbarSwitch toolbarSwitch, Component... components)
```

Creates a center-positioned dialog with components arranged in a `VerticalLayout`.

---

#### horizontal

```java
public static ToolbarDialog horizontal(ToolbarSwitch toolbarSwitch, Component... components)
```

Creates a center-positioned dialog with components arranged in a `HorizontalLayout` (center-aligned, with padding).

---

#### horizontal (with alignment)

```java
public static ToolbarDialog horizontal(ToolbarSwitch toolbarSwitch, FlexComponent.Alignment alignment, Component... components)
```

Creates a center-positioned dialog with the specified horizontal alignment.

### Methods

#### setFocusOnOpenTarget

```java
public void setFocusOnOpenTarget(Component focusOnOpenTarget)
```

Sets a component to receive focus when the dialog opens.

| Parameter | Type | Description |
|-----------|------|-------------|
| `focusOnOpenTarget` | `Component` | Component to focus on open |

---

#### getToolbarSwitch

```java
public ToolbarSwitch getToolbarSwitch()
```

**Returns:** The toolbar switch that controls this dialog.

---

#### openAtSwitch

```java
public ToolbarDialog openAtSwitch()
```

Configures the dialog to open at the toolbar switch's position (below the button) instead of centering. Returns `this` for method chaining.

**Example:**
```java
ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
Checkbox showRulers = new Checkbox("Show rulers");
Checkbox showWhitespace = new Checkbox("Show whitespace");

ToolbarDialog.vertical(settingsSwitch, showRulers, showWhitespace)
    .openAtSwitch();

editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
```

---

## 10. EnhancedRichTextEditorI18n

```java
package com.vaadin.componentfactory;

public static class EnhancedRichTextEditorI18n
    extends RichTextEditor.RichTextEditorI18n
    implements Serializable  // inner class of EnhancedRichTextEditor
```

Extended i18n for ERTE -- adds labels for ERTE-specific toolbar buttons and the placeholder dialog. Inherits all standard RTE 2 labels.

### ERTE-Specific Labels

All setters return `EnhancedRichTextEditorI18n` for fluent chaining.

| Getter | Setter | Default Value | Purpose |
|--------|--------|---------------|---------|
| `getReadonly()` | `setReadonly(String)` | "Readonly" | Lock button tooltip |
| `getWhitespace()` | `setWhitespace(String)` | "Show whitespace" | Whitespace indicator button tooltip |
| `getPlaceholder()` | `setPlaceholder(String)` | "Placeholder" | Placeholder button tooltip |
| `getPlaceholderAppearance()` | `setPlaceholderAppearance(String)` | "Toggle placeholder appearance" | Appearance toggle tooltip |
| `getPlaceholderDialogTitle()` | `setPlaceholderDialogTitle(String)` | "Placeholders" | Dialog title |
| `getPlaceholderComboBoxLabel()` | `setPlaceholderComboBoxLabel(String)` | "Select a placeholder" | Combo-box label |
| `getPlaceholderAppearanceLabel1()` | `setPlaceholderAppearanceLabel1(String)` | "Plain" | First appearance label |
| `getPlaceholderAppearanceLabel2()` | `setPlaceholderAppearanceLabel2(String)` | "Value" | Second appearance label |
| `getAlignJustify()` | `setAlignJustify(String)` | "Justify" | Justify button tooltip |

### Inherited Labels (Covariant Overrides)

All inherited setters return `EnhancedRichTextEditorI18n` (not the parent type), enabling full fluent chaining:

`setUndo()`, `setRedo()`, `setBold()`, `setItalic()`, `setUnderline()`, `setStrike()`, `setColor()`, `setBackground()`, `setH1()`, `setH2()`, `setH3()`, `setSubscript()`, `setSuperscript()`, `setListOrdered()`, `setListBullet()`, `setOutdent()`, `setIndent()`, `setAlignLeft()`, `setAlignCenter()`, `setAlignRight()`, `setImage()`, `setLink()`, `setBlockquote()`, `setCodeBlock()`, `setClean()`

**Example:**
```java
editor.setI18n(new EnhancedRichTextEditorI18n()
    .setBold("Fett")
    .setItalic("Kursiv")
    .setReadonly("Schreibschutz")
    .setPlaceholder("Platzhalter")
    .setAlignJustify("Blocksatz"));
```

---

## 11. Events

All placeholder events are inner classes of `EnhancedRichTextEditor`.

### 11.1 PlaceholderButtonClickedEvent

```java
@DomEvent("placeholder-button-click")
public static class PlaceholderButtonClickedEvent
    extends ComponentEvent<EnhancedRichTextEditor>
```

Fired when the placeholder toolbar button is clicked. This event fires **before** the placeholder dialog opens.

| Method | Returns | Description |
|--------|---------|-------------|
| `getPosition()` | `int` | Cursor position (zero-based delta index) |
| `insert(Placeholder)` | `void` | Inserts a placeholder at the cursor position |
| `insert(Placeholder, int)` | `void` | Inserts a placeholder at the specified position |

> **Note:** `@EventData("event.preventDefault()")` is used as a side effect to prevent the default dialog. If you have a listener registered, the built-in dialog is suppressed. To re-open it, call `editor.getElement().executeJs("this._placeholderEditing = true")`.

---

### 11.2 PlaceholderBeforeInsertEvent

```java
@DomEvent("placeholder-before-insert")
public static class PlaceholderBeforeInsertEvent
    extends AbstractMultiPlaceholderEvent
```

Fired before placeholders are inserted. **Cancellable:** the insertion only proceeds if `insert()` is called.

| Method | Returns | Description |
|--------|---------|-------------|
| `getPlaceholders()` | `List<Placeholder>` | Placeholders about to be inserted (from master list) |
| `insert()` | `void` | **Must call** to confirm insertion. Omitting cancels. |

---

### 11.3 PlaceholderInsertedEvent

```java
@DomEvent("placeholder-insert")
public static class PlaceholderInsertedEvent
    extends AbstractMultiPlaceholderEvent
```

Fired after placeholders are successfully inserted. Notification event (read-only).

| Method | Returns | Description |
|--------|---------|-------------|
| `getPlaceholders()` | `List<Placeholder>` | Inserted placeholders (from master list) |

---

### 11.4 PlaceholderBeforeRemoveEvent

```java
@DomEvent("placeholder-before-delete")
public static class PlaceholderBeforeRemoveEvent
    extends AbstractMultiPlaceholderEvent
```

Fired before placeholders are removed. **Cancellable:** the removal only proceeds if `remove()` is called.

| Method | Returns | Description |
|--------|---------|-------------|
| `getPlaceholders()` | `List<Placeholder>` | Placeholders about to be removed |
| `remove()` | `void` | **Must call** to confirm removal. Omitting cancels. |

---

### 11.5 PlaceholderRemovedEvent

```java
@DomEvent("placeholder-delete")
public static class PlaceholderRemovedEvent
    extends AbstractMultiPlaceholderEvent
```

Fired after placeholders are successfully removed. Notification event (read-only).

| Method | Returns | Description |
|--------|---------|-------------|
| `getPlaceholders()` | `List<Placeholder>` | Removed placeholders |

---

### 11.6 PlaceholderSelectedEvent

```java
@DomEvent("placeholder-select")
public static class PlaceholderSelectedEvent
    extends AbstractMultiPlaceholderEvent
```

Fired when the user clicks on a placeholder or navigates to it via keyboard.

| Method | Returns | Description |
|--------|---------|-------------|
| `getPlaceholders()` | `List<Placeholder>` | Selected placeholders |

---

### 11.7 PlaceholderLeaveEvent

```java
@DomEvent("placeholder-leave")
public static class PlaceholderLeaveEvent
    extends ComponentEvent<EnhancedRichTextEditor>
```

Fired when the cursor moves away from a placeholder. No additional methods beyond the base `ComponentEvent`.

---

### 11.8 PlaceholderAppearanceChangedEvent

```java
@DomEvent("placeholder-appearance-change")
public static class PlaceholderAppearanceChangedEvent
    extends ComponentEvent<EnhancedRichTextEditor>
```

Fired when a placeholder switches between its default format and altFormat.

| Method | Returns | Description |
|--------|---------|-------------|
| `getAltAppearance()` | `Boolean` | `true` if using altFormat, `false` if default, `null` if unknown |
| `getAppearanceLabel()` | `String` | Custom appearance label, or `null` if not set |

---

### AbstractMultiPlaceholderEvent (Base Class)

```java
public static abstract class AbstractMultiPlaceholderEvent
    extends ComponentEvent<EnhancedRichTextEditor>
```

Base class for events that carry a list of placeholders. Handles deserialization and master-list lookup.

| Method | Returns | Description |
|--------|---------|-------------|
| `getPlaceholders()` | `List<Placeholder>` | Full placeholder objects from the master list, with `index` populated |

---

## 12. Inherited from RichTextEditor

`EnhancedRichTextEditor` inherits the full API of Vaadin's `RichTextEditor`. Key inherited methods:

### Value Access

| Method | Description |
|--------|-------------|
| `setValue(String html)` | Sets HTML content |
| `getValue()` | Returns HTML content |
| `asDelta()` | Returns `DeltaValue` wrapper for Delta JSON access |
| `asDelta().setValue(String deltaJson)` | Sets content via Delta JSON |
| `asDelta().getValue()` | Returns content as Delta JSON |
| `addValueChangeListener(...)` | Listens for HTML value changes (fires on blur) |
| `asDelta().addValueChangeListener(...)` | Listens for Delta value changes (fires on blur) |

### State

| Method | Description |
|--------|-------------|
| `setReadOnly(boolean)` | Whole-editor readonly mode |
| `isReadOnly()` | Returns readonly state |
| `setEnabled(boolean)` | Enable/disable editor |
| `isEnabled()` | Returns enabled state |
| `setValueChangeMode(ValueChangeMode)` | Controls when value sync fires |

### Focus

| Method | Description |
|--------|-------------|
| `focus()` | Programmatic focus (delegates to Quill's internal focus mechanism for reliable behavior) |

For the complete inherited API, see the [Vaadin RichTextEditor documentation](https://vaadin.com/docs/v25/components/rich-text-editor).

---

## 13. CSS Shadow Parts

ERTE toolbar buttons and groups expose CSS shadow parts for external styling.

### Individual Button Parts

```css
vcf-enhanced-rich-text-editor::part(toolbar-button-bold) {
    color: red;
}

vcf-enhanced-rich-text-editor::part(toolbar-button-readonly) {
    background: lightyellow;
}
```

Available button part suffixes (30 total):
- Standard: `undo`, `redo`, `bold`, `italic`, `underline`, `strike`, `color`, `background`, `h1`, `h2`, `h3`, `subscript`, `superscript`, `list-ordered`, `list-bullet`, `outdent`, `indent`, `align-left`, `align-center`, `align-right`, `image`, `link`, `blockquote`, `code-block`, `clean`
- ERTE-specific: `readonly`, `placeholder`, `placeholder-display`, `whitespace`, `align-justify`

Part names are: `toolbar-button-{suffix}` (e.g., `toolbar-button-bold`, `toolbar-button-readonly`).

### Group Parts

```css
vcf-enhanced-rich-text-editor::part(toolbar-group-history) {
    border-right: 2px solid var(--lumo-primary-color);
}
```

Available group part suffixes (11 total):
- `history`, `emphasis`, `style`, `heading`, `glyph-transformation`, `list`, `indent`, `alignment`, `rich-text`, `block`, `format`

Part names are: `toolbar-group-{suffix}` (e.g., `toolbar-group-history`, `toolbar-group-emphasis`).

### Toolbar Container

```css
vcf-enhanced-rich-text-editor::part(toolbar) {
    background: var(--lumo-tint-5pct);
}
```

### Slotted Custom Components

Components added via `addToolbarComponents()` automatically receive `part="toolbar-custom-component"`. ERTE provides interactive state styling for slotted components:

| State | CSS Selector | Behavior |
|-------|-------------|----------|
| Default | `::slotted([part~='toolbar-custom-component'])` | Inherits `--vaadin-rich-text-editor-toolbar-button-*` custom properties |
| Hover | `::slotted(button[part~='toolbar-custom-component']:not([on]):hover)` | Subtle background highlight |
| Focus | `::slotted(button[part~='toolbar-custom-component']:focus-visible)` | Focus ring outline |
| Active/Pressed | `::slotted([part~='toolbar-custom-component'][on])` | Solid primary background (matches built-in pressed style) |
| Disabled | `::slotted([part~='toolbar-custom-component'][disabled])` | Muted text, no pointer events |

> **Note:** Custom components inherit from RTE 2's `--vaadin-rich-text-editor-toolbar-button-*` properties, so user overrides of those properties apply to custom components too.

---

## 14. CSS Content Classes

These CSS classes are applied to elements inside `.ql-editor` (the editor content area).

| Class | Element | Purpose |
|-------|---------|---------|
| `.ql-tab` | `<span>` | Tab embed (inline-block, calculated width) |
| `.ql-placeholder` | `<span>` | Placeholder embed |
| `.ql-readonly` | `<span>` | Readonly section (contenteditable="false") |
| `.ql-soft-break` | `<span>` | Soft-break embed (contains `<br>`) |
| `.ql-nbsp` | `<span>` | Non-breaking space |

### Quill Alignment & Indent Classes

- `ql-align-left`, `ql-align-center`, `ql-align-right`, `ql-align-justify` -- paragraph alignment
- `ql-indent-1` through `ql-indent-8` -- indentation levels

### Ruler Selectors

| Selector | Purpose |
|----------|---------|
| `[part~="horizontalRuler"]` | Horizontal ruler container |
| `vaadin-icon` inside ruler | Tabstop markers (alignment icons) |

---

## 15. ERTE Custom Properties

ERTE provides 20 CSS custom properties for customizing the appearance of editor content elements. These are the recommended approach for visual customization -- they use Vaadin design tokens as fallback values, ensuring theme awareness.

Override custom properties on the host element:

```css
vcf-enhanced-rich-text-editor {
    --vaadin-erte-readonly-background: lightyellow;
    --vaadin-erte-readonly-border-color: orange;
    --vaadin-erte-placeholder-background: #e8f4fd;
    --vaadin-erte-ruler-height: 1.25rem;
}
```

### Readonly Sections (6 properties)

| Property | Default | Description |
|----------|---------|-------------|
| `--vaadin-erte-readonly-color` | `var(--vaadin-text-color-secondary)` | Text color |
| `--vaadin-erte-readonly-background` | `var(--vaadin-background-container)` | Background color |
| `--vaadin-erte-readonly-border-color` | `var(--vaadin-border-color-secondary)` | Border (outline) color |
| `--vaadin-erte-readonly-border-width` | `1px` | Border (outline) width |
| `--vaadin-erte-readonly-border-radius` | `var(--lumo-border-radius-s)` | Corner radius |
| `--vaadin-erte-readonly-padding` | `calc(var(--vaadin-padding-xs) / 2)` | Horizontal padding |

### Placeholders (6 properties)

| Property | Default | Description |
|----------|---------|-------------|
| `--vaadin-erte-placeholder-color` | `inherit` | Text color |
| `--vaadin-erte-placeholder-background` | `var(--lumo-primary-color-10pct)` | Background color |
| `--vaadin-erte-placeholder-border-color` | `transparent` | Border (outline) color |
| `--vaadin-erte-placeholder-border-width` | `0` | Border (outline) width |
| `--vaadin-erte-placeholder-border-radius` | `var(--lumo-border-radius-s)` | Corner radius |
| `--vaadin-erte-placeholder-padding` | `calc(var(--vaadin-padding-xs) / 2)` | Horizontal padding |

### Whitespace Indicators (3 properties)

| Property | Default | Description |
|----------|---------|-------------|
| `--vaadin-erte-whitespace-indicator-color` | `var(--lumo-contrast-40pct)` | Color for tab, NBSP, soft-break indicators |
| `--vaadin-erte-whitespace-paragraph-indicator-color` | `var(--lumo-contrast-30pct)` | Color for paragraph-end indicator |
| `--vaadin-erte-whitespace-indicator-spacing` | `calc(var(--vaadin-padding-xs) / 2)` | Spacing around indicators |

### Ruler (5 properties)

| Property | Default | Description |
|----------|---------|-------------|
| `--vaadin-erte-ruler-height` | `0.9375rem` | Ruler bar height |
| `--vaadin-erte-ruler-border-color` | `var(--vaadin-border-color, var(--lumo-contrast-20pct, rgb(158, 170, 182)))` | Ruler border color |
| `--vaadin-erte-ruler-background` | `url(...)` (ruler tick image) | Ruler background image |
| `--vaadin-erte-ruler-marker-size` | `0.9375rem` | Tabstop marker icon size |
| `--vaadin-erte-ruler-marker-color` | `inherit` | Tabstop marker icon color |

> **Migration note:** V24 (ERTE 1) required manual CSS class overrides for content styling. V25 (ERTE 2) provides these official custom properties as the preferred approach.

---

## 16. Sanitization

### 16.1 Default Sanitizer

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

### 16.2 What Gets Stripped

- `<script>`, `<iframe>`, `<object>`, `<embed>`, `<form>` tags
- JavaScript event handlers (`onclick`, `onerror`, `onload`, etc.)
- Unknown CSS classes (not in the ERTE or Quill whitelist)
- Dangerous CSS functions (e.g., `url()`, `expression()`, `var()`)
- `@import` directives in CSS
- SVG data URLs (can contain embedded scripts)
- `contenteditable="true"` (only `"false"` is allowed)
- CSS comments (`/* ... */`)

### 16.3 Custom Sanitization

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

## 17. Built-in Keyboard Shortcuts

These shortcuts are always active and cannot be removed:

| Shortcut | Action | Condition |
|----------|--------|-----------|
| Tab | Insert tab embed | Tabstops configured |
| Shift+Enter | Insert soft-break | Always |
| Shift+Space | Insert non-breaking space | Always |
| Ctrl+P / Cmd+P | Open placeholder dialog | Placeholders configured |

> **Note:** Ctrl+P/Cmd+P may be intercepted by the browser's print dialog in some configurations.

---

## 18. Value Format Guide

### 18.1 HTML (Default)

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

### 18.2 Delta (Optional)

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

### 18.3 Delta Structure Reference

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

### 18.4 Format Selection Guide

| Use Case | Recommended Format | Reason |
|----------|-------------------|--------|
| Form field binding | HTML | Standard Vaadin pattern |
| Database storage | HTML | Compact, query-friendly |
| Readonly sections | Delta | Readonly is a Delta attribute |
| Placeholder content | Delta | Placeholder is a Delta embed |
| Template engine input | HTML | Easy to process server-side |
| Content migration | Delta | Preserves all formatting precisely |
| Unit test assertions | Delta | Deterministic structure |
