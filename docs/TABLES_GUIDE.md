# ERTE Tables Extension Guide

**For developers who need to add table support to their ERTE-based applications.** This guide covers everything from setup through advanced customization. No prior experience with Quill tables required.

**Related documentation:** Extend the ERTE core component with [EXTENDING.md](dev/EXTENDING.md). Use I18n across your app with [CONTRIBUTING.md](dev/CONTRIBUTING.md) patterns.

---

## 1. Getting Started

### What You Get

Three toolbar buttons:

- **Add Table** — Insert new table (rows & columns)
- **Modify Table** — Add/remove rows/columns, merge/split cells, delete table
- **Style Templates** — Apply named styling rules (colors, borders, dimensions)

Plus events when tables/cells are modified or templates change, so your app can react.

### Installation

Add this to your `pom.xml`:

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-tables</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Requirements:** Java 21+, Vaadin 25.0.x, `enhanced-rich-text-editor` 6.0.0

### Quickstart

```java
EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte);
add(rte);
```

The three buttons appear automatically in the toolbar.

---

## 2. Toolbar Components

### The Three Buttons

**Add Table** — Opens a popover with Rows/Columns inputs (default: 3×3, range: 1–20). Click the plus button to insert at cursor. Disabled inside tables (no nesting).

**Modify Table** — Enabled when a table is selected. Menu items: append/remove rows, append/remove columns, merge cells (multi-select only), split cell, remove table.

**Style Templates** — Opens a dialog for template management (see below). Disabled when no table selected.

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

Through the UI: click **Add Table** and enter dimensions.

Programmatically:

```java
tables.insertTableAtCurrentPosition(4, 3); // 4 rows, 3 columns
tables.insertTableAtCurrentPosition(4, 3, "myTemplate"); // with template
```

Template ID must be a valid CSS class name. If it doesn't exist, the table inserts without styling.

### Modifying Tables

Use the **Modify Table** menu. Operations work on the currently selected table (where cursor is). The UI updates automatically when you add/remove rows/columns or merge cells.

### Cell Selection

**Ctrl+click** individual cells or **Ctrl+drag** for a range. Selected cells get `ql-cell-selected` class (light background). **Merge cells** menu item only enables with multiple cells selected. Click without Ctrl to clear selection.

### Removing Tables

Click **Modify Table** → **Remove table**.

---

## 4. Style Templates

### What Are Templates?

Named collections of styling rules. Target table-level properties, specific rows (header, footer, even, odd, or numbered), columns (numbered), or cells (x/y coordinates). Define once, apply to multiple tables.

### How to Use Templates

**Load from JSON:**
```java
ObjectNode templates = TemplateParser.parseJson(jsonString);
tables.setTemplates(templates);
```

**Apply via UI:** Select a table, click **Style Templates**, choose template from dropdown.

**Programmatic application:**
```java
tables.setTemplateIdForCurrentTable("myTemplate"); // for selected table
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

8 event types fire on table and template changes. Register on the `EnhancedRichTextEditorTables` instance.

### Table Selection Events

**TableSelectedEvent** — cursor enters/leaves a table

```java
tables.addTableSelectedListener(e -> {
    if (e.isSelected()) log.info("Table selected: {}", e.getTemplate());
    if (e.isCellSelectionActive()) log.info("Multiple cells selected");
});
```
- `isSelected()` — table now selected
- `isCellSelectionActive()` — multiple cells selected
- `getTemplate()` — template ID (or null)

**TableCellChangedEvent** — cursor moves to different cell

```java
tables.addTableCellChangedListener(e -> {
    if (e.getRowIndex() != null) log.info("Row: {}, Col: {}", e.getRowIndex(), e.getColIndex());
});
```

### Template Events

All extend `TemplateModificationEvent`. Fire on user interaction or code:

**TemplatesInitializedEvent**
```java
tables.addTemplatesInitializedListener(e -> {
    log.info("Loaded {} templates", e.getTemplates().size());
});
```

**TemplateCreatedEvent** — user clicked Create

**TemplateCopiedEvent** — user clicked Copy

**TemplateUpdatedEvent** — name or rule changed

**TemplateDeletedEvent** — user confirmed deletion

**TemplateSelectedEvent** — active template changed for table

All provide `getTemplateId()`. Common pattern:

```java
tables.addTemplateCreatedListener(e -> saveTemplatesToDatabase(tables.getTemplates()));
tables.addTemplateUpdatedListener(e -> saveTemplatesToDatabase(tables.getTemplates()));
tables.addTemplateDeletedListener(e -> saveTemplatesToDatabase(tables.getTemplates()));
```

### Event Registration

Every `add*Listener()` returns a `Registration` to unregister:

```java
Registration reg = tables.addTableSelectedListener(e -> { /* ... */ });
reg.remove();
```

---

## 6. Theming & Styling

### CSS Custom Properties

11 CSS custom properties for styling. Set on the ERTE component:

```css
vcf-enhanced-rich-text-editor {
  /* Borders & Padding */
  --vaadin-erte-table-border-color: #e0e0e0;
  --vaadin-erte-table-border-width: 1px;
  --vaadin-erte-table-border-style: solid;
  --vaadin-erte-table-cell-padding: 2px 5px;
  --vaadin-erte-table-cell-min-height: 1.625em;
  --vaadin-erte-table-cell-background: transparent;
  --vaadin-erte-table-cell-vertical-align: top;

  /* Selection & Focus */
  --vaadin-erte-table-cell-selected-background: var(--lumo-primary-color-10pct);
  --vaadin-erte-table-cell-hover-background: transparent;
  --vaadin-erte-table-cell-focus-color: var(--vaadin-focus-ring-color);
  --vaadin-erte-table-cell-focus-width: 2px;
}
```

### Programmatic Color Control

Set hover/focus colors at runtime:

```java
tables.setTableHoverColor("var(--lumo-primary-color)"); // table border
tables.setTableCellHoverColor("var(--lumo-primary-color-10pct)"); // cell bg
tables.setTableFocusColor("var(--lumo-warning-color)"); // table border
tables.setTableCellFocusColor("var(--lumo-warning-color-10pct)"); // cell bg
tables.setTableHoverColor(null); // disable
```

Accepted formats: hex, named colors, `rgb()`/`rgba()`, `hsl()`/`hsla()`, CSS variables. Invalid colors throw `IllegalArgumentException`.

### Custom Styling

Focused cell gets `focused-cell` class:
```css
vcf-enhanced-rich-text-editor::part(editor) table td.focused-cell {
  outline: 2px solid red;
}
```

Selected cells (multi-select) get `ql-cell-selected` class. High contrast mode supported.

---

## 7. Internationalization (i18n)

Customize all toolbar labels and dialog text:

```java
TablesI18n i18n = new TablesI18n();
i18n.setInsertTableToolbarSwitchTooltip("Tabelle einfügen");
i18n.setInsertTableRowsFieldLabel("Zeilen");
i18n.setInsertTableColumnsFieldLabel("Spalten");
// ... more setters ...

// Template dialog
TablesI18n.TemplatesI18n templatesI18n = i18n.getTemplatesI18n();
templatesI18n.setDialogTitle("Tabellenvorlagen");
templatesI18n.setCurrentTemplateSelectFieldLabel("Aktuelle Vorlage");
// ... more setters ...

EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte, i18n);
```

Setter names follow the pattern `set[Component][Property](String)`. Use IDE autocomplete to discover them. Default: English. Pass `null` to any setter to use default.

---

## 8. Customizing the Toolbar

Access and modify toolbar components:

```java
tables.getAddTableToolbarButton().setVisible(false);
tables.getModifyTableToolbarButton().setEnabled(false);

ToolbarPopover addPopover = tables.getAddTablePopup();
addPopover.setAutofocus(false);

TemplateDialog templateDialog = tables.getStyleTemplatesDialog();
templateDialog.setWidth("30rem");
templateDialog.setHeight("50vh");
```

**Accessor methods:**

| Method | Returns |
|--------|---------|
| `getAddTableToolbarButton()` | `ToolbarSwitch` for Add Table |
| `getAddTablePopup()` | `ToolbarPopover` for rows/cols |
| `getModifyTableToolbarButton()` | `ToolbarSwitch` for Modify Table |
| `getModifyTableSelectPopup()` | `ToolbarSelectPopup` for menu |
| `getStyleTemplatesDialogToolbarButton()` | `ToolbarSwitch` for Templates |
| `getStyleTemplatesDialog()` | `TemplateDialog` for management |

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
// Listen to all template changes
tables.addTemplateCreatedListener(e -> saveTemplates());
tables.addTemplateUpdatedListener(e -> saveTemplates());
tables.addTemplateCopiedListener(e -> saveTemplates());
tables.addTemplateDeletedListener(e -> saveTemplates());

private void saveTemplates() {
    database.save("table_templates", tables.getTemplates().toString());
}
```

### Warn Before Deleting a Template

```java
tables.addTemplateDeletedListener(e -> {
    if (EnhancedRichTextEditorTables.getAssignedTemplateIds(
            editor.asDelta().getValue()).contains(e.getTemplateId())) {
        log.warn("Template {} still in use", e.getTemplateId());
    }
});
```

### Dynamic Table Styling

```java
tables.setTableHoverColor("var(--lumo-primary-color-50pct)");
tables.setTableCellHoverColor("var(--lumo-primary-color-10pct)");
tables.setTableFocusColor("var(--lumo-warning-color)");
tables.setCustomStyles("table { margin: 1em 0; }", true); // before
```

