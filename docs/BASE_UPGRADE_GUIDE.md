# Upgrading from ERTE 1.x to 2.x

Migration guide from ERTE 1 (v5.x, Vaadin 24, Quill 1) to ERTE 2 (v6.x, Vaadin 25, Quill 2).

**Terminology:** ERTE 1 = v5.x = V24; ERTE 2 = v6.x = V25. Code examples marked with `// --- ERTE 1 (V24) ---` and `// --- ERTE 2 (V25) ---`.

---

## Table of Contents

- [1. Quick Reference](#1-quick-reference)
- [2. Breaking Changes](#2-breaking-changes)
- [3. New in ERTE 2](#3-new-in-erte-2)
- [4. Known Limitations](#4-known-limitations)
- [5. Quick Troubleshooting](#5-quick-troubleshooting)
- [6. Migration Checklist](#6-migration-checklist)

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
See [Section 2.1](#21-primary-value-format-delta-to-html) for details and migration strategies.

**Maven coordinates:** GroupId and package names unchanged; version bumps from 5.2.0 to 6.0.0.

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor</artifactId>
    <version>6.0.0</version>
</dependency>
```

---

## 2. Breaking Changes

This section gives you an overview of the breaking changes, when upgrading to ERTE 6.0.

### 2.1 Primary Value Format (Delta to HTML)

With Vaadin 25, the value of the Rich Text Editor is not delta anymore, but html. You can still
use the delta value, if you want or need to process it.

**Impact:** High — affects all code that reads or writes editor content.

**Migration strategy:**

**Option A (recommended): Convert to HTML**
```java
editor.asDelta().setValue(storedDelta);
String html = editor.getValue();  // Convert
database.save(id, html);
```
> Requires Vaadin UI context (client-side conversion). Build a dedicated migration view.

**Option B: Keep Delta via wrapper**
```java
editor.asDelta().setValue(deltaJson);
editor.asDelta().addValueChangeListener(e -> database.save(e.getValue()));
```
> No database migration, but all code must use `asDelta()`.

### 2.2 getTextLength()

The text length is read from the client via internal JavaScript. Since any client side interaction is
asynchronous, the method to obtain the current text length has changed. It works now with a `Consumer<Integer>`.

```java
// ERTE 1: synchronous (removed)
int length = editor.getTextLength();

// ERTE 2: callback pattern
editor.getTextLength(length -> {
    System.out.println("Length: " + length);
});
```

> Text length excludes Quill's trailing newline ("Hello" = 5, not 6).

### 2.3 I18n Class

The i18n class name has changed. Also the name for the "deindent" toolbar button has changed to "outdent".

```java
// ERTE 1: RichTextEditorI18n with all fields
var i18n = new EnhancedRichTextEditor.RichTextEditorI18n();
i18n.setDeindent("...");

// ERTE 2: EnhancedRichTextEditorI18n (extends RTE 2 base)
var new EnhancedRichTextEditor.EnhancedRichTextEditorI18n();
var i18n .setOutdent("...");
```

Key changes: class name, `setDeindent()` → `setOutdent()`, typo fix `setPlaceholderAppeance()` → `setPlaceholderAppearance()`

### 2.4 ToolbarButton Enum Changes

As with 2.3, the enum for the "deindent" button has changed.

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

### 2.5 Keyboard Shortcut API

The old API used numeric keyCodes and three boolean parameters for modifier keys. The new API uses Vaadin's `Key` and `KeyModifier` classes, which is more readable and less error-prone. Also, the typo in `addToobarFocusShortcut()` has been fixed.

```java
// ERTE 1: numeric keyCodes + boolean modifiers
editor.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, 66, true, false, false);
editor.addToobarFocusShortcut(121, false, false, false);

// ERTE 2: Key constants + KeyModifier varargs
editor.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, Key.KEY_B, KeyModifier.CONTROL);
editor.addToolbarFocusShortcut(Key.F10);
```

Common key mappings: `66`→`Key.KEY_B`, `73`→`Key.KEY_I`, `85`→`Key.KEY_U`, `9`→`Key.TAB`, `13`→`Key.ENTER`, `121`→`Key.F10`.

### 2.6 Typo Fixes (Placeholder)

A few method names from ERTE 1 had typos that have been corrected:
- `setPlacehoderAltAppearance()` → `setPlaceholderAltAppearance()`
- `addToobarFocusShortcut()` → `addToolbarFocusShortcut()` (see also 2.5)

### 2.7 Placeholder Format API

Placeholder formatting now uses standard `Map<String, Object>` instead of Vaadin's `JsonObject`. This aligns with the rest of the ERTE 2 API and removes the dependency on the elemental JSON library.

```java
// ERTE 1: JsonObject
JsonObject fmt = Json.createObject();
fmt.put("bold", true);
p.setFormat(fmt);

// ERTE 2: Map<String, Object>
p.setFormat(Map.of("bold", true, "italic", true));
```

### 2.8 Removed V24 Methods

Some methods have been removed or replaced. If you used any of these, here are the replacements:

| V24 Method                    | V25 Replacement                                                                                     |
|-------------------------------|-----------------------------------------------------------------------------------------------------|
| `addCustomButton(button)`     | `addCustomToolbarComponents(...)` or `addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, ...)`          |
| `getHtmlValue()`              | `getValue()` (now returns HTML by default)                                                          |
| `getValue()` (returned Delta) | `asDelta().getValue()`                                                                              |

### 2.9 extendOptions Hook

The `extendOptions` callback from ERTE 1 is deprecated. It has been replaced by two more specific hooks: `extendQuill` for registering custom blots before the editor initializes, and `extendEditor` for adding event handlers or other post-init logic. See [EXTENDING.md](dev/EXTENDING.md) for details and examples.

_Side note: this is only relevant, if you develop your own ERTE extension._

---

## 3. New in ERTE 2

ERTE 2 introduces a few new features that weren't available in ERTE 1:

- **CSS custom properties** (`--vaadin-erte-*`) for fine-grained theming of colors, sizes, and spacing
- **Toolbar part-based styling** — custom toolbar components get `part="toolbar-custom-component"` for easy CSS targeting
- **Arrow key navigation** between toolbar buttons (left/right to move, Home/End to jump)
- **Reliable `editor.focus()`** — works from any server-side context without timing workarounds

---

## 4. Known Limitations

These are Quill 2 / Parchment 3 platform constraints, not ERTE bugs. They affect all Quill 2-based editors and cannot be fixed at the ERTE level.

- **Guard nodes:** Quill 2 uses invisible guard characters around embedded elements (like tabstops or placeholders). The cursor may occasionally appear at the guard position instead of the element itself, and format toggles (Ctrl+B/I) can be unreliable right next to an embed.
- **Undo:** Quill's history module may not fully restore embedded blots or readonly formatting after undo.
- **Copy-Paste:** Custom embeds may not survive a clipboard roundtrip (copy from ERTE, paste back). Cross-editor paste loses custom blot types entirely.

---

## 5. Quick Troubleshooting

If you hit a compilation error or unexpected runtime behavior after upgrading, check this table first:

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

## 6. Migration Checklist

A step-by-step checklist for upgrading your project. Work through it top to bottom.

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
