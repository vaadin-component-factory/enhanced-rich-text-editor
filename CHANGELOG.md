# Changelog

All notable changes to the Enhanced Rich Text Editor (ERTE) will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [6.0.0] - Unreleased

Complete rewrite for Vaadin 25. ERTE 2 extends Vaadin's built-in Rich Text Editor 2
(Quill 2, Lit, Parchment 3) instead of maintaining a standalone fork.

For migration instructions, see [Upgrade Guide](enhanced-rich-text-editor-v25/docs/UPGRADE_GUIDE.md).

### Added

- **Tabstops** -- Left, Right, and Middle alignment with pixel-precise positioning
- **Rulers** -- Horizontal and vertical rulers with click-to-add/cycle/remove tabstops
- **Placeholders** -- Embedded tokens with dialog, primary/alt formatting, keyboard shortcut (Ctrl+P)
- **Readonly Sections** -- Inline content protection with delete prevention and lock icon
- **Toolbar Slot System** -- 27 named slots across 11 button groups for custom components
- **Toolbar Button Visibility** -- Show/hide any of 30 standard toolbar buttons
- **Toolbar Icon Replacement** -- Replace any standard button icon with Vaadin icons
- **Custom Keyboard Shortcuts** -- Register shortcuts with modifier keys via Java API
- **Non-Breaking Space** -- Shift+Space inserts NBSP
- **Soft-Break** -- Shift+Enter inserts line break within paragraph, copies preceding tabs
- **Whitespace Indicators** -- Visual display of tabs, line breaks, paragraph marks, and word wraps
- **Align Justify** -- Additional justify alignment button in the toolbar
- **Extension Hooks** -- `extendQuill()` and `extendEditor()` for custom Quill formats and modules
- **I18n** -- Full internationalization for all ERTE-specific labels and tooltips
- **Sanitization** -- Server-side HTML sanitizer with ERTE-aware class whitelist and XSS protection
- **Programmatic Text Insertion** -- `addText()` at cursor or position, `getTextLength()` query
- **Toolbar Popover** -- `ToolbarPopover` component for popover-based toolbar controls
- **Toolbar Select Popup** -- `ToolbarSelectPopup` component for dropdown menu toolbar controls
- **Toolbar Dialog** -- `ToolbarDialog` component for dialog-based toolbar controls
- **Custom CSS Properties** -- 22 `--vaadin-erte-*` custom properties for styling readonly, placeholder, whitespace, and ruler elements
- **Slotted Component Styling** -- `part="toolbar-custom-component"` with built-in hover, focus, active, and disabled states
- **Full documentation suite** -- User Guide, API Reference, Upgrade Guide, Developer Guide, Architecture Guide, Contributing Guide

### Changed

- **Value format:** HTML-primary (matching Vaadin RTE 2). Delta access via `asDelta()` wrapper. ERTE 1 used Delta-primary format.
- **Quill:** 1.3.6 to 2.0.3
- **Parchment:** 1.x to 3.x
- **Web Component:** Polymer to Lit
- **Java:** 17+ to 21+
- **Vaadin:** 24.x to 25.0.x
- **Spring Boot:** 3.x to 4.x
- **JSON API:** `elemental.json` to Jackson 3.x (`tools.jackson`)
- **JUnit:** 4/5 to 5 only
- **Mockito:** 1.x / PowerMock to 5.x
- **Tag name:** `vcf-enhanced-rich-text-editor` (own web component registration)
- **Placeholder API:** `getPlaceholders()` returns `List` instead of `Collection`; typo fixes (`Palceholder` to `Placeholder`)
- **Toolbar button enum:** `DEINDENT` renamed to `OUTDENT`; new `COLOR` and `BACKGROUND` values
- **I18n class:** Dedicated `EnhancedRichTextEditorI18n` with ERTE-specific labels

### Removed

- **Delta-primary value API** -- Use `getValue()`/`setValue()` for HTML, `asDelta()` for Delta access
- **`setTabStops(String json)`** -- Replaced by `setTabStops(List<TabStop>)` with typed API
- **`setPlaceholders(String json)`** -- Replaced by `setPlaceholders(List<Placeholder>)` with typed API
- **PowerMock** -- Replaced by Mockito 5.x
- **Polymer patterns** -- Replaced by Lit

## [5.2.0] - Previous Release

Last release on the V24 (Vaadin 24) branch. See `master` branch for V24 code.

- Vaadin 24.x, Java 17+, Quill 1.3.6
- All 20 ERTE features supported
- Tables extension available separately

[6.0.0]: https://github.com/nicofr/vaadin-erte/compare/v5.2.0...HEAD
[5.2.0]: https://github.com/nicofr/vaadin-erte/releases/tag/v5.2.0
