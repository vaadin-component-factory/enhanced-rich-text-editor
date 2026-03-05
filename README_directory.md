# Enhanced Rich Text Editor for Flow

Enhanced Rich Text Editor (ERTE) for Flow is an extended version of the Vaadin Rich Text Editor with tabstops, placeholders, non-breaking space, rulers, customizable toolbar, readonly sections, whitespace indicators, and more.

**Version 6.x** targets Vaadin 25.0.x, Java 21+, Spring Boot 4.x, and Quill 2.

**License:** [CVALv3](https://vaadin.com/license/cval-3) (Commercial Vaadin Add-On License)

## Features

| Feature | Description |
|---------|-------------|
| **Tabstops** | Left/Right/Middle alignment with pixel-precise positioning |
| **Rulers** | Horizontal and vertical rulers with click-to-add tabstops |
| **Placeholders** | Embedded tokens with dialog, formatting, and alt appearance |
| **Readonly Sections** | Inline content protection with delete prevention |
| **Toolbar Customization** | Named slots, button visibility controls, custom keyboard shortcuts |
| **Toolbar Icon Replacement** | Replace any standard button icon with Vaadin icons |
| **Non-Breaking Space** | Shift+Space inserts non-breaking space |
| **Soft-Break** | Shift+Enter inserts line break within paragraph |
| **Whitespace Indicators** | Visual display of tabs, breaks, paragraphs, and wraps |
| **Align Justify** | Additional justify alignment option |
| **Extension Hooks** | Register custom Quill formats and modules |
| **I18n** | Full internationalization for all ERTE-specific labels |
| **Sanitization** | Server-side HTML sanitizer with XSS protection |
| **Programmatic Text** | Insert text at cursor or position, query length |


## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor</artifactId>
    <version>6.0.0</version>
</dependency>
```

> **Note:** Vaadin 25 moved the Rich Text Editor from `vaadin-core` to the commercial `vaadin` artifact. A Vaadin Pro subscription or higher is required for production use.

### Basic Usage

```java
EnhancedRichTextEditor editor = new EnhancedRichTextEditor();

// Configure tabstops
editor.setTabStops(List.of(
    new TabStop(TabStop.Direction.LEFT, 150),
    new TabStop(TabStop.Direction.RIGHT, 350),
    new TabStop(TabStop.Direction.MIDDLE, 550)
));

// Hide unwanted toolbar buttons
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.IMAGE, false,
    EnhancedRichTextEditor.ToolbarButton.CODE_BLOCK, false
));

// Set and get HTML content
editor.setValue("<p>Hello, world!</p>");
String html = editor.getValue();

// Listen for changes (fires on blur)
editor.addValueChangeListener(e -> save(e.getValue()));
```
## Documentation

- [User Guide](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/blob/master/docs/BASE_USER_GUIDE.md) — Features, examples, and best practices
- [Upgrade Guide](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/blob/master/docs/BASE_UPGRADE_GUIDE.md) — Migrating from ERTE 1 (Vaadin 24) to ERTE 2 (Vaadin 25)
- [Tables Guide](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/blob/master/docs/TABLES_GUIDE.md) — Table support: setup, templates, events, and theming
- [Tables Upgrade Guide](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/blob/master/docs/TABLES_UPGRADE_GUIDE.md) — Migrating Tables addon from V1 to V2
- [Architecture](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/blob/master/docs/dev/ARCHITECTURE.md) — Internal structure and design decisions
- [Developer Guide](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/blob/master/docs/dev/DEVELOPER_GUIDE.md) — Building, running, and testing from source
- [Extending ERTE](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/blob/master/docs/dev/EXTENDING.md) — Building custom blots and extensions

Or visit [GitHub](https://github.com/vaadin-component-factory/enhanced-rich-text-editor) for more infos.

## Tables

To enable table functionality, add the separate Tables addon:

```xml
<dependency>
    <groupId>org.vaadin.addons.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-tables</artifactId>
    <version>2.0.0</version>
</dependency>
```

```java
EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte);
```

See the [Tables Guide](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/blob/master/docs/TABLES_GUIDE.md) for full documentation, and the [Tables Upgrade Guide](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/blob/master/docs/TABLES_UPGRADE_GUIDE.md) for V1 → V2 migration.

## Running the Demo

```bash
# Build all modules
bash build.sh

# Start the demo server on port 8080
bash server-start.sh

# Browse to http://localhost:8080

# Stop the server when done
bash server-stop.sh
```

## Running Tests

ERTE tests use Playwright and run against a dedicated IT server (port 8081):

```bash
# Build including IT module
bash build-it.sh

# Start the IT server
bash it-server-start.sh

# Run Playwright tests
cd enhanced-rich-text-editor-it
npx playwright test tests/erte/

# Stop the IT server when done
bash it-server-stop.sh
```

## Project Structure

| Module | Description |
|--------|-------------|
| `enhanced-rich-text-editor/` | Core ERTE addon (Java + JavaScript) |
| `enhanced-rich-text-editor-tables/` | Tables addon for ERTE |
| `enhanced-rich-text-editor-demo/` | Demo application with sample views |
| `enhanced-rich-text-editor-it/` | Integration tests (Playwright) |

## Version History

| Version | Vaadin | Java | Quill | Status |
|---------|--------|------|-------|--------|
| 6.0.x | 25.0.x | 21+ | 2.0.3 | Active development |
| 5.2.x | 24.x | 17+ | 1.3.6 | Maintenance (master branch) |
