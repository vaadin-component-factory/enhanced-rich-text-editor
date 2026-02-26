# ERTE Tables Extension Guide

**For developers who need to add table support to their ERTE-based applications.** This guide covers everything from setup through advanced customization. No prior experience with Quill tables required.

**Related documentation:** Extend the ERTE core component with [EXTENDING.md](/workspace/docs/dev/EXTENDING.md). Use I18n across your app with [CONTRIBUTING.md](/workspace/docs/dev/CONTRIBUTING.md) patterns.

---

## 1. Getting Started

### What You Get

The Tables extension adds three toolbar buttons that let end users:

- **Add Table** — Insert a new table (specify rows and columns)
- **Modify Table** — Append/remove rows or columns, merge/split cells, remove the table
- **Style Templates** — Apply named, reusable table styling rules (colors, borders, dimensions, etc.)

The extension also fires events when tables are selected, cells are changed, or templates are modified — so your app can react to table operations.

### Installation

Add this to your `pom.xml`:

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-tables-v25</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

**Requirements:** Java 21+, Vaadin 25.0.x, `enhanced-rich-text-editor-v25` 6.0.0-SNAPSHOT

### Quickstart

Enable tables on an ERTE instance in three lines:

```java
EnhancedRichTextEditor rte = new EnhancedRichTextEditor();

// Enable tables with default i18n
EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte);

add(rte);
```

The three toolbar buttons appear automatically. Users can now insert and modify tables. Done.

---

## 2. Toolbar Components

### The Three Buttons

**Add Table** — Appears as a table icon with a plus sign. When clicked, opens a popover with two `IntegerField` inputs:

- **Rows** — number of rows (default: 3, min: 1, max: 20)
- **Columns** — number of columns (default: 3, min: 1, max: 20)

After entering values, click the **Add** button (the plus icon) to insert the table at the current cursor position. The popover closes automatically. Disabled when the cursor is already inside a table (you can't nest tables).

**Modify Table** — Appears as a table icon with tools/wrench icon. Enabled only when a table is selected. Opens a dropdown menu with:

- Append row above / below (2 items)
- Remove row (1 item)
- Separator
- Append column before / after (2 items)
- Remove column (1 item)
- Separator
- Merge selected cells (enabled only during multi-cell selection)
- Split cell (1 item)
- Separator
- Remove table (1 item)

All operations work on the currently selected table.

**Style Templates** — Appears as a table icon with eye icon. Opens a dialog for managing table styling templates. Disabled when no table is selected. See the **Style Templates** section below for details.

### Toggling and Customizing

You can access and modify these buttons programmatically:

```java
tables.getAddTableToolbarButton().setVisible(false); // hide Add Table button
tables.getModifyTableToolbarButton().setEnabled(false); // disable Modify Table button

// Access the popup for advanced configuration
ToolbarPopover addPopover = tables.getAddTablePopup();
addPopover.setAutofocus(false); // don't focus first field on open

// Access the dropdown menu for Modify Table
ToolbarSelectPopup modifyMenu = tables.getModifyTableSelectPopup();
modifyMenu.getMenuItems(); // list of all menu items
```

---

## 3. Table Operations

### Inserting Tables

The easiest way is through the UI — click **Add Table** and fill in the dimensions. Programmatically, you can insert a table at the current cursor position:

```java
// Insert a 4×3 table (4 rows, 3 columns)
tables.insertTableAtCurrentPosition(4, 3);

// Insert a table and apply a style template
tables.insertTableAtCurrentPosition(4, 3, "myTemplate");
```

The template ID must be a valid CSS class name (letters, numbers, hyphens only) and must exist in your templates. If the template doesn't exist, the table is still inserted but without styling.

### Modifying Tables

All modifications happen through the **Modify Table** menu. Programmatically, you can't directly modify a specific table — instead, operations work on the currently selected table (the one your cursor is in or on). This mirrors how users interact with the UI.

The toolbar buttons handle row/column changes automatically:

```java
// User clicks "Append row above" → the template dialog's row index updates
// User clicks "Merge cells" → the template dialog updates to show merged cell
```

### Cell Selection

To select multiple cells for merging, hold **Ctrl** and click individual cells, or **Ctrl+drag** to select a range. Selected cells get the `ql-cell-selected` CSS class (a light background by default). The **Merge selected cells** menu item is enabled only when you have multiple cells selected.

Clicking a cell without Ctrl clears the selection and places the cursor in that cell.

### Removing Tables

Click **Modify Table** → **Remove table**. This deletes the entire table from the editor. Cannot be undone by the Templates dialog.

---

## 4. Style Templates

### What Are Templates?

Templates are named collections of styling rules for tables. Each rule targets table-level properties (background, borders), specific rows (header, footer, even, odd, or by number), specific columns (by number), or specific cells (by x/y coordinates). You define templates once, then apply them to tables.

### How to Use Templates

**Load templates from JSON:**

```java
String templateJson = loadFromDatabase(); // your storage
ObjectNode templates = TemplateParser.parseJson(templateJson);
tables.setTemplates(templates);
```

**Apply a template to a table:** When a table is selected, click **Style Templates** and choose a template from the **Current Template** dropdown. The template is applied immediately.

**Programmatically apply a template:**

```java
// Set a template for the currently selected table
tables.setTemplateIdForCurrentTable("myTemplate");
```

### Template JSON Structure

Each template is keyed by its **template ID** (a CSS class name) and has the following structure:

```json
{
  "myTemplate": {
    "name": "Professional",
    "table": {
      "bgColor": "#ffffff",
      "color": "#000000",
      "width": "100%",
      "height": "auto",
      "border": "2px solid #333",
      "borderCells": "1px solid #ddd"
    },
    "rows": [
      {
        "index": "0n+1",
        "declarations": {
          "bgColor": "#f5f5f5",
          "color": "#333",
          "height": "2rem",
          "border": "1px solid #999"
        }
      },
      {
        "index": "2n",
        "declarations": {
          "bgColor": "#fafafa"
        }
      }
    ],
    "cols": [
      {
        "index": "1",
        "declarations": {
          "width": "15rem",
          "bgColor": "#f0f0f0"
        }
      }
    ],
    "cells": [
      {
        "x": 1,
        "y": 1,
        "declarations": {
          "bgColor": "#ff0000",
          "color": "#ffffff"
        }
      }
    ]
  }
}
```

### Row Index Patterns

Row and column indexes use CSS pseudo-class syntax:

| Pattern | Meaning |
|---------|---------|
| `"1"` | Row 1 (first row = header) |
| `"2n"` | Even-numbered rows (2, 4, 6, ...) |
| `"2n+1"` | Odd-numbered rows (1, 3, 5, ...) |
| `"0n+1"` | First row (shorthand for header) |
| `"0n+1"` with `"last": true` | Last row (footer) — see example below |

**Special syntax for header/footer:**

```json
{
  "rows": [
    {
      "index": "0n+1",
      "declarations": { "bgColor": "#e0e0e0" }
    },
    {
      "index": "0n+1",
      "last": true,
      "declarations": { "bgColor": "#e0e0e0" }
    }
  ]
}
```

The `"last": true` flag distinguishes the footer row from the header row.

### Style Properties

Each `declarations` object supports CSS properties. Allowed properties depend on context:

| Property | table | rows | cols | cells |
|----------|:-----:|:----:|:----:|:-----:|
| `bgColor` | yes | yes | yes | yes |
| `color` | yes | yes | yes | yes |
| `border` | yes | yes | yes | yes |
| `borderCells` | yes | — | — | — |
| `width` | yes | — | yes | — |
| `height` | yes | yes | — | — |

**CSS color formats:** hex (`#fff`), named (`red`), rgb/rgba, hsl/hsla, or CSS variables (`var(--color-name)`).

### The Templates Dialog

Click **Style Templates** to open the dialog. It has these sections:

**Active Template Selection**
- **Current Template** dropdown — choose from loaded templates
- **Template name** text field — edit the display name
- **Create** button — create a new template
- **Copy** button — duplicate the selected template
- **Delete** button — remove the template (with confirmation)

**Table Section**
- Background color, text color, borders (table and cells), dimensions

**Current Row Section**
- Edit styles for the row the cursor is currently in
- Background, text color, height, border

**Current Column Section**
- Edit styles for the column the cursor is currently in
- Background, text color, width, border

**Special Rows Section**
- Header, footer, even, odd — each with background, text color, height, border

All changes are applied in real-time. No "Save" button needed.

### Customizing Dimension Units

By default, dimension fields (width, height) use `rem` as the unit. Change it globally:

```java
tables.getStyleTemplatesDialog().getDefaults().setDimensionUnit("px");
// Now all dimension fields will show values in pixels
```

### Injecting Custom CSS

You can inject additional CSS before or after the auto-generated template CSS:

```java
// Inject custom CSS before generated styles (lower priority)
tables.setCustomStyles("table { margin: 1em 0; }", true);

// Inject custom CSS after generated styles (higher priority)
tables.setCustomStyles("table td { font-size: 14px; }", false);

// Call again to replace (each position is independent)
tables.setCustomStyles("table { margin: 0; }", true); // replaces previous "before"
```

The CSS is injected into the editor's shadow DOM, so it only affects table styling, not the rest of your page.

---

## 5. Events

The Tables extension fires 8 event types. All are registered on the `EnhancedRichTextEditorTables` instance.

### Table Selection Events

**TableSelectedEvent** — fired when the cursor enters or leaves a table.

```java
tables.addTableSelectedListener(e -> {
    if (e.isSelected()) {
        log.info("Table selected: {}", e.getTemplate());
    } else {
        log.info("Table deselected");
    }

    // Cell selection also matters
    if (e.isCellSelectionActive()) {
        log.info("Multiple cells are selected");
    }
});
```

Properties:
- `isSelected()` — true if a table is now selected
- `isCellSelectionActive()` — true if multiple cells are selected (merge is possible)
- `getTemplate()` — template ID of the selected table (null if no template)

**TableCellChangedEvent** — fired when the cursor moves to a different cell within a table.

```java
tables.addTableCellChangedListener(e -> {
    Integer row = e.getRowIndex();
    Integer col = e.getColIndex();

    if (row != null) {
        log.info("Moved to row {}, col {}", row, col);
    }
});
```

Properties:
- `getRowIndex()` — 0-based row index (or null if deselected)
- `getColIndex()` — 0-based column index (or null if deselected)
- `getOldRowIndex()` — previous row index (or null)
- `getOldColIndex()` — previous column index (or null)

### Template Events

All template events extend `TemplateModificationEvent` and fire when templates are modified (either by the user or by your code).

**TemplatesInitializedEvent** — fired after `setTemplates()` is called.

```java
tables.addTemplatesInitializedListener(e -> {
    ObjectNode templates = e.getTemplates();
    String css = e.getCssString();
    log.info("Templates loaded: {} templates, CSS: {} bytes",
        templates.size(), css.length());
});
```

**TemplateCreatedEvent** — new template created (user clicked Create button).

```java
tables.addTemplateCreatedListener(e -> {
    String templateId = e.getTemplateId();
    saveTemplatesToDatabase(tables.getTemplates()); // persist
});
```

**TemplateCopiedEvent** — template copied (user clicked Copy button).

```java
tables.addTemplateCopiedListener(e -> {
    String newTemplateId = e.getTemplateId();
    String copiedFrom = e.getCopiedTemplateId().orElse(null);
    log.info("Copied {} to {}", copiedFrom, newTemplateId);
    saveTemplatesToDatabase(tables.getTemplates());
});
```

**TemplateUpdatedEvent** — template modified (name change, rule change).

```java
tables.addTemplateUpdatedListener(e -> {
    String templateId = e.getTemplateId();
    log.info("Template updated: {}", templateId);
    saveTemplatesToDatabase(tables.getTemplates());
});
```

**TemplateDeletedEvent** — template deleted (user confirmed deletion).

```java
tables.addTemplateDeletedListener(e -> {
    String deletedId = e.getTemplateId();
    log.info("Template deleted: {}", deletedId);
    saveTemplatesToDatabase(tables.getTemplates());
});
```

**TemplateSelectedEvent** — active template changed for the current table.

```java
tables.addTemplateSelectedListener(e -> {
    String templateId = e.getTemplateId(); // null if no template
    log.info("Table template changed to: {}", templateId);
});
```

### Event Base Class

All events inherit from `EnhancedRichTextEditorTablesComponentEvent`, which extends `ComponentEvent<EnhancedRichTextEditor>`. They fire on the underlying ERTE component, but you register listeners on the `EnhancedRichTextEditorTables` instance for convenience. Every `add*Listener()` method returns a `Registration` you can use to unregister later:

```java
Registration reg = tables.addTableSelectedListener(e -> { /* ... */ });
// Later, when no longer needed:
reg.remove();
```

```java
// Get the underlying ERTE component from any event
tables.addTemplateUpdatedListener(e -> {
    EnhancedRichTextEditor rte = e.getTableExtension().getRte();
});
```

---

## 6. Theming & Styling

### CSS Custom Properties

Tables support 11 CSS custom properties for fine-grained styling. Set them on the ERTE component to control appearance.

**Cell borders and padding (7 properties):**

```css
vcf-enhanced-rich-text-editor {
  --vaadin-erte-table-border-color: #e0e0e0;
  --vaadin-erte-table-border-width: 1px;
  --vaadin-erte-table-border-style: solid;
  --vaadin-erte-table-cell-padding: 2px 5px;
  --vaadin-erte-table-cell-min-height: 1.625em;
  --vaadin-erte-table-cell-background: transparent;
  --vaadin-erte-table-cell-vertical-align: top;
}
```

**Cell selection and focus states (4 properties):**

```css
vcf-enhanced-rich-text-editor {
  --vaadin-erte-table-cell-selected-background: var(--lumo-primary-color-10pct);
  --vaadin-erte-table-cell-hover-background: transparent;
  --vaadin-erte-table-cell-focus-color: var(--vaadin-focus-ring-color);
  --vaadin-erte-table-cell-focus-width: var(--vaadin-focus-ring-width, 2px);
}
```

**In your application CSS:**

```css
.my-editor {
  --vaadin-erte-table-border-color: #2196f3;
  --vaadin-erte-table-cell-padding: 8px 12px;
}
```

### Programmatic Color Control

Set table and cell hover/focus colors at runtime:

```java
// Table border on hover (dashed outline around the table)
tables.setTableHoverColor("var(--lumo-primary-color)");

// Cell background on hover
tables.setTableCellHoverColor("var(--lumo-primary-color-10pct)");

// Table border on focus (when a cell is focused)
tables.setTableFocusColor("var(--lumo-warning-color)");

// Cell background on focus
tables.setTableCellFocusColor("var(--lumo-warning-color-10pct)");

// Disable a feature by passing null
tables.setTableHoverColor(null);
```

Pass a CSS color value. Invalid colors throw `IllegalArgumentException`. Accepted formats: hex (`#fff`, `#ffffff`), named colors (`red`, `blue`), `rgb()`/`rgba()`, `hsl()`/`hsla()`, and CSS variables (`var(--my-color)`).

### CSS Classes for Custom Styling

The currently focused cell gets the `focused-cell` CSS class. Use it to style:

```css
vcf-enhanced-rich-text-editor::part(editor) table td.focused-cell {
  outline: 2px solid red;
}
```

Selected cells (during multi-select) get the `ql-cell-selected` class.

### Accessibility

The extension includes forced-colors media query support, so table selection and focus states remain visible in Windows High Contrast Mode.

---

## 7. Internationalization (i18n)

All toolbar labels and dialog text can be customized for any language.

### Toolbar Labels

Use `TablesI18n` to customize toolbar button tooltips and menu item labels:

```java
TablesI18n i18n = new TablesI18n();
i18n.setInsertTableToolbarSwitchTooltip("Tabelle einfügen");
i18n.setInsertTableRowsFieldLabel("Zeilen");
i18n.setInsertTableColumnsFieldLabel("Spalten");
i18n.setModifyTableToolbarSwitchTooltip("Tabelle ändern");
i18n.setModifyTableAppendRowAboveItemLabel("Zeile oben einfügen");
// ... set all other labels ...

EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte, i18n);
```

All setters follow the pattern `set[Component][Property](String)`. Setter names are self-explanatory; use your IDE's autocomplete to discover them.

### Template Dialog Labels

Customize the template dialog through the nested `TemplatesI18n` class:

```java
TablesI18n i18n = new TablesI18n();
TablesI18n.TemplatesI18n templatesI18n = i18n.getTemplatesI18n();
templatesI18n.setDialogTitle("Tabellenvorlagen");
templatesI18n.setCurrentTemplateSelectFieldLabel("Aktuelle Vorlage");
templatesI18n.setCreateNewTemplateButtonTooltip("Neue Vorlage erstellen");
templatesI18n.setCurrentRowSectionTitle("Aktuelle Zeile");
templatesI18n.setCurrentColumnSectionTitle("Aktuelle Spalte");
// ... etc. ...

EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte, i18n);
```

### Default Behavior

If no i18n is provided, all labels default to English. You can pass `null` to any setter to use the default.

---

## 8. Customizing the Toolbar

Access toolbar components to hide, disable, or modify them:

```java
// Hide/show buttons
tables.getAddTableToolbarButton().setVisible(false);
tables.getModifyTableToolbarButton().setEnabled(false);
tables.getStyleTemplatesDialogToolbarButton().setEnabled(true);

// Access UI elements
ToolbarPopover addPopover = tables.getAddTablePopup();
ToolbarSelectPopup modifyMenu = tables.getModifyTableSelectPopup();
TemplateDialog templateDialog = tables.getStyleTemplatesDialog();

// Customize popover behavior
addPopover.setAutofocus(false); // don't focus first field

// Customize dialog
templateDialog.setWidth("30rem");
templateDialog.setHeight("50vh");
```

**Accessor methods:**

| Method | Returns |
|--------|---------|
| `getAddTableToolbarButton()` | `ToolbarSwitch` for Add Table button |
| `getAddTablePopup()` | `ToolbarPopover` for rows/cols input |
| `getModifyTableToolbarButton()` | `ToolbarSwitch` for Modify Table button |
| `getModifyTableSelectPopup()` | `ToolbarSelectPopup` for row/col/cell menu |
| `getStyleTemplatesDialogToolbarButton()` | `ToolbarSwitch` for Templates button |
| `getStyleTemplatesDialog()` | `TemplateDialog` for template management |

---

## 9. Data Formats

### Delta Representation

Tables are stored in Quill Delta JSON. Each table cell is a line with a special `td` attribute:

```json
{
  "insert": "\n",
  "attributes": {
    "td": "tableId|rowId|cellId|rowspan|colspan|templateId|uniqueId"
  }
}
```

The `td` value is a pipe-separated string with 7 fields:

1. **tableId** — shared by all cells in the table (e.g., "t1")
2. **rowId** — shared by all cells in the same row (e.g., "r1")
3. **cellId** — unique per cell (e.g., "c1")
4. **rowspan** — empty for normal cells, number for merged (e.g., "2")
5. **colspan** — empty for normal cells, number for merged (e.g., "")
6. **templateId** — template ID on the FIRST cell only, empty for others
7. **uniqueId** — reserved, usually empty

**Example: 2×2 table with template "blue":**

```json
[
  {"insert": "\n", "attributes": {"td": "t1|r1|c1||||blue"}},
  {"insert": "\n", "attributes": {"td": "t1|r1|c2|||||"}},
  {"insert": "\n", "attributes": {"td": "t1|r2|c3|||||"}},
  {"insert": "\n", "attributes": {"td": "t1|r2|c4|||||"}},
  {"insert": "\n"}
]
```

**Merged cells:** The "root" cell (top-left) has rowspan/colspan set. Other cells in the merged region reference the root's cellId.

### Finding Used Templates

Scan a delta string for template IDs in use:

```java
String delta = editor.asDelta().getValue();
Set<String> usedTemplates = EnhancedRichTextEditorTables.getAssignedTemplateIds(delta);
log.info("Used templates: {}", usedTemplates);
```

This is useful for:
- Alerting users if a used template is about to be deleted
- Exporting only used templates to external storage
- Cleaning up unused templates during admin tasks

### Template JSON Schema

Templates are stored as an `ObjectNode` (Jackson 3). The structure is:

```json
{
  "templateId": {
    "name": "Display Name",
    "table": {
      "bgColor": "#fff",
      "color": "#000",
      "width": "100%",
      "height": "auto",
      "border": "2px solid black",
      "borderCells": "1px solid gray"
    },
    "rows": [
      {
        "index": "2n",
        "declarations": { "bgColor": "#f0f0f0", "height": "2rem" }
      }
    ],
    "cols": [
      {
        "index": "1",
        "declarations": { "width": "10rem" }
      }
    ],
    "cells": [
      {
        "x": 1,
        "y": 2,
        "declarations": { "bgColor": "#ffff00" }
      }
    ]
  }
}
```

---

## 10. API Quick Reference

### EnhancedRichTextEditorTables

| Method | Description |
|--------|-------------|
| `enable(rte)` | Enable tables with default i18n |
| `enable(rte, i18n)` | Enable tables with custom i18n |
| `insertTableAtCurrentPosition(rows, cols)` | Insert table at cursor |
| `insertTableAtCurrentPosition(rows, cols, templateId)` | Insert table with template |
| `setTemplates(templates)` | Set template JSON |
| `getTemplates()` | Get current templates |
| `getTemplatesAsCssString()` | Get templates as CSS |
| `setTemplateIdForCurrentTable(templateId)` | Apply template to selected table |
| `setCustomStyles(css, beforeGenerated)` | Inject custom CSS |
| `setTableHoverColor(color)` | Table border on hover |
| `setTableCellHoverColor(color)` | Cell background on hover |
| `setTableFocusColor(color)` | Table border on focus |
| `setTableCellFocusColor(color)` | Cell background on focus |
| `getAssignedTemplateIds(delta)` | (static) Find template IDs in delta |
| `add*Listener(listener)` | Register event listeners (8 types) |
| `getRte()` | Access the underlying ERTE |
| `getAddTableToolbarButton()` | Access Add Table button |
| `getAddTablePopup()` | Access Add Table popover |
| `getModifyTableToolbarButton()` | Access Modify Table button |
| `getModifyTableSelectPopup()` | Access Modify Table menu |
| `getStyleTemplatesDialogToolbarButton()` | Access Style Templates button |
| `getStyleTemplatesDialog()` | Access TemplateDialog |

### TemplateDialog

Key accessors for programmatic control:

| Method | Description |
|--------|-------------|
| `getTemplates()` / `setTemplates(...)` | Template JSON |
| `getActiveTemplateId()` / `setActiveTemplateId(...)` | Currently selected template |
| `getDefaults()` | Access Defaults (dimension unit) |
| `getTemplateSection()` | Template selection UI |
| `getTableSection()` | Table styling section |
| `getCurrentRowSection()` | Current row styling |
| `getCurrentColSection()` | Current column styling |
| `getSpecialRowsSection()` | Special rows (header, footer, etc.) |
| `getTemplateSelectionField()` | Template ComboBox |
| `getTemplateNameField()` | Template name TextField |
| `getCreateNewTemplateButton()` | Create button |
| `getCopySelectedTemplateButton()` | Copy button |
| `getDeleteSelectedTemplateButton()` | Delete button |

### TemplateParser

Static utility methods:

| Method | Description |
|--------|-------------|
| `convertToCss(ObjectNode)` | Convert templates to CSS |
| `convertToCss(String)` | Parse JSON string and convert |
| `parseJson(String)` | Parse template JSON string |
| `parseJson(String, boolean)` | Parse + optionally remove empty nodes |
| `removeEmptyChildren(ObjectNode)` | Clean empty nodes |
| `isValidTemplateId(String)` | Check if ID is valid CSS class name |
| `isValidPropertyValue(property, value)` | Validate CSS value for security |
| `clone(ObjectNode)` | Deep-copy a template |

### Defaults

Access via `table.getStyleTemplatesDialog().getDefaults()`:

| Method | Description |
|--------|-------------|
| `getDimensionUnit()` | Get default dimension unit (e.g., "rem") |
| `setDimensionUnit(String)` | Set default unit for all dimension fields |
| `addDimensionUnitChangedListener(...)` | Listen for unit changes |

---

## Common Patterns

### Save Templates to Database

```java
tables.addTemplateUpdatedListener(e -> {
    ObjectNode templates = tables.getTemplates();
    String json = templates.toString();
    database.save("table_templates", json);
});

// Also listen to create/copy/delete events
tables.addTemplateCreatedListener(e -> saveTemplates());
tables.addTemplateCopiedListener(e -> saveTemplates());
tables.addTemplateDeletedListener(e -> saveTemplates());

private void saveTemplates() {
    ObjectNode templates = tables.getTemplates();
    database.save("table_templates", templates.toString());
}
```

### Warn Before Deleting a Template

```java
tables.addTemplateDeletedListener(e -> {
    String templateId = e.getTemplateId();
    String delta = editor.asDelta().getValue();

    if (EnhancedRichTextEditorTables.getAssignedTemplateIds(delta).contains(templateId)) {
        log.warn("Warning: deleted template {} is still in use in tables", templateId);
    }
});
```

### Change Table Appearance Dynamically

```java
// Apply hover effects
tables.setTableHoverColor("var(--lumo-primary-color-50pct)");
tables.setTableCellHoverColor("var(--lumo-primary-color-10pct)");

// Apply focus effects
tables.setTableFocusColor("var(--lumo-warning-color)");
tables.setTableCellFocusColor("var(--lumo-warning-color-10pct)");

// Add global margins
tables.setCustomStyles("table { margin: 1em 0; }", true);
```

### Full i18n Setup (German Example)

```java
TablesI18n i18n = new TablesI18n();

// Toolbar
i18n.setInsertTableToolbarSwitchTooltip("Tabelle einfügen");
i18n.setInsertTableRowsFieldLabel("Zeilen");
i18n.setInsertTableColumnsFieldLabel("Spalten");
i18n.setInsertTableAddButtonTooltip("Tabelle hinzufügen");
i18n.setModifyTableToolbarSwitchTooltip("Tabelle ändern");
i18n.setModifyTableAppendRowAboveItemLabel("Zeile oben einfügen");
i18n.setModifyTableAppendRowBelowItemLabel("Zeile unten einfügen");
i18n.setModifyTableRemoveRowItemLabel("Zeile löschen");
i18n.setModifyTableAppendColumnBeforeItemLabel("Spalte links einfügen");
i18n.setModifyTableAppendColumnAfterItemLabel("Spalte rechts einfügen");
i18n.setModifyTableRemoveColumnItemLabel("Spalte löschen");
i18n.setModifyTableMergeCellsItemLabel("Zellen zusammenführen");
i18n.setModifyTableSplitCellItemLabel("Zelle teilen");
i18n.setModifyTableRemoveTableItemLabel("Tabelle löschen");
i18n.setTableTemplatesToolbarSwitchTooltip("Tabellenvorlagen");

// Templates dialog
TablesI18n.TemplatesI18n templatesI18n = i18n.getTemplatesI18n();
templatesI18n.setDialogTitle("Tabellenvorlagen");
templatesI18n.setCurrentTemplateSelectFieldLabel("Aktuelle Vorlage");
templatesI18n.setCreateNewTemplateButtonTooltip("Neue Vorlage erstellen");
templatesI18n.setCurrentRowSectionTitle("Aktuelle Zeile");
templatesI18n.setCurrentColumnSectionTitle("Aktuelle Spalte");
// ... etc. ...

EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte, i18n);
```

