# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Enhanced Rich Text Editor (ERTE) for Vaadin - a rich text editor component extending Vaadin's RTE with tabstops, non-breaking space, rulers, customizable toolbar, and read-only sections. Built on Quill.js v1.3.6.

## Build Commands

```bash
# Full build (skip tests for faster iteration)
mvn clean install -DskipTests

# Run demo locally (Spring Boot)
mvn -pl enhanced-rich-text-editor-demo spring-boot:run
# Then visit: http://127.0.0.1:8080/enhanced-rich-text-editor

# Run tests
mvn verify

# Production build
mvn clean package -Pproduction -DskipTests
```

## Project Structure

Multi-module Maven project with Java 17, Vaadin 24.9.7, Spring Boot 3.5.9:

- **enhanced-rich-text-editor/** - Core component wrapping Quill.js as a Vaadin web component (`<vcf-enhanced-rich-text-editor>`)
- **enhanced-rich-text-editor-tables/** - Separate addon extending ERTE with table functionality (forked from quill1-table)
- **enhanced-rich-text-editor-demo/** - Spring Boot demo application with React frontend

## Key Classes

- `EnhancedRichTextEditor.java` - Main server-side Vaadin component
- `TabStop.java` - Tab stop positions with Direction enum (LEFT, RIGHT, MIDDLE)
- `ToolbarSlot.java` - Enumeration for toolbar customization positions
- `EnhancedRichTextEditorTables.java` - Tables extension, enabled via `EnhancedRichTextEditorTables.enable(rte)`

## Architecture Notes

- **Extension pattern**: Tables addon extends ERTE without subclassing - just call `EnhancedRichTextEditorTables.enable(rte)`
- **Quill Delta format**: Content stored as Quill Delta JSON. Tables use a special attribute format with pipe-separated cell metadata
- **Style templates**: Tables support JSON-based style templates that convert to CSS. Use `TemplateParser` for parsing/conversion
- **Toolbar customization**: Use `setToolbarButtonsVisibility()` and `addToolbarComponents(ToolbarSlot, Component...)` for toolbar modifications

## Testing

Tests use JUnit 4, Mockito, and PowerMock. Vaadin TestBench available for browser testing.

```bash
# Run all tests
mvn test

# Run single test class
mvn -pl enhanced-rich-text-editor test -Dtest=RichTextEditorTest
```

## License

CVALv3 (Commercial Vaadin Add-On License). License headers are enforced on Java files.
