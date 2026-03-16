Enhanced Rich Text Editor (ERTE) for Flow is an extended version of the Vaadin Rich Text Editor with tabstops, placeholders, non-breaking space, rulers, customizable toolbar, readonly sections, whitespace indicators, and more.

**Version 6.x** targets Vaadin 25.0.x and Java 21+.

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


## Quick start

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

For more samples, please check the [User Guide](https://github.com/vaadin-component-factory/enhanced-rich-text-editor/blob/master/docs/BASE_USER_GUIDE.md).

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

To enable table functionality, see the [Tables Extension](https://vaadin.com/directory/component/enhanced-rich-text-editor-tables-extension).
