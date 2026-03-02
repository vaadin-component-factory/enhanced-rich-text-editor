# Upgrading from ERTE 1.x to 2.x

Migration guide from ERTE 1 (v5.x, Vaadin 24, Quill 1) to ERTE 2 (v6.x, Vaadin 25, Quill 2).

**Terminology:** ERTE 1 = v5.x = V24; ERTE 2 = v6.x = V25. Code examples marked with `// --- ERTE 1 (V24) ---` and `// --- ERTE 2 (V25) ---`.

---

## Table of Contents

- [1. Quick Reference](#1-quick-reference)
- [2. Prerequisites and Dependencies](#2-prerequisites-and-dependencies)
- [3. Breaking Changes](#3-breaking-changes)
- [4. New in ERTE 2](#4-new-in-erte-2)
- [5. Known Limitations](#5-known-limitations)
- [6. Troubleshooting](#6-troubleshooting)
- [7. Migration Checklist](#7-migration-checklist)
- [8. Additional Considerations](#8-additional-considerations)

---

## 1. Quick Reference

### Version Mapping

| Component | ERTE 1 (v5.x) | ERTE 2 (v6.x) |
|-----------|----------------|----------------|
| **ERTE Version** | 5.2.0 | 6.0.0 |
| **Vaadin** | 24.x | 25.0.x |
| **Quill** | 1.3.6 | 2.0.3 |
| **Parchment** | 1.x | 3.x |

### Feature Migration Effort Summary

All 20 ERTE features supported in v6.x. Migration effort by category:

| Category | Effort | Count | Key Changes |
|----------|:------:|:-----:|-----------|
| No changes | None | 11 | Tabstops, Rulers, Soft-Break, Readonly, NBSP, Whitespace, Value Change, Align Justify, Icons, Arrow Nav, Theme Variants |
| Enum/name renames | Low | 6 | DEINDENT→OUTDENT, typo fixes (`setPlaceholder...`), Placeholder format (JsonObject→Map) |
| API changes | Medium | 3 | getTextLength (async), Keyboard shortcuts (keyCode→Key), extendOptions (deprecated) |

**Critical change:** The primary value format changed from Delta JSON to HTML.
See [Section 3.1](#31-primary-value-format-delta-to-html) for details and migration
strategies.

---

## 2. Prerequisites and Dependencies
### 2.1 Maven Coordinates

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor</artifactId>
    <version>6.0.0</version>
</dependency>
```

GroupId and package names unchanged; version bumps from 5.2.0 to 6.0.0.

### 2.2 Primary Value Format: Delta to HTML

**What changed:**
- `getValue()` returns HTML (not Delta)
- `setValue()` accepts HTML
- `getHtmlValue()` removed — use `getValue()`
- Delta access via `asDelta()` wrapper

**Migration strategy:**

**Option A (recommended): Convert to HTML**
```java
// One-time migration
editor.asDelta().setValue(storedDelta);
String html = editor.getValue();  // Convert
database.save(id, html);
```
> Requires Vaadin UI context (client-side conversion). Build a dedicated migration view.

**Option B: Keep Delta via wrapper**
```java
// All reads/writes through asDelta()
editor.asDelta().setValue(deltaJson);
editor.asDelta().addValueChangeListener(e -> database.save(e.getValue()));
```
> No database migration, but all code must use `asDelta()`.

---

## 3. Breaking Changes

This section documents every API change that will cause a compilation error or
runtime behavior change. Each change includes before/after code examples.

### 3.1 Primary Value Format (Delta to HTML)

**Impact:** High -- affects all code that reads or writes editor content.

```java
// --- ERTE 1 (V24) ---
// Delta is the primary format
editor.setValue(deltaJsonString);
String delta = editor.getValue();     // Returns Delta JSON
String html = editor.getHtmlValue();  // Separate HTML getter

// --- ERTE 2 (V25) ---
// HTML is the primary format
editor.setValue(htmlString);
String html = editor.getValue();      // Returns HTML

// Delta access via wrapper
editor.asDelta().setValue(deltaJsonString);
String delta = editor.asDelta().getValue();
```

See [Section 2.2](#22-the-delta-vs-html-storage-decision) for migration strategies.

### 3.2 getTextLength() -- Now Asynchronous

```java
// ERTE 1: synchronous (removed)
int length = editor.getTextLength();

// ERTE 2: callback pattern
editor.getTextLength(length -> {
    System.out.println("Length: " + length);
});
```

> Text length excludes Quill's trailing newline ("Hello" = 5, not 6).

### 3.3 I18n Class

```java
// ERTE 1: RichTextEditorI18n with all fields
new EnhancedRichTextEditor.RichTextEditorI18n().setDeindent("...")

// ERTE 2: EnhancedRichTextEditorI18n (extends RTE 2 base)
new EnhancedRichTextEditor.EnhancedRichTextEditorI18n().setOutdent("...")
```

Key changes: class name, `setDeindent()` → `setOutdent()`, typo fix `setPlaceholderAppeance()` → `setPlaceholderAppearance()`

### 3.4 ToolbarButton Enum Changes

**Impact:** Low -- only affects code referencing `DEINDENT`.

```java
// --- ERTE 1 (V24) ---
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.DEINDENT, false,
    EnhancedRichTextEditor.ToolbarButton.BOLD, false
));

// --- ERTE 2 (V25) ---
editor.setToolbarButtonsVisibility(Map.of(
    EnhancedRichTextEditor.ToolbarButton.OUTDENT, false,  // renamed
    EnhancedRichTextEditor.ToolbarButton.BOLD, false
));
```

**Changes from V24:**

| V24 | V25 | Change |
|-----|-----|--------|
| `DEINDENT` | `OUTDENT` | Renamed (matches RTE 2 naming) |
| -- | `COLOR` | Added (new in RTE 2) |
| -- | `BACKGROUND` | Added (new in RTE 2) |
| -- | `ALIGN_JUSTIFY` | Added (promoted from Quill-only to enum) |

Each enum constant now has a `getPartSuffix()` method (e.g., `"bold"`) and a
`getPartName()` method (e.g., `"toolbar-button-bold"`) for CSS part-based targeting.

### 3.5 Keyboard Shortcut API

```java
// ERTE 1: numeric keyCodes + boolean modifiers
editor.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, 66, true, false, false);
editor.addToobarFocusShortcut(121, false, false, false);

// ERTE 2: Key constants + KeyModifier varargs + typo fix
editor.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, Key.KEY_B, KeyModifier.CONTROL);
editor.addToolbarFocusShortcut(Key.F10);
```

Key mappings: `66`→`Key.KEY_B`, `73`→`Key.KEY_I`, `85`→`Key.KEY_U`, `9`→`Key.TAB`, `13`→`Key.ENTER`, `121`→`Key.F10`. Typo fix: `addToobarFocusShortcut()` → `addToolbarFocusShortcut()`.

### 3.6 Typo Fixes (Placeholder)

Rename: `setPlacehoderAltAppearance()` → `setPlaceholderAltAppearance()` (typo fix).

### 3.7 Placeholder Format API

```java
// ERTE 1: JsonObject
JsonObject fmt = Json.createObject();
fmt.put("bold", true);
p.setFormat(fmt);

// ERTE 2: Map<String, Object>
p.setFormat(Map.of("bold", true, "italic", true));
```

### 3.8 Removed V24 Methods

| V24 | V25 |
|-----|----|
| `addCustomButton(Button)` | `addCustomToolbarComponents(Component...)` or `addToolbarComponents(ToolbarSlot, ...)` |
| `getHtmlValue()` | `getValue()` |
| `getValue()` (Delta) | `asDelta().getValue()` |

### 3.9 extendOptions Hook

Deprecated. Use `extendQuill` (register blots) or `extendEditor` (event handlers) instead. See [EXTENDING.md](dev/EXTENDING.md).

---

## 4. New in ERTE 2

- **22 CSS custom properties** (`--vaadin-erte-*`) for theming
- **Toolbar part-based styling** for custom components (`part="toolbar-custom-component"`)
- **Arrow key navigation** between toolbar buttons
- **Reliable `editor.focus()`** from any context

---

## 5. Known Limitations

Quill 2 / Parchment 3 constraints (not ERTE bugs):

- **Guard nodes:** Cursor may appear at guard position; Ctrl+B/I unreliable at embed boundary
- **Undo:** May not restore embed blots or readonly formatting
- **Copy-Paste:** Embeds may not survive clipboard roundtrip; cross-editor paste loses custom blot types

---

## 6. Quick Troubleshooting

| Error | Fix |
|-------|-----|
| `DEINDENT` not found | Use `OUTDENT` |
| `setPlacehoderAltAppearance()` not found | Typo: use `setPlaceholderAltAppearance()` |
| `addToobarFocusShortcut()` not found | Typo: use `addToolbarFocusShortcut()` |
| `getTextLength()` no-arg | Now async: `getTextLength(len -> { })` |
| `RichTextEditorI18n` not found | Rename to `EnhancedRichTextEditorI18n` |
| `setDeindent()` not found | Rename to `setOutdent()` |
| Keycode `66` error | Use `Key.KEY_B` constant |
| `getValue()` returns HTML | Use `asDelta().getValue()` for Delta |
| `setValue(deltaJson)` disappears | Use `asDelta().setValue()` for Delta |

---

## 7. Migration Checklist

### Before Starting
- [ ] Back up database if storing content
- [ ] Decide: Convert to HTML or keep Delta via `asDelta()`

### API Changes
- [ ] `DEINDENT` → `OUTDENT`
- [ ] `setPlacehoderAltAppearance()` → `setPlaceholderAltAppearance()`
- [ ] `addToobarFocusShortcut()` → `addToolbarFocusShortcut()`
- [ ] Keyboard shortcuts: keyCodes → `Key` constants
- [ ] `getTextLength()` → async callback
- [ ] I18n class → `EnhancedRichTextEditorI18n`
- [ ] `getHtmlValue()` → `getValue()`
- [ ] `JsonObject` → `Map<String, Object>` for placeholders

### Data Format
- [ ] Audit `getValue()`/`setValue()` (now HTML)
- [ ] Add `asDelta()` where Delta needed
- [ ] Test stored content loads/saves correctly

### Verification
- [ ] Compiles without errors
- [ ] All features work (toolbar, placeholders, readonly, tabstops, etc.)
- [ ] Tests pass
- [ ] Tested with production data
