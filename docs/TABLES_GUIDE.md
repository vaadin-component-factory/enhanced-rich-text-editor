# ERTE Tables Extension Guide

Everything you need to add table support to your ERTE editor — from a three-line quickstart to advanced template styling. No prior experience with Quill tables required.

**Related:** [EXTENDING.md](dev/EXTENDING.md) for extending the ERTE core, [User Guide](BASE_USER_GUIDE.md) for core ERTE features.

---

## Table of Contents

- [1. Getting Started](#1-getting-started)
- [2. Toolbar Components](#2-toolbar-components)
- [3. Table Operations](#3-table-operations)
- [4. Style Templates](#4-style-templates)
  - [How to Use Templates](#how-to-use-templates)
  - [Template JSON Structure](#template-json-structure)
  - [Row and Column Index Patterns](#row-and-column-index-patterns)
  - [Style Properties](#style-properties)
  - [The Templates Dialog](#the-templates-dialog)
  - [Dimension Units](#dimension-units)
  - [Injecting Custom CSS](#injecting-custom-css)
- [5. Events](#5-events)
- [6. Theming & Styling](#6-theming--styling)
- [7. Internationalization (i18n)](#7-internationalization-i18n)
- [8. Data Formats](#8-data-formats)
- [9. API Quick Reference](#9-api-quick-reference)
- [10. Common Patterns](#10-common-patterns)

---

## 1. Getting Started

### What You Get

Enabling the tables addon gives you three toolbar buttons out of the box: **Add Table** (insert new table), **Modify Table** (rows/columns/merge/delete), and **Style Templates** (apply styling rules). You also get event listeners for table selection, cell changes, and template modifications.

### Installation

Add this to your `pom.xml`:

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-tables</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Requirements:** Vaadin 25.0.x, `enhanced-rich-text-editor` 6.0.0

### Quickstart

```java
EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte);
add(rte);
```

That's it — the three toolbar buttons appear automatically. Everything else in this guide is optional.

---

## 2. Toolbar Components

### Buttons

The Tables addon automatically adds toolbar buttons to create and modify tables and their styling.

| Button | Behavior |
|--------|----------|
| **Add Table** | Popover with row/column inputs (default 3×3, 1–20 range). Insert at cursor. Disabled inside tables. |
| **Modify Table** | Menu: append/remove rows/columns, merge, split, delete. Enabled when cursor is inside a table. |
| **Style Templates** | Dialog for template management. Enabled when cursor is inside a table. |

### Customization

You can access and modify these buttons programmatically:

```java
tables.getAddTableToolbarButton().setVisible(false); // hide Add Table button
tables.getModifyTableToolbarButton().setEnabled(false); // disable Modify Table button

// Access the popup for advanced configuration
ToolbarPopover addPopover = tables.getAddTablePopup();
addPopover.setAutofocus(false); // don't focus first field on open

// Access the dropdown menu for Modify Table
ToolbarSelectPopup modifyMenu = tables.getModifyTableSelectPopup();
modifyMenu.getItems(); // list of all menu items
```

For details on toolbar component types (`ToolbarPopover`, `ToolbarSelectPopup`, etc.), see the [User Guide](BASE_USER_GUIDE.md#21-toolbar-customization). For a full list of accessor methods, see the [API Quick Reference](#9-api-quick-reference).

---

## 3. Table Operations

Your users interact with tables through the toolbar buttons, but you can also drive everything from Java.

**Insert:** Click the Add Table button or call `tables.insertTableAtCurrentPosition(rows, cols)`. You can also pass a template ID to apply styling immediately: `insertTableAtCurrentPosition(rows, cols, templateId)`.

**Modify:** When the cursor is inside a table, the Modify Table menu offers append/remove rows and columns, merge and split cells, and delete the entire table.

**Select cells:** Hold Ctrl and click individual cells, or Ctrl+drag to select a range. Selected cells get the `ql-cell-selected` CSS class. Merge is only available when multiple cells are selected.

**Remove:** Use the Modify Table menu → **Remove table**.

---

## 4. Style Templates

Templates let you define a visual style once — borders, colors, alternating rows — and apply it to any table with a single click. Think of them as CSS classes for your tables.

### How to Use Templates

**Load from JSON:**
```java
ObjectNode templates = TemplateParser.parseJson(jsonString);
tables.setTemplates(templates);
```

> **Note:** `ObjectNode` is `tools.jackson.databind.node.ObjectNode` (Jackson 3, shipped with Vaadin 25) — not the familiar `com.fasterxml.jackson` package.

**Apply via UI:** Select a table, click **Style Templates**, choose template from dropdown.

**Programmatic application:**
```java
tables.setTemplateIdForCurrentTable("myTemplate"); // for selected table
```

### Template JSON Structure

Each template is keyed by its **template ID**, which is applied as a CSS class to the `<table>` element. The ID must be a valid CSS class name — use `TemplateParser.isValidTemplateId()` to check.

Here's a complete template example showing all available options. You only need to include the properties relevant to your use case — `table` properties are the minimum.

The `rows` and `cols` entries use an `index` field with CSS `nth-child()` syntax to select which rows/columns to style (e.g., `"0n+1"` = first only, `"2n"` = every even row). See [Row and Column Index Patterns](#row-and-column-index-patterns) below for the full syntax.

**Cell coordinates in `cells`:** `x` is the row position (1-based, maps to `tr:nth-of-type()`), `y` is the column position (1-based, maps to `td:nth-of-type()`). So `"x": 1, "y": 1` targets the first cell in the first row. **Note:** This is opposite to the typical `x=horizontal, y=vertical` convention — here `x` means row and `y` means column.

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

### Row and Column Index Patterns

The `index` field in `rows` and `cols` entries follows CSS `nth-child()` formula syntax (`An+B`):

| Pattern | Meaning | Selects |
|---------|---------|---------|
| `"0n+1"` | First row only | Header row |
| `"1"` | First row only | Same selection as `"0n+1"`, but generates a more specific CSS rule (appears later in the stylesheet, so it wins on conflicts) |
| `"2n"` | Every even row | Even rows (2nd, 4th, 6th…) |
| `"2n+1"` | Every odd row | Odd rows (1st, 3rd, 5th…) |
| `"3"` | Third row only | A specific single row |

Add `"last": true` to a row entry to count from the bottom instead of the top — useful for footer rows. For example, `{"index": "0n+1", "last": true}` targets the last row only.

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

### Dimension Units

Set default unit (default: `rem`):
```java
tables.getStyleTemplatesDialog().getDefaults().setDimensionUnit("px");
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

ERTE Tables fires events when a table gets selected, the user moves to a different cell, or a template changes. All listeners return a `Registration` for cleanup, following the standard Vaadin pattern.

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

**TemplatesInitializedEvent** — fired after `setTemplates()`. Provides `getTemplates()` and `getCssString()`.
```java
tables.addTemplatesInitializedListener(e -> {
    log.info("Loaded {} templates", e.getTemplates().size());
});
```

The following modification events extend `TemplateModificationEvent` and all provide `getTemplateId()` and `getTemplate()` (the full template JSON):

**TemplateCreatedEvent** — user clicked Create

**TemplateCopiedEvent** — user clicked Copy

**TemplateUpdatedEvent** — name or rule changed

**TemplateDeletedEvent** — user confirmed deletion

**TemplateSelectedEvent** — active template changed for table. This event extends `EnhancedRichTextEditorTablesComponentEvent` directly (not `TemplateModificationEvent`). It provides `getTemplateId()` but not `getTemplate()`.

Common pattern:

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

This section covers **global** table styling — the default look that applies to every table in the editor. CSS custom properties control borders, padding, and selection colors for all tables uniformly.

For **per-table** styling — giving individual tables their own colors, borders, and row patterns — use [Style Templates](#4-style-templates) instead. Templates are applied to specific tables and override the global defaults defined here.

### CSS Custom Properties

ERTE Tables provides CSS custom properties for borders, padding, colors, and selection styling:

```css
vcf-enhanced-rich-text-editor {
  /* Borders & Padding */
  --vaadin-erte-table-border-color: var(--vaadin-border-color, var(--lumo-contrast-30pct));
  --vaadin-erte-table-border-width: 1px;
  --vaadin-erte-table-border-style: solid;
  --vaadin-erte-table-cell-padding: 2px 5px;
  --vaadin-erte-table-cell-min-height: 1.625em;
  --vaadin-erte-table-cell-background: transparent;
  --vaadin-erte-table-cell-vertical-align: top;

  /* Selection & Focus */
  --vaadin-erte-table-cell-selected-background: var(--lumo-primary-color-10pct, rgba(25, 118, 210, 0.1));
  --vaadin-erte-table-cell-hover-background: transparent;
  --vaadin-erte-table-cell-focus-color: var(--vaadin-focus-ring-color, var(--lumo-primary-color-50pct));
  --vaadin-erte-table-cell-focus-width: var(--vaadin-focus-ring-width, 2px);
}
```

### Programmatic Color Control

Besides styling and templates, you can also set hover and focus colors for different parts of the table. These methods apply to the specific `EnhancedRichTextEditorTables` instance — each editor on the page can have its own values. This allows you to give the user visual feedback about table interactions without the need for explicit stylesheets.

We recommend highlighting on hover or focus when the table itself gives no visual clues by itself (e.g. when the borders are transparent).

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

All user-facing labels and tooltips are customizable. Here's a German example:

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

## 8. Data Formats

### Delta Representation

Under the hood, tables are stored as Quill Delta JSON. Each cell line carries a `td` attribute with pipe-separated metadata (7 fields):

`tableId|rowId|cellId|mergeId|colspan|rowspan|tableClass`

- **Unmerged cells:** `mergeId`, `colspan`, and `rowspan` are empty (the runtime reads empty as `1`)
- **Merged cells (root):** The top-left cell carries the actual `colspan`/`rowspan` values
- **Merged cells (non-root):** Their `mergeId` references the root cell's `cellId`; their own `colspan`/`rowspan` are empty
- **Table class:** The 7th field is the CSS class of the `<table>` element (equals the template ID when one is applied). Stored on the first cell of each table only.

You usually don't need to work with this directly — the Java API handles it for you. But it's useful to know if you're building custom delta processors (e.g., for PDF export).

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

Templates are stored as an `ObjectNode` (Jackson 3). See [Section 4 — Style Templates](#4-style-templates) for the complete JSON structure, row index patterns, and supported style properties.

---

## 9. API Quick Reference

A quick overview of what's available. For full details, check the Javadoc in the source.

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
| `getActiveTemplateId()` / `setActiveTemplateId(...)` | Currently selected template. Getter returns `Optional<String>` — use `.orElse(null)` or `getActiveTemplateIdOrThrow()` |
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

Access via `tables.getStyleTemplatesDialog().getDefaults()`:

| Method | Description |
|--------|-------------|
| `getDimensionUnit()` | Get default dimension unit (e.g., "rem") |
| `setDimensionUnit(String)` | Set default unit for all dimension fields |
| `addDimensionUnitChangedListener(...)` | Listen for unit changes |

---

## 10. Common Patterns

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

### Detect Deletion of In-Use Templates

The `TemplateDeletedEvent` fires after a template has been deleted. Use it to detect when a template that is still assigned to tables was removed:

```java
tables.addTemplateDeletedListener(e -> {
    if (EnhancedRichTextEditorTables.getAssignedTemplateIds(
            editor.asDelta().getValue()).contains(e.getTemplateId())) {
        log.warn("Deleted template {} was still in use", e.getTemplateId());
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

