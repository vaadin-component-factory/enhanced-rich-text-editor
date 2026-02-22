# Enhanced Rich Text Editor V25 -- Core Component

Core ERTE component for Vaadin 25. Extends Vaadin's built-in Rich Text Editor 2
(Quill 2, Lit) with tabstops, placeholders, readonly sections, rulers, customizable
toolbar, whitespace indicators, and more.

## Maven Dependency

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-v25</artifactId>
    <version>6.0.0-SNAPSHOT</version>
</dependency>
```

> **Note:** Vaadin 25 moved the Rich Text Editor to the commercial `vaadin` artifact.
> A Vaadin Pro subscription or higher is required for production use.

## Quick Start

```java
EnhancedRichTextEditor editor = new EnhancedRichTextEditor();

// Configure tabstops
editor.setTabStops(List.of(
    new TabStop(TabStop.Direction.LEFT, 150),
    new TabStop(TabStop.Direction.RIGHT, 350)
));

// Set HTML content
editor.setValue("<p>Hello, world!</p>");
```

## Requirements

- Java 21+
- Vaadin 25.0.x
- Quill 2.0.3 (vendored by Vaadin RTE 2)

## Documentation

- [User Guide](../docs/USER_GUIDE.md)
- [API Reference](../docs/API_REFERENCE.md)
- [Configuration Guide](../docs/CONFIGURATION.md)
- [Upgrade Guide](../docs/UPGRADE_GUIDE.md) (migrating from v5.x / Vaadin 24)

## License

[CVALv3](https://vaadin.com/license/cval-3) (Commercial Vaadin Add-On License)
