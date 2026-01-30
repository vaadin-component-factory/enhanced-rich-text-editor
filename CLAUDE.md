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

# Run single test class
mvn -pl enhanced-rich-text-editor test -Dtest=RichTextEditorTest

# Production build
mvn clean package -Pproduction -DskipTests
```

## Project Structure

Multi-module Maven project with Java 17, Vaadin 24.9.7, Spring Boot 3.5.9:

- **enhanced-rich-text-editor/** - Core component: Vaadin web component (`<vcf-enhanced-rich-text-editor>`) wrapping Quill.js
- **enhanced-rich-text-editor-tables/** - Tables addon extending ERTE (forked from quill1-table)
- **enhanced-rich-text-editor-demo/** - Spring Boot demo application

## Architecture

### Web Component Layer (JavaScript)
The editor is a Polymer-based web component in `enhanced-rich-text-editor/src/main/resources/META-INF/resources/frontend/src/`:

- `vcf-enhanced-rich-text-editor.js` - Main web component extending PolymerElement with ThemableMixin
- `vcf-enhanced-rich-text-editor-blots.js` - Custom Quill blots: ReadOnlyBlot, TabBlot, PreTabBlot, TabsContBlot, LinePartBlot, PlaceholderBlot
- `vendor/` - Standard Quill.js code (**ignore** - not project-specific)

Blots are registered with Quill in a specific order via `Inline.order.push()`.

### Java Server Component
- `EnhancedRichTextEditor.java` - Server-side component using `@Tag("vcf-enhanced-rich-text-editor")`, communicates via `runBeforeClientResponse()` pattern
- `GeneratedEnhancedRichTextEditor.java` - Generated base class with property bindings

### Extension Pattern
Tables addon uses composition rather than inheritance:
```java
EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte);
```
The extension adds toolbar buttons, registers additional Quill blots (`TableBlot`, `TableCellBlot`, `TableRowBlot`), and handles table-specific events.

### Data Format
Content is stored as Quill Delta JSON. Tables encode cell metadata in a pipe-separated format within the `td` attribute:
`{tableId}|{rowId}|{cellId}|{rowspan}|{colspan}|{templateId}|{uniqueId}`

### Toolbar Customization
- Hide/show buttons: `setToolbarButtonsVisibility(Map<ToolbarButton, Boolean>)`
- Add custom components: `addToolbarComponents(ToolbarSlot.START, component)`
- Slots: START, END, BEFORE_GROUP_*, AFTER_GROUP_*, CUSTOM (legacy)

## Key Classes

- `EnhancedRichTextEditor.java` - Main server-side component
- `TabStop.java` - Tabstop position and alignment (LEFT, RIGHT, MIDDLE directions)
- `ToolbarSlot.java` - Toolbar slot positions for custom components
- `EnhancedRichTextEditorTables.java` - Tables extension
- `TemplateParser.java` - Converts JSON style templates to CSS for tables

## Playwright Tests (Tab-Stop Prototype)

The demo module includes Playwright tests for the tab-stop prototype feature.
See [prototype_tests.md](enhanced-rich-text-editor-demo/prototype_tests.md) for detailed test documentation.

```bash
# Run Playwright tests (server must be running)
cd enhanced-rich-text-editor-demo
npx playwright test

# Run with UI for debugging
npx playwright test --ui
```

**Important:** Before running Playwright tests, ensure:
1. Build with `mvn clean package -DskipTests`
2. Start server with `mvn -pl enhanced-rich-text-editor-demo spring-boot:run`

### Test Summary (75 Tests)

| Category | Tests | Description |
|----------|-------|-------------|
| **Hard-Break (Enter)** | 2 | New paragraph creation, tabs don't copy |
| **Soft-Break (Shift+Enter)** | 6 | Tab copying based on cursor position |
| **Automatic Wrap** | 2 | Fixed width for wrapped lines |
| **Combined Scenarios** | 3 | Mixed hard/soft breaks with tabs |
| **Tab Alignment** | 5 | L/C/R tabstops + overflow tabs (fixed width) |
| **Overflow Tabs + Soft-Break** | 3 | More tabs than tabstops with soft-breaks |
| **Ruler/Tabstop Manipulation** | 4 | Add/remove/cycle tabstops via ruler |
| **Edge Cases** | 5 | Empty lines, many tabs, alternating patterns |
| **Mixed Break Types** | 4 | Complex break sequences, copy-paste |
| **Stress Tests** | 2 | Rapid operations, all tabstops removed |
| **Undo/Redo** | 4 | Undo operations for tabs and soft-breaks |
| **Selection Operations** | 3 | Select, delete, replace across soft-breaks |
| **Backspace/Delete** | 3 | Delete at boundaries and tab characters |
| **Tab After Soft-Break** | 2 | Tab insertion on new visual lines |
| **Formatted Text** | 2 | Bold/italic preserved with tabs |
| **Cursor Navigation** | 3 | Arrow keys and selection around tabs |
| **Multiple Paragraphs** | 2 | Independent soft-breaks per paragraph |
| **Overflow Tab Limit** | 4 | Customer requirement: limit copies to tabstop count |
| **Browser Resize** | 1 | Tab recalculation after viewport change |
| **Focus/Blur** | 1 | Tab visibility after focus changes |
| **Tab at Tabstop Boundary** | 1 | Consecutive tabs positioning |
| **Empty Visual Line** | 1 | Soft-break on empty visual line |
| **Whitespace Indicators** | 12 | Show/hide whitespace symbols (→↵¶↲), legend |

### Key Soft-Break Behavior
- Soft-break inserts at end of visual line (line stays intact)
- Tabs are copied from visual line start to cursor position
- **Tabs copied are limited to the number of defined tabstops** (customer requirement)
- Multiple soft-breaks maintain consistent tab count per visual line
- Works correctly with more tabs than defined tabstops

## License

CVALv3 (Commercial Vaadin Add-On License). License headers are enforced on Java files.
