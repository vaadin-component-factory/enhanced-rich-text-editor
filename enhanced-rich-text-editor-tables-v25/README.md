# Enhanced Rich Text Editor Tables Extension V25

Tables extension for the Enhanced Rich Text Editor (ERTE) on Vaadin 25. Adds table
creation, modification, cell merging, and style templates to the ERTE.

## Status

> **Phase 4 has not yet started.** This module exists as a scaffold only. The V25
> tables migration (Quill 2 / Parchment 3 blot rewrite) is planned but not yet
> implemented.
>
> For the current V24 tables feature set, see the
> [V24 Tables README](../enhanced-rich-text-editor-tables/README.md).

## Maven Dependency

```xml
<dependency>
    <groupId>org.vaadin.addons.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-tables-v25</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

## Requirements

- Java 21+
- Vaadin 25.0.x
- `enhanced-rich-text-editor-v25` 6.0.0-SNAPSHOT

## Planned Features

- Table creation and modification (rows, columns, merge/split cells)
- Style templates (named, reusable table styling rules)
- Cell selection (Ctrl+Click)
- Table events (selected, cell changed, template CRUD)
- I18n support for all table-related labels

## License

[CVALv3](https://vaadin.com/license/cval-3) (Commercial Vaadin Add-On License)
