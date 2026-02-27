# Upgrading from ERTE 1.x to 2.x

Migration guide from ERTE 1 (v5.x, Vaadin 24, Quill 1) to ERTE 2 (v6.x, Vaadin 25, Quill 2).

**Terminology:** ERTE 1 = v5.x = V24; ERTE 2 = v6.x = V25. Code examples marked with `// --- ERTE 1 (V24) ---` and `// --- ERTE 2 (V25) ---`.

---

## Table of Contents

- [1. Quick Reference](#1-quick-reference)
- [2. Prerequisites and Dependencies](#2-prerequisites-and-dependencies)
- [3. Breaking Changes](#3-breaking-changes)
- [4. Feature-by-Feature Migration](#4-feature-by-feature-migration)
- [4A. New in ERTE 2](#4a-new-in-erte-2)
- [5. Known Limitations](#5-known-limitations)
- [6. Step-by-Step Migration Strategy](#6-step-by-step-migration-strategy)
- [7. Troubleshooting](#7-troubleshooting)
- [8. Migration Checklist](#8-migration-checklist)
- [9. Additional Considerations](#9-additional-considerations)

---

## 1. Quick Reference

### Version Mapping

| Component | ERTE 1 (v5.x) | ERTE 2 (v6.x) |
|-----------|----------------|----------------|
| **ERTE Version** | 5.2.0 | 6.0.0 |
| **Vaadin** | 24.x | 25.0.x |
| **Spring Boot** | 3.x | 4.x |
| **Java** | 17+ | 21+ |
| **Quill** | 1.3.6 | 2.0.3 |
| **Parchment** | 1.x | 3.x |
| **JSON API** | `elemental.json` (Vaadin's JSON API, not Jackson) | Jackson 3.x (`tools.jackson`) |
| **JUnit** | 4/5 | 5 |
| **Mockito** | 1.x / PowerMock | 5.x |
| **Web Component** | Polymer | Lit |

### Feature Migration Effort Summary

All 20 ERTE features are fully supported in v6.x. The following table shows how much
work is required on the *consumer* side during migration.

| # | Feature | Migration Effort | Notes |
|---|---------|:----------------:|-------|
| 1 | Tabstops (L/R/M alignment) | None | Java API unchanged |
| 2 | Rulers (horizontal + vertical) | None | API unchanged |
| 3 | Soft-Break (Shift+Enter + tab copy) | None | Behavior unchanged |
| 4 | Readonly Sections (inline) | None | API unchanged |
| 5 | Placeholders (embed + dialog) | Medium | Typo fixes, Collection to List, multiple API changes |
| 6 | Non-Breaking Space (Shift+Space) | None | Behavior unchanged |
| 7 | Whitespace Indicators | None | API unchanged |
| 8 | Toolbar Slot System | Low | 2 new slots added |
| 9 | Toolbar Button Visibility | Low | Enum rename: DEINDENT to OUTDENT |
| 10 | Custom Keyboard Shortcuts | Medium | keyCode to key name, method rename |
| 11 | HTML Sanitization | Low | Improved security (transparent), stricter filtering |
| 12 | I18n | Medium | New class hierarchy |
| 13 | Theme Variants | Low | `EnhancedRichTextEditorVariant` removed, use standard Vaadin variants |
| 14 | Programmatic Text Insertion | Medium | getTextLength is now async |
| 15 | Value Change Mode | None | Inherited |
| 16 | extendOptions Hook | Medium | Deprecated, new hooks available |
| 17 | List Indentation Buttons | Low | DEINDENT renamed to OUTDENT |
| 18 | Align Justify | None | API unchanged |
| 19 | Replace Toolbar Button Icons | None | API unchanged, new convenience method |
| 20 | Arrow Navigation | None | Behavior unchanged |

**Critical change:** The primary value format changed from Delta JSON to HTML.
See [Section 3.1](#31-primary-value-format-delta-to-html) for details and migration
strategies.

---

## 2. Prerequisites and Dependencies

### 2.1 Java Version

Update to **Java 21+** in `maven.compiler.release`:

```xml
<maven.compiler.release>21</maven.compiler.release>
```

### 2.2 Vaadin 25 and Pro Subscription

RTE moved to the `vaadin` (Pro) artifact. Requires **Vaadin Pro or higher** for production (free for dev/eval).

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin</artifactId>
</dependency>
```

### 2.3 Spring Boot 4.x

Update to **Spring Boot 4.x** (tested 4.0.2). See [Spring Boot 4 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) for details.

### 2.4 Maven Coordinates

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor</artifactId>
    <version>6.0.0</version>
</dependency>
```

GroupId and package names unchanged; version bumps from 5.2.0 to 6.0.0.

### 2.5 Lumo Theme Configuration

Add to `AppShellConfigurator` to load Lumo (toolbar icons won't render without it):

```java
@StyleSheet(Lumo.STYLESHEET)
public class Application implements AppShellConfigurator { }
```

### 2.6 The Delta vs HTML Storage Decision

Primary value format is now HTML (not Delta JSON). Two options:

**What changed:**

| Aspect | ERTE 1 (v5.x) | ERTE 2 (v6.x) |
|--------|----------------|----------------|
| `getValue()` returns | Delta JSON | HTML |
| `setValue()` accepts | Delta JSON | HTML |
| `getHtmlValue()` | HTML (separate getter) | *Removed* (use `getValue()`) |
| Delta access | Primary API | Via `asDelta()` wrapper |
| Value-changed events carry | Delta JSON | HTML |

**ERTE 2 provides the `asDelta()` wrapper** for applications that still need Delta
access:

```java
// Read Delta
String deltaJson = editor.asDelta().getValue();

// Write Delta
editor.asDelta().setValue(deltaJsonString);

// Listen to Delta changes
editor.asDelta().addValueChangeListener(event -> {
    String newDelta = event.getValue();
});
```

> **Warning:** The `asDelta()` value-changed listener fires on **blur** (Vaadin's
> lazy value sync), not immediately after each keystroke. If you need immediate
> Delta updates, read the Delta directly from the client via
> `editor.getElement().executeJs(...)`.

**If your application stores content in a database, you have two options:**

**Option A: Convert stored Deltas to HTML (recommended)**

Perform a one-time migration of your database content from Delta JSON to HTML.
This aligns your storage with the new primary format and is the cleanest approach
going forward.

```java
// One-time migration script (example)
for (MyDocument doc : documentRepository.findAll()) {
    String deltaJson = doc.getContent(); // stored as Delta
    // Use a temporary ERTE instance to convert
    editor.asDelta().setValue(deltaJson);
    String html = editor.getValue(); // now returns HTML
    doc.setContent(html);
    documentRepository.save(doc);
}
```

> **Note:** The conversion must happen with a running Vaadin UI context because
> the Delta-to-HTML conversion happens on the client side. Consider building a
> dedicated migration view for this purpose.

**Option B: Keep Delta format using `asDelta()` wrapper**

If converting stored content is not feasible, you can continue using Delta format
through the wrapper:

```java
// Read from database (Delta JSON)
String deltaJson = documentRepository.findById(id).getContent();
editor.asDelta().setValue(deltaJson);

// Save to database (Delta JSON)
editor.asDelta().addValueChangeListener(event -> {
    document.setContent(event.getValue());
    documentRepository.save(document);
});
```

This approach requires no database migration but means all reads/writes must go
through `asDelta()`. The standard `getValue()`/`setValue()` methods will return
and expect HTML.

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

See [Section 2.6](#26-the-delta-vs-html-storage-decision) for migration strategies.

### 3.2 getTextLength() -- Synchronous to Asynchronous

**Impact:** Medium -- any code calling `getTextLength()` must be rewritten.

The synchronous API was removed because Vaadin 25's deadlock detection prevents
blocking server-side calls that wait for client responses. The new API uses an
async callback pattern (similar to `WebStorage.getItem()`).

```java
// --- ERTE 1 (V24) ---
int length = editor.getTextLength();
System.out.println("Length: " + length);

// --- ERTE 2 (V25) ---
editor.getTextLength(length -> {
    System.out.println("Length: " + length);
});
```

> **Note:** The callback receives the text length excluding Quill's internal
> trailing newline. "Hello" returns 5, not 6.

### 3.3 I18n Class Hierarchy

**Impact:** Medium -- code that creates or uses `RichTextEditorI18n` must update.

Class renamed from `RichTextEditorI18n` to `EnhancedRichTextEditorI18n` and now extends Vaadin's base class:

```java
// --- ERTE 1 (V24) ---
editor.setI18n(new EnhancedRichTextEditor.RichTextEditorI18n()
    .setDeindent("Weniger einrücken"));

// --- ERTE 2 (V25) ---
editor.setI18n(new EnhancedRichTextEditor.EnhancedRichTextEditorI18n()
    .setOutdent("Weniger einrücken"));   // renamed
```

Key changes: `setDeindent()` → `setOutdent()`, typo fixed `setPlaceholderAppeance()` → `setPlaceholderAppearance()`
| All fields in one monolithic class | Standard RTE fields inherited from `RichTextEditor.RichTextEditorI18n`; ERTE-specific fields in subclass | Structural split |

The main breaking change is **structural**: V24 had all 32+ fields in a single
`RichTextEditorI18n` class. V25 splits this into a parent class (standard RTE 2
labels like bold, italic, etc.) and an `EnhancedRichTextEditorI18n` subclass
(ERTE-specific labels). The API is source-compatible for most setters -- you just
need to change the class name and fix the renamed/typo-fixed methods.

**Key structural change:** V24's monolithic `RichTextEditorI18n` inner class contained all fields (standard RTE + ERTE-specific). V25 separates them:
- **Parent class `RichTextEditor.RichTextEditorI18n`**: Standard RTE 2 fields (inherited from Vaadin's RTE 2)
- **Subclass `EnhancedRichTextEditor.EnhancedRichTextEditorI18n`**: ERTE-specific fields only

Use the subclass for ERTE features. All setters on both classes support fluent chaining.

**ERTE-specific I18n fields (V25 subclass):**

Most of these fields already existed in V24's monolithic class and have been moved
into the V25 subclass. Only `whitespace` is genuinely new.

| Method | Default | Status |
|--------|---------|--------|
| `setReadonly(String)` | "Readonly" | Moved from V24 |
| `setWhitespace(String)` | "Whitespace" | **New in V25** |
| `setPlaceholder(String)` | "Placeholder" | Moved from V24 |
| `setPlaceholderAppearance(String)` | "Appearance" | Moved from V24 (typo fixed) |
| `setPlaceholderDialogTitle(String)` | "Placeholder" | Moved from V24 |
| `setPlaceholderComboBoxLabel(String)` | "Select placeholder" | Moved from V24 |
| `setPlaceholderAppearanceLabel1(String)` | "Plain" | Moved from V24 |
| `setPlaceholderAppearanceLabel2(String)` | "Value" | Moved from V24 |
| `setAlignJustify(String)` | "Justify" | Moved from V24 |

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

**Complete V25 ToolbarButton enum:**

Standard RTE 2 buttons (25):
`UNDO`, `REDO`, `BOLD`, `ITALIC`, `UNDERLINE`, `STRIKE`, `COLOR`, `BACKGROUND`,
`H1`, `H2`, `H3`, `SUBSCRIPT`, `SUPERSCRIPT`, `LIST_ORDERED`, `LIST_BULLET`,
`OUTDENT`, `INDENT`, `ALIGN_LEFT`, `ALIGN_CENTER`, `ALIGN_RIGHT`,
`IMAGE`, `LINK`, `BLOCKQUOTE`, `CODE_BLOCK`, `CLEAN`

ERTE-specific buttons (5):
`READONLY`, `PLACEHOLDER`, `PLACEHOLDER_APPEARANCE`, `WHITESPACE`, `ALIGN_JUSTIFY`

**Changes from V24:**

| V24 | V25 | Change |
|-----|-----|--------|
| `DEINDENT` | `OUTDENT` | Renamed (matches RTE 2 naming) |
| -- | `COLOR` | Added (new in RTE 2) |
| -- | `BACKGROUND` | Added (new in RTE 2) |
| -- | `ALIGN_JUSTIFY` | Added (promoted from Quill-only to enum) |

Each enum constant now has a `getPartSuffix()` method (e.g., `"bold"`) and a
`getPartName()` method (e.g., `"toolbar-button-bold"`) for CSS part-based targeting.

### 3.5 ToolbarSlot Enum Changes

**Impact:** Low

V25 adds 2 new slots: `BEFORE_GROUP_STYLE` and `AFTER_GROUP_STYLE` (for the new color/background group). Total: 27 slots (11 groups). Existing slots work unchanged.

### 3.6 Keyboard Shortcut API

**Impact:** Medium

Change numeric keyCodes to string key names. Method also renamed: `addToobarFocusShortcut()` → `addToolbarFocusShortcut()`.

```java
// V24: numeric keyCodes
editor.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, 66, true, false, false);
editor.addToobarFocusShortcut(121, false, false, false);

// V25: string key names
editor.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, "b", true, false, false);
editor.addToolbarFocusShortcut("F10", false, false, false);
```

Key mappings: `66`→`"b"`, `73`→`"i"`, `85`→`"u"`, `9`→`"Tab"`, `13`→`"Enter"`, `112`→`"F1"`, `121`→`"F10"`, `27`→`"Escape"`

### 3.7 Typo Fixes (Placeholder and I18n)

**Impact:** Low

Rename: `setPlacehoderAltAppearance()` → `setPlaceholderAltAppearance()`, `setPlaceholderAppeance()` → `setPlaceholderAppearance()` (missing 'l' and 'r').

### 3.8 Placeholder Collection to List

**Impact:** Low

Change `Collection<Placeholder>` to `List<Placeholder>` where used explicitly. Mostly backward-compatible since `List extends Collection`.

### 3.9 Placeholder Format API

**Impact:** High

Change from `JsonObject` to `Map<String, Object>`:

```java
// V24: Placeholder format
Map<String, Object> fmt = Map.of("bold", true, "italic", true);
p.setFormat(fmt);

// V25: same pattern (Map replaces JsonObject)
```

Use `Map.of()` or `new HashMap<>()` instead of `JreJsonObject()`.

### 3.10 JSON API Change

**Impact:** None for most users

Internal: `elemental.json` → `tools.jackson`. If extending ERTE, replace `import elemental.json.*` with `import tools.jackson.databind.*`.

### 3.11 Removed V24 Methods

| V24 | V25 |
|-----|-----|
| `addCustomButton(Button)` | `addCustomToolbarComponents(Component...)` |
| `addCustomButtons(Button...)` | `addCustomToolbarComponents(Component...)` |
| `getCustomButton(String)` | `getToolbarComponent(ToolbarSlot, String)` |
| `removeCustomButton(String)` | `removeToolbarComponent(ToolbarSlot, String)` |
| `getHtmlValue()` | `getValue()` |
| Constructor `(String initialValue)` | Still available |
| Constructor `(String, ValueChangeListener)` | Still available |
| Constructor `(ValueChangeListener)` | Still available |

### 3.12 extendOptions Hook

**Impact:** Medium

Deprecated. Replace with `extendQuill` (pre-init, register blots) or `extendEditor` (post-init, access editor instance).

### 3.13 Toolbar Button Labels

**Impact:** Low

Toolbar buttons now use `aria-label` instead of `title`. Update selectors if needed.

---

## 4. Feature-by-Feature Migration

### No Changes (Tier 0)

**Features 3, 6, 13, 15, 17, 20** — Behavior and API unchanged:
- Feature 3: Soft-Break (Shift+Enter)
- Feature 6: Non-Breaking Space (Shift+Space)
- Feature 13: Theme Variants (now use Vaadin's standard theme mechanism instead of `EnhancedRichTextEditorVariant`)
- Feature 15: Value Change Mode
- Feature 17: List Indentation Buttons (only change: `DEINDENT` → `OUTDENT` enum)
- Feature 20: Arrow Navigation

**Features 1, 2, 4, 7, 18, 19** — API unchanged:
- Feature 1: Tabstops
- Feature 2: Rulers
- Feature 4: Readonly Sections
- Feature 7: Whitespace Indicators
- Feature 18: Align Justify
- Feature 19: Replace Toolbar Button Icons (new: pass `null` to restore default)

#### Feature 5: Placeholders

**Minor changes:** Fix `setPlacehoderAltAppearance` typo; change `Collection` to `List`.
Rest unchanged (events, dialog, tags, alt appearance). API compatible with V24.

#### Feature 8: Toolbar Slot System

API unchanged. V25 adds 2 new slots: `BEFORE_GROUP_STYLE`, `AFTER_GROUP_STYLE`. Existing slots work without changes.

#### Feature 9: Toolbar Button Visibility

Update `DEINDENT` to `OUTDENT` in visibility maps.

#### Feature 11: HTML Sanitization

No API changes. Sanitizer now stricter: CSS properties whitelisted, `data:` URLs filtered, SVG blocked. Transparent to code.

### Moderate Changes (Tier 2)

#### Feature 10: Custom Keyboard Shortcuts

See [Section 3.6](#36-keyboard-shortcut-api) — change numeric keyCodes to string key names; fix `addToobarFocusShortcut()` typo.

```java
// V24: numeric keyCodes
editor.addStandardToolbarButtonShortcut(ToolbarButton.ALIGN_CENTER, 120, false, true, false);
editor.addToobarFocusShortcut(121, false, true, false);

// V25: string key names
editor.addStandardToolbarButtonShortcut(ToolbarButton.ALIGN_CENTER, "F9", false, true, false);
editor.addToolbarFocusShortcut("F10", false, true, false);
```

#### Feature 12: I18n

See [Section 3.3](#33-i18n-class-hierarchy). Update class to `EnhancedRichTextEditorI18n` and change `setDeindent()` to `setOutdent()`.

#### Feature 14: Programmatic Text Insertion

`addText()` unchanged; `getTextLength()` changed to async (see [Section 3.2](#32-gettextlength----synchronous-to-asynchronous)).

#### Feature 16: extendOptions Hook

Deprecated. Replace with `extendQuill` (pre-init) or `extendEditor` (post-init) (see [Section 3.12](#312-extendoptions-hook)).

---

## 4A. New in ERTE 2

### 4A.1 Toolbar Helper Classes

Four helper classes (`ToolbarSwitch`, `ToolbarPopover`, `ToolbarSelectPopup`, `ToolbarDialog`) for custom toolbar extensions. See [User Guide](BASE_USER_GUIDE.md) for examples.

### 4A.2 CSS Custom Properties

20 properties (`--vaadin-erte-*`) for theming: readonly sections, placeholders, whitespace indicators, rulers. Override on host element. All have sensible Lumo defaults.

### 4A.3 Toolbar Part-Based Styling

Custom components automatically get `part="toolbar-custom-component"`. Style via `vcf-enhanced-rich-text-editor [part~='toolbar-custom-component']` from application CSS.

### 4A.4 Other Improvements

Arrow keys navigate between toolbar buttons. Reliable `editor.focus()` from any context (V24 had timing issues).

---

## 5. Known Limitations

Quill 2 / Parchment 3 platform constraints (not ERTE bugs):

**Quill 2 Guard Nodes:** Embed blots (tabs, placeholders, soft-breaks) have invisible guard characters inside. Effects: cursor may appear at guard position (especially tabs); Ctrl+B/I at embed boundary unreliable; selection with embeds may include guards.

**Undo:** Undo may not restore embed blots or may strip readonly formatting.

**Copy-Paste:** Embed blots (placeholders, tabs) may not survive clipboard roundtrip (convert to plain text). Cross-editor paste loses custom blot types.

**Browser notes:** TabBlot uses `inline-block` (not `inline-flex`) to prevent Chrome skipping lines. Custom arrow handlers ensure correct navigation in tab-filled lines.

---

## 6. Migration Strategy

**Big-Bang (small projects):** Update deps, fix errors, test all at once.

**Gradual (larger projects):** Phase 1) Update deps, fix compilation. Phase 2) Fix API changes (enum renames, async callbacks, I18n class, typos). Phase 3) Handle Delta-to-HTML conversion. Phase 4) Test and deploy.

**Testing:** Unit tests (update `getValue()` calls, `getTextLength()` async pattern); Integration tests (Delta/HTML round-trips, sanitizer); UI tests (toolbar, shortcuts, dialogs, readonly, tabstops, i18n).

**Verification checklist:** Tabstops, rulers, soft-break, readonly, placeholders, NBSP, whitespace, toolbar slots, button visibility, shortcuts, sanitization, I18n, theme, addText, value change, extendOptions, indent, align justify, replace icons, arrow navigation.

---

## 7. Troubleshooting

### Compilation Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `cannot find symbol: class RichTextEditor` | Using `vaadin-core` instead of `vaadin` | Switch to `vaadin` artifact |
| `cannot find symbol: variable DEINDENT` | Renamed to `OUTDENT` | Replace `DEINDENT` → `OUTDENT` |
| `cannot find symbol: method setPlacehoderAltAppearance(...)` | Typo fixed | Use `setPlaceholderAltAppearance()` |
| `cannot find symbol: method addToobarFocusShortcut(...)` | Typo fixed | Use `addToolbarFocusShortcut()` |
| `cannot find symbol: method getTextLength()` (no-arg) | Changed to async | Use `editor.getTextLength(len -> { })` |
| `cannot find symbol: class RichTextEditorI18n` | Renamed to `EnhancedRichTextEditorI18n` | Update class reference |
| `cannot find symbol: method setDeindent(...)` | Renamed to `setOutdent()` | Use `i18n.setOutdent()` |
| `incompatible types: Number cannot be converted to String` | keyCodes → string key names | Use `"b"` instead of `66`, `"F10"` instead of `121` |
| `cannot find symbol: method getHtmlValue()` | Removed; use `getValue()` | Replace `getHtmlValue()` with `getValue()` |

### Runtime Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| Toolbar buttons show raw SVG (no Lumo icons) | Missing `@StyleSheet(Lumo.STYLESHEET)` | Add to `AppShellConfigurator` |
| Editor content disappears after `setValue(deltaJson)` | `setValue()` expects HTML, not Delta | Use `editor.asDelta().setValue(deltaJson)` |
| `extendOptions` deprecated warning | V24 hook is deprecated | Migrate to `extendQuill`/`extendEditor` |
| Cursor stuck near tabs | Quill 2 guard nodes (known limitation) | Expected behavior; clicking/arrows work correctly (see [Section 5.1](#51-quill-2-guard-nodes)) |
| Readonly formatting disappears on undo | Quill 2 history limitation | No workaround; see [Section 5.2](#52-history-and-undo-limitations) |
| Spring Boot fails to start | Vaadin 25 requires Spring Boot 4.x | Update to Spring Boot 4.0.2+ |
| Jackson errors (`com.fasterxml.jackson`) | Vaadin 25 uses Jackson 3 (`tools.jackson`) | Update imports from `com.fasterxml` → `tools.jackson` |
| Keyboard shortcut does nothing | Using numeric keyCodes instead of string names | Use string key names: `"b"`, `"F10"`, etc. |
| Custom toolbar component in wrong position | V25 adds style group between emphasis/heading | Verify `ToolbarSlot` references |

---

## 8. Migration Checklist

Use this checklist to track your migration progress. Copy it into your project's
issue tracker or migration document.

### Before Migration

- [ ] Read this upgrade guide completely
- [ ] Review the [breaking changes](#3-breaking-changes) section
- [ ] Decide on Delta vs HTML storage strategy ([Section 2.6](#26-the-delta-vs-html-storage-decision))
- [ ] Verify you have a Vaadin Pro (or higher) subscription
- [ ] Back up your database if it stores editor content
- [ ] Ensure Java 21+ is available in your build and runtime environments
- [ ] Create a migration branch in version control

### During Migration -- Dependencies

- [ ] Update `pom.xml`: change artifactId to `enhanced-rich-text-editor`
- [ ] Update `pom.xml`: change version to `6.0.0`
- [ ] Update Vaadin BOM to 25.0.x
- [ ] Switch from `vaadin-core` to `vaadin` if applicable
- [ ] Update Spring Boot to 4.x
- [ ] Update Java compiler target to 21
- [ ] Add `@StyleSheet(Lumo.STYLESHEET)` to `AppShellConfigurator`

### During Migration -- API Changes

- [ ] Replace `ToolbarButton.DEINDENT` with `ToolbarButton.OUTDENT`
- [ ] Replace `setPlacehoderAltAppearance()` with `setPlaceholderAltAppearance()`
- [ ] Replace `isPlacehoderAltAppearance()` with `isPlaceholderAltAppearance()`
- [ ] Replace `addToobarFocusShortcut()` with `addToolbarFocusShortcut()`
- [ ] Convert keyboard shortcut keyCodes to string key names
- [ ] Convert `getTextLength()` calls to async callback pattern
- [ ] Update I18n class from `RichTextEditorI18n` to `EnhancedRichTextEditorI18n`
- [ ] Replace `setDeindent()` with `setOutdent()` in I18n configuration
- [ ] Replace `getHtmlValue()` with `getValue()`
- [ ] Update `setValue()` / `getValue()` usage (HTML format, not Delta)
- [ ] Migrate `extendOptions` hooks to `extendQuill` / `extendEditor`
- [ ] Replace convenience constructors with no-arg constructor + setters
- [ ] Update Jackson imports if extending ERTE classes (`com.fasterxml` to `tools.jackson`)

### During Migration -- Data Format

- [ ] Audit all `getValue()` call sites (now returns HTML)
- [ ] Audit all `setValue()` call sites (now accepts HTML)
- [ ] Add `asDelta()` wrapper where Delta format is still needed
- [ ] If converting stored Deltas: implement and test the migration script
- [ ] Verify value-changed event listeners receive HTML (not Delta)
- [ ] Test Delta round-trip via `asDelta()` if using Option B

### After Migration -- Verification

- [ ] Application compiles without errors
- [ ] Application starts successfully
- [ ] Lumo theme loads (toolbar icons render correctly)
- [ ] All toolbar buttons visible and functional
- [ ] Keyboard shortcuts work
- [ ] Placeholder dialog opens, inserts, and removes correctly
- [ ] Readonly sections toggle and protect content
- [ ] Tabstops render at correct positions
- [ ] Rulers display and tabstop click-to-add works
- [ ] Whitespace indicators toggle on/off
- [ ] I18n labels display correctly (if configured)
- [ ] Editor content loads from database correctly
- [ ] Editor content saves to database correctly
- [ ] Custom toolbar components render in correct positions
- [ ] Automated tests pass
- [ ] Tested with production data (or representative sample)

### After Migration -- New V25 Features (optional)

- [ ] CSS custom properties override correctly (e.g., `--vaadin-erte-readonly-background`)
- [ ] Toolbar helper classes work if used (`ToolbarSwitch`, `ToolbarPopover`, etc.)
- [ ] Custom toolbar components show correct interactive states (hover, focus, active)
- [ ] Toolbar arrow-key navigation works across built-in and custom buttons
- [ ] `editor.focus()` reliably focuses the editor content area

---

## 9. Additional Considerations

### 9.1 Tables Extension

Tables addon (v2.0.0) available for V25. Breaking change: Table Delta format differs from ERTE 1 (surrounding ops, not just cell format). Deltas structurally compatible but not byte-identical. Verify compatibility if parsing table deltas outside ERTE.

### 9.2 Performance

ERTE 2 faster: Lit replaces Polymer, Quill 2 improved rendering, better sanitizer. No code changes needed.

### 9.3 Rollback

Keep V24 branch available. Don't change DB schema. If converting Delta→HTML, keep backup. Separate artifact IDs avoid conflicts.

### 9.4 Security

ERTE 2 hardens: `innerHTML` replaced with `textContent`/`createElement()`, CSS properties whitelisted, data URLs filtered, CSS functions restricted. Transparent to code.

### 9.5 Documentation

For details, see the full migration guide and [USER_GUIDE.md](BASE_USER_GUIDE.md).

ERTE 2 introduces 22 CSS custom properties (`--vaadin-erte-*`) for theming
ERTE-specific visual elements. If your application customized ERTE 1 appearance
via direct CSS selectors (e.g., `.ql-readonly { background: ... }`), consider
switching to the custom property approach for better maintainability and forward
compatibility.

See [Section 4A.2](#4a2-css-custom-properties) for the full property list and
examples.

---

*This guide covers the migration from ERTE v5.x to v6.x. For the latest
updates, consult the project repository and release notes.*
