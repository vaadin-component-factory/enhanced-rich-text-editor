# Enhanced Rich Text Editor for Flow

Enhanced Rich Text Editor (ERTE) for Flow is an extended version of the Vaadin Rich Text Editor with tabstops, placeholders, non-breaking space, rulers, customizable toolbar, readonly sections, whitespace indicators, and more.

**Version 6.x** targets Vaadin 25.0.x, Java 21+, Spring Boot 4.x, and Quill 2.

**License:** [CVALv3](https://vaadin.com/license/cval-3) (Commercial Vaadin Add-On License)

## Documentation

- [User Guide](docs/USER_GUIDE.md) -- Features, examples, and best practices
- [API Reference](docs/API_REFERENCE.md) -- Complete API surface (methods, events, enums)
- [Configuration Guide](docs/CONFIGURATION.md) -- Toolbar, i18n, shortcuts, theming, sanitization
- [Upgrade Guide](docs/UPGRADE_GUIDE.md) -- Migrating from ERTE 1 (v5.x, Vaadin 24) to ERTE 2 (v6.x, Vaadin 25)

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-v25</artifactId>
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

## Features

| Feature | Description |
|---------|-------------|
| **Tabstops** | Left/Right/Middle alignment with pixel-precise positioning |
| **Rulers** | Horizontal and vertical rulers with click-to-add tabstops |
| **Placeholders** | Embedded tokens with dialog, formatting, and alt appearance |
| **Readonly Sections** | Inline content protection with delete prevention |
| **Toolbar Customization** | 27 slots, 30 button visibility controls, custom keyboard shortcuts |
| **Toolbar Icon Replacement** | Replace any standard button icon with Vaadin icons |
| **Non-Breaking Space** | Shift+Space inserts non-breaking space |
| **Soft-Break** | Shift+Enter inserts line break within paragraph |
| **Whitespace Indicators** | Visual display of tabs, breaks, paragraphs, and wraps |
| **Align Justify** | Additional justify alignment option |
| **Extension Hooks** | Register custom Quill formats and modules |
| **I18n** | Full internationalization for all ERTE-specific labels |
| **Sanitization** | Server-side HTML sanitizer with XSS protection |
| **Programmatic Text** | Insert text at cursor or position, query length |

## Tables

To enable table functionality, add the separate Tables Extension addon:

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-tables-v25</artifactId>
    <version>2.0.0</version>
</dependency>
```

```java
EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte);
```

See the [Tables Guide](enhanced-rich-text-editor-tables-v25/docs/TABLES_GUIDE.md) for full documentation, and the [Tables Upgrade Guide](enhanced-rich-text-editor-tables-v25/docs/TABLES_UPGRADE_GUIDE.md) for V1 → V2 migration.

## Running the Demo

```bash
# Build all modules
bash v25-build.sh

# Start the demo server on port 8080
bash v25-server-start.sh

# Browse to http://localhost:8080

# Stop the server
bash v25-server-stop.sh
```

## Running Tests

```bash
# Build and start server first
bash v25-build.sh
bash v25-server-start.sh

# Run Playwright tests
cd enhanced-rich-text-editor-demo
npx playwright test tests/erte/

# Stop server after testing
bash v25-server-stop.sh
```

## Project Structure

| Module | Description |
|--------|-------------|
| `enhanced-rich-text-editor-v25/` | Core component (V25) |
| `enhanced-rich-text-editor-tables-v25/` | Tables extension (V25) |
| `enhanced-rich-text-editor-demo/` | Demo application with test views |
| `enhanced-rich-text-editor/` | V24 core (reference only, excluded from build) |
| `enhanced-rich-text-editor-tables/` | V24 tables (reference only, excluded from build) |

## Version History

| Version | Vaadin | Java | Quill | Status |
|---------|--------|------|-------|--------|
| 6.0.x | 25.0.x | 21+ | 2.0.3 | Active development |
| 5.2.x | 24.x | 17+ | 1.3.6 | Maintenance (master branch) |
