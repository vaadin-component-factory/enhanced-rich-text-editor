# Enhanced Rich Text Editor Tables Extension V25

Add professional table support to your ERTE documents: create tables, modify structure (rows, columns, merge/split cells), and apply reusable style templates.

## Features

- **Table Creation** — Insert tables with custom row/column counts via toolbar
- **Table Modification** — Append/remove rows and columns, merge/split cells, remove entire tables
- **Cell Selection** — Ctrl+click or Ctrl+drag to select multiple cells for merging
- **Style Templates** — Define named, reusable styling rules for table appearance (colors, borders, dimensions)
- **Template Dialog** — Visual editor for creating and modifying templates
- **Events** — React to table selection, cell changes, and template modifications
- **I18n Support** — Customize all toolbar labels and dialog text for any language
- **Keyboard Navigation** — Arrow keys and Tab navigate between cells
- **Theming** — 11 CSS custom properties for fine-grained control over table appearance

## Quick Start

**Add the dependency:**

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-tables-v25</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

**Enable tables on your editor:**

```java
EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte);
add(rte);
```

Three toolbar buttons appear automatically. Users can now insert and modify tables.

## Requirements

- Java 21+
- Vaadin 25.0.x
- `enhanced-rich-text-editor-v25` 6.0.0-SNAPSHOT

## Documentation

- **[TABLES_GUIDE.md](./docs/TABLES_GUIDE.md)** — Complete developer guide with API reference, examples, and patterns
- **[TABLES_UPGRADE_GUIDE.md](./docs/TABLES_UPGRADE_GUIDE.md)** — Migration guide from Tables V1 (Vaadin 24) to V2

## What's New in V2

- **Quill 2 / Parchment 3** — Complete rewrite of blots for Quill 2 compatibility
- **Jackson 3** — Uses modern Jackson 3 (`tools.jackson`) instead of Vaadin's elemental JSON
- **Better Validation** — Color and template ID validation with clear error messages
- **CSS Custom Properties** — 11 variables for theming table appearance
- **Improved Keyboard Navigation** — Better arrow key support for cell selection
- **Forced-Colors Mode** — Accessible in Windows High Contrast Mode

## License

[CVALv3](https://vaadin.com/license/cval-3) (Commercial Vaadin Add-On License)

## Contributing

Contributions welcome. See [CONTRIBUTING.md](../docs/dev/CONTRIBUTING.md) for guidelines.
