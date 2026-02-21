# Upgrading from Enhanced Rich Text Editor 1.x to 2.x

This guide helps developers migrate from ERTE 1 (v5.x, Vaadin 24, Quill 1) to ERTE 2 (v6.x, Vaadin 25, Quill 2). It covers every breaking change, provides before/after code examples, and includes a step-by-step migration strategy.

**Audience:** Java developers using the Enhanced Rich Text Editor component in their Vaadin applications.

**Scope:** Server-side Java API changes, client-side behavior differences, data format migration, and known limitations.

---

## Table of Contents

- [1. Quick Reference](#1-quick-reference)
- [2. Prerequisites and Dependencies](#2-prerequisites-and-dependencies)
- [3. Breaking Changes](#3-breaking-changes)
- [4. Feature-by-Feature Migration](#4-feature-by-feature-migration)
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
| **Jackson** | 2.x (`com.fasterxml.jackson`) | 3.x (`tools.jackson`) |
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
| 5 | Placeholders (embed + dialog) | Low | Typo fixes, Collection to List |
| 6 | Non-Breaking Space (Shift+Space) | None | Behavior unchanged |
| 7 | Whitespace Indicators | None | API unchanged |
| 8 | Toolbar Slot System | Low | 3 new slots added |
| 9 | Toolbar Button Visibility | Low | Enum rename: DEINDENT to OUTDENT |
| 10 | Custom Keyboard Shortcuts | Medium | keyCode to key name, method rename |
| 11 | HTML Sanitization | None | Improved security (transparent) |
| 12 | I18n | Medium | New class hierarchy |
| 13 | Theme Variants | None | Inherited from Vaadin 25 |
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

ERTE 2 requires **Java 21 or later**. Update your `maven.compiler.release` property:

```xml
<!-- V24 -->
<maven.compiler.release>17</maven.compiler.release>

<!-- V25 -->
<maven.compiler.release>21</maven.compiler.release>
```

### 2.2 Vaadin 25 and Pro Subscription

Vaadin 25 moved the Rich Text Editor from the `vaadin-core` artifact to the
commercial `vaadin` artifact. This means:

- Your project must depend on `vaadin` (not `vaadin-core`)
- A **Vaadin Pro or higher subscription** is required for production use
- The RTE is still free for development and evaluation

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin</artifactId>
</dependency>
```

> **Warning:** If you were previously using `vaadin-core` to avoid Pro dependencies,
> you must now switch to `vaadin` to get RichTextEditor support.

### 2.3 Spring Boot 4.x

Vaadin 25 requires **Spring Boot 4.x** (tested with 4.0.2). Spring Boot 3.x is
not compatible. Key changes:

- Spring Framework 7 (up from 6)
- Jakarta EE 11 namespace (some packages changed)
- Configuration property changes (consult the
  [Spring Boot 4 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide))

### 2.4 Maven Coordinates

Update your ERTE dependency:

```xml
<!-- V24 (ERTE 1) -->
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor</artifactId>
    <version>5.2.0</version>
</dependency>

<!-- V25 (ERTE 2) -->
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-v25</artifactId>
    <version>6.0.0</version>
</dependency>
```

The groupId (`com.vaadin.componentfactory`) and the main Java package remain the
same. Only the artifactId changes.

### 2.5 Lumo Theme Configuration

Vaadin 25 requires explicit Lumo stylesheet loading. Without this, toolbar icons
will render as raw SVG mask images instead of the Lumo icon font.

Add the `@StyleSheet` annotation to your `AppShellConfigurator` implementation:

```java
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;

@StyleSheet(Lumo.STYLESHEET)
public class Application implements AppShellConfigurator {
    // ...
}
```

> **Note:** If you already have a Vaadin 25 application with Lumo working, you likely
> already have this annotation. It is only needed once per application.

### 2.6 The Delta vs HTML Storage Decision

This is the most impactful change for existing applications and deserves careful
planning.

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

ERTE 1 defined its own `RichTextEditorI18n` inner class with all 32+ fields.
ERTE 2 extends Vaadin's built-in `RichTextEditor.RichTextEditorI18n` and adds
only the ERTE-specific fields.

```java
// --- ERTE 1 (V24) ---
EnhancedRichTextEditor.RichTextEditorI18n i18n =
        new EnhancedRichTextEditor.RichTextEditorI18n();
i18n.setBold("Fett");
i18n.setDeindent("Weniger einrücken");  // "deindent" in V24
i18n.setReadonly("Schreibschutz");
editor.setI18n(i18n);

// --- ERTE 2 (V25) ---
EnhancedRichTextEditor.EnhancedRichTextEditorI18n i18n =
        new EnhancedRichTextEditor.EnhancedRichTextEditorI18n();
i18n.setBold("Fett");
i18n.setOutdent("Weniger einrücken");   // "outdent" in V25 (renamed)
i18n.setReadonly("Schreibschutz");
i18n.setPlaceholder("Platzhalter");     // new ERTE-specific field
i18n.setPlaceholderDialogTitle("Platzhalter einfügen"); // new
editor.setI18n(i18n);
```

**Key differences:**

| V24 | V25 | Change |
|-----|-----|--------|
| `RichTextEditorI18n` | `EnhancedRichTextEditorI18n` | Class renamed |
| `setDeindent()` / `getDeindent()` | `setOutdent()` / `getOutdent()` | Renamed |
| All 32 fields in one class | Standard fields inherited from parent | Structural |
| No dialog/combo labels | 8 new ERTE-specific fields | Added |

**New ERTE-specific I18n fields (V25):**

| Method | Default | Description |
|--------|---------|-------------|
| `setReadonly(String)` | "Readonly" | Readonly toolbar button label |
| `setWhitespace(String)` | "Whitespace" | Whitespace toggle label |
| `setPlaceholder(String)` | "Placeholder" | Placeholder button label |
| `setPlaceholderAppearance(String)` | "Appearance" | Appearance toggle label |
| `setPlaceholderDialogTitle(String)` | "Placeholder" | Dialog title |
| `setPlaceholderComboBoxLabel(String)` | "Select placeholder" | Combo-box label |
| `setPlaceholderAppearanceLabel1(String)` | "Plain" | Normal appearance label |
| `setPlaceholderAppearanceLabel2(String)` | "Value" | Alt appearance label |
| `setAlignJustify(String)` | "Justify" | Justify alignment button label |

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

**Impact:** Low -- code using toolbar slots may need awareness of the new style group.

V25 adds a new toolbar group for text color and background color buttons. This
introduces 2 new `ToolbarSlot` values:

| New V25 Slot | Position |
|-------------|----------|
| `BEFORE_GROUP_STYLE` | Before color/background group |
| `AFTER_GROUP_STYLE` | After color/background group |

The total slot count increases from 24 (V24) to 27 (V25), for 11 groups.

Existing slot names are unchanged. Code referencing existing slots (e.g.,
`BEFORE_GROUP_EMPHASIS`, `GROUP_CUSTOM`, `END`) works without changes.

> **Note:** The `GROUP_CUSTOM` slot name remains `"toolbar"` for backward
> compatibility with the tables extension.

### 3.6 Keyboard Shortcut API -- Numeric keyCode to String Key Name

**Impact:** Medium -- all keyboard shortcut registrations must be updated.

Quill 2 requires string key names instead of numeric keyCodes. Additionally, a
method name typo was fixed.

```java
// --- ERTE 1 (V24) ---
// Used numeric keyCodes (Number type)
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.BOLD, 66, true, false, false);        // 66 = 'B'
editor.addToobarFocusShortcut(121, false, false, false); // 121 = F10
//     ^^^ note the typo: "Toobar" (missing 'l')

// --- ERTE 2 (V25) ---
// Uses string key names (String type)
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.BOLD, "b", true, false, false);
editor.addToolbarFocusShortcut("F10", false, false, false);
//     ^^^ typo fixed: "Toolbar"
```

**Common key name mappings:**

| V24 (keyCode) | V25 (key name) | Description |
|:-:|:-:|---|
| 66 | `"b"` | Letter B |
| 73 | `"i"` | Letter I |
| 85 | `"u"` | Letter U |
| 9 | `"Tab"` | Tab key |
| 13 | `"Enter"` | Enter key |
| 112 | `"F1"` | Function key F1 |
| 121 | `"F10"` | Function key F10 |
| 27 | `"Escape"` | Escape key |

### 3.7 Placeholder API Typo Fixes

**Impact:** Low -- search-and-replace.

Two method names had typos in V24 that are fixed in V25:

```java
// --- ERTE 1 (V24) ---
editor.setPlacehoderAltAppearance(true);   // missing 'l'
boolean alt = editor.isPlacehoderAltAppearance();

// --- ERTE 2 (V25) ---
editor.setPlaceholderAltAppearance(true);  // fixed
boolean alt = editor.isPlaceholderAltAppearance();
```

### 3.8 Placeholder Collection to List

**Impact:** Low -- affects code using the return type explicitly.

```java
// --- ERTE 1 (V24) ---
editor.setPlaceholders(Collection<Placeholder> placeholders);
Collection<Placeholder> p = editor.getPlaceholders();

// --- ERTE 2 (V25) ---
editor.setPlaceholders(List<Placeholder> placeholders);
List<Placeholder> p = editor.getPlaceholders();
```

Since `List` extends `Collection`, existing code passing a `List` to
`setPlaceholders()` will compile without changes. Only code that stores the
return value in a `Collection` variable needs updating (or can remain as-is
since `List` is a `Collection`).

### 3.9 Placeholder Format API (JsonObject → Map)

**Impact:** High -- affects code creating or modifying Placeholder formats.

The `format` and `altFormat` properties changed from Vaadin's `JsonObject` to
standard Java `Map<String, Object>`.

```java
// --- ERTE 1 (V24) ---
import elemental.json.JsonObject;
import elemental.json.impl.JreJsonFactory;
import elemental.json.impl.JreJsonObject;

Placeholder p = new Placeholder("{{name}}");
JreJsonFactory factory = new JreJsonFactory();
JsonObject fmt = new JreJsonObject(factory);
fmt.put("bold", true);
p.setFormat(fmt);

JsonObject altFmt = p.getAltFormat();

// --- ERTE 2 (V25) ---
import java.util.Map;

Placeholder p = new Placeholder("{{name}}");
Map<String, Object> fmt = Map.of("bold", true, "italic", true);
p.setFormat(fmt);

// Or for mutable maps:
Map<String, Object> fmt = new HashMap<>();
fmt.put("bold", true);
fmt.put("italic", true);
p.setFormat(fmt);

Map<String, Object> altFmt = p.getAltFormat();
```

**Why:** Aligns with Jackson 3 migration and removes dependency on legacy
`elemental.json` API.

**Migration:** Replace `JsonObject` creation with `Map.of()` or `new HashMap<>()`.

### 3.10 Jackson 2 to Jackson 3

**Impact:** None for most users. Only affects code that directly manipulates ERTE's
internal JSON structures.

Vaadin 25 uses Jackson 3 (`tools.jackson` package) instead of Jackson 2
(`com.fasterxml.jackson` package). If your code extends ERTE classes or directly
uses `JsonNode`, `ObjectNode`, or `ArrayNode` from ERTE's API:

```java
// --- ERTE 1 (V24) ---
import com.fasterxml.jackson.databind.JsonNode;
import elemental.json.JsonArray;

// --- ERTE 2 (V25) ---
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
```

### 3.11 Deprecated V24 Methods

The following V24 methods are removed or deprecated in V25. The table shows the
replacement API.

| V24 Method | V25 Replacement | Notes |
|------------|----------------|-------|
| `addCustomButton(Button)` | `addCustomToolbarComponents(Component...)` | Accepts any Component |
| `addCustomButtons(Button...)` | `addCustomToolbarComponents(Component...)` | Same |
| `getCustomButton(String)` | `getToolbarComponent(ToolbarSlot, String)` | Requires slot parameter |
| `removeCustomButton(String)` | `removeToolbarComponent(ToolbarSlot, String)` | Requires slot parameter |
| `removeCustomButton(Button)` | `removeToolbarComponent(ToolbarSlot, Component)` | Requires slot parameter |
| `getHtmlValue()` | `getValue()` | HTML is now primary format |
| Constructor `(String initialValue)` | `new ERTE()` + `setValue(html)` | Two-step initialization |
| Constructor `(String, ValueChangeMode)` | `new ERTE()` + configure separately | Two-step initialization |

### 3.12 extendOptions Hook -- Deprecated

**Impact:** Medium -- affects extensions (e.g., tables addon) and custom Quill
module registration.

The V24 `extendOptions` hook is deprecated in V25. It still works but prints a
console warning. V25 provides two new, more specific hooks:

```javascript
// --- ERTE 1 (V24) ---
// Single hook: runs before Quill instantiation, receives options + Quill class
window.Vaadin.Flow.vcfEnhancedRichTextEditor = {
    extendOptions: [(options, Quill) => {
        // Register custom blots
        Quill.register('formats/myBlot', MyBlot);
        // Modify Quill options
        options.modules.keyboard.bindings.myBinding = { key: 'F5', handler: () => {} };
    }]
};

// --- ERTE 2 (V25) ---
// Two separate hooks for clarity
window.Vaadin.Flow.vcfEnhancedRichTextEditor = {
    // Pre-init: register blots and modules (receives Quill class only)
    extendQuill: [(Quill) => {
        Quill.register('formats/myBlot', MyBlot);
    }],
    // Post-init: access editor instance (receives editor + Quill class)
    extendEditor: [(editor, Quill) => {
        editor.keyboard.addBinding({ key: 'F5' }, () => { /* handler */ });
    }]
};
```

> **Note:** The V24 `extendOptions` hook still runs if present (with a deprecation
> warning in the console). However, it only receives `(options, Quill)` -- it does
> NOT receive the editor instance. Use `extendEditor` for post-init access.

### 3.13 Toolbar Button Labels -- title to aria-label

**Impact:** Low -- affects test code or CSS that targets button `title` attributes.

RTE 2 (Vaadin 25) uses `aria-label` instead of `title` for toolbar button labels.
If your code or tests query buttons by title attribute, update accordingly:

```java
// --- V24 ---
// Toolbar buttons had title="Bold", title="Italic", etc.

// --- V25 ---
// Toolbar buttons have aria-label="Bold", aria-label="Italic", etc.
```

### 3.14 Removed: Constructors with Initial Value

**Impact:** Low -- affects code using convenience constructors.

```java
// --- ERTE 1 (V24) ---
var editor = new EnhancedRichTextEditor(deltaJsonString);
var editor = new EnhancedRichTextEditor(deltaJsonString, ValueChangeMode.EAGER);

// --- ERTE 2 (V25) ---
var editor = new EnhancedRichTextEditor();
editor.setValue(htmlString);
editor.setValueChangeMode(ValueChangeMode.EAGER);
```

---

## 4. Feature-by-Feature Migration

This section covers all 20 ERTE features organized by migration effort. For each
feature that requires changes, concrete code examples are provided.

### Tier 0 -- No Changes Required

These features work identically in V24 and V25. No code changes needed.

#### Feature 13: Theme Variants

Vaadin 25's Lumo theme is applied automatically. ERTE-specific styles (readonly
sections, tab indicators, ruler rendering) are included in the component.

```java
// Works in both V24 and V25
editor.addThemeVariants(EnhancedRichTextEditorVariant.COMPACT);
```

> **Note:** Ensure `@StyleSheet(Lumo.STYLESHEET)` is on your `AppShellConfigurator`
> (see [Section 2.5](#25-lumo-theme-configuration)).

#### Feature 15: Value Change Mode

Fully inherited from Vaadin's `HasValueChangeMode`. No changes.

```java
// Works in both V24 and V25
editor.setValueChangeMode(ValueChangeMode.EAGER);
```

#### Feature 17: List Indentation Buttons

The indent/outdent buttons work unchanged. The only change is the `ToolbarButton`
enum rename from `DEINDENT` to `OUTDENT` if you reference it in visibility maps.

#### Feature 6: Non-Breaking Space (Shift+Space)

Behavior unchanged. Shift+Space inserts a non-breaking space character.

#### Feature 3: Soft-Break (Shift+Enter)

Behavior unchanged. Shift+Enter inserts a line break within a paragraph and copies
tabs from the previous line.

#### Feature 20: Arrow Navigation

Behavior unchanged. Custom ArrowUp/ArrowDown handling for tab-filled lines is
built in.

### Tier 1 -- Minor API Changes

These features require small, mechanical code changes.

#### Feature 1: Tabstops

The Java API is unchanged:

```java
// Works in both V24 and V25
import com.vaadin.componentfactory.TabStop;

editor.setTabStops(List.of(
    new TabStop(TabStop.Direction.LEFT, 150),
    new TabStop(TabStop.Direction.RIGHT, 350),
    new TabStop(TabStop.Direction.MIDDLE, 550)
));

List<TabStop> stops = editor.getTabStops();
```

> **Note:** Quill 2's embed guard nodes may cause subtle cursor positioning
> differences near tab embeds. See [Section 5.1](#51-quill-2-guard-nodes).

#### Feature 2: Rulers

The Java API is unchanged:

```java
// Works in both V24 and V25
editor.setNoRulers(false);  // show rulers
boolean hidden = editor.isNoRulers();
```

#### Feature 4: Readonly Sections

The Java API is unchanged. Readonly sections are created via the toolbar button
or programmatically via Delta:

```java
// Works in both V24 and V25
editor.asDelta().setValue(
    "[{\"insert\":\"Normal \"},{\"insert\":\"Protected\","
    + "\"attributes\":{\"readonly\":true}},{\"insert\":\" Normal\\n\"}]"
);
```

#### Feature 7: Whitespace Indicators

The Java API is unchanged:

```java
// Works in both V24 and V25
editor.setShowWhitespace(true);
boolean showing = editor.isShowWhitespace();
```

#### Feature 18: Align Justify

The Java API is unchanged. The justify button is included in the toolbar
automatically.

#### Feature 19: Replace Toolbar Button Icons

The API is unchanged, with an additional convenience method in V25:

```java
// Works in both V24 and V25
editor.replaceStandardToolbarButtonIcon(
    EnhancedRichTextEditor.ToolbarButton.BOLD,
    new Icon(VaadinIcon.STAR)
);

// V25 only: pass null to restore default icon
editor.replaceStandardToolbarButtonIcon(
    EnhancedRichTextEditor.ToolbarButton.BOLD,
    null  // restores default
);
```

#### Feature 5: Placeholders

**Changes required:**

1. Fix `setPlacehoderAltAppearance` typo (see [Section 3.7](#37-placeholder-api-typo-fixes))
2. Update `Collection` to `List` if using explicit type (see [Section 3.8](#38-placeholder-collection-to-list))

The rest of the placeholder API -- including events, dialog, tags, and alt
appearance -- is unchanged:

```java
// V25 placeholder setup (same as V24 except typo fix)
List<Placeholder> placeholders = new ArrayList<>();

Placeholder p1 = new Placeholder();
p1.setText("Company Name");
p1.getFormat().put("italic", true);
p1.getAltFormat().put("bold", true);
placeholders.add(p1);

editor.setPlaceholders(placeholders);
editor.setPlaceholderTags("@", "");
editor.setPlaceholderAltAppearancePattern("(?<=\\=).*$");
editor.setPlaceholderAltAppearance(true);  // fixed spelling

// Event listeners (unchanged)
editor.addPlaceholderBeforeInsertListener(event -> {
    event.insert();  // confirm insertion
});
editor.addPlaceholderBeforeRemoveListener(event -> {
    event.remove();  // confirm removal
});
editor.addPlaceholderSelectedListener(event -> {
    List<Placeholder> selected = event.getPlaceholders();
});
```

> **Warning:** Placeholder removal events use `PlaceholderBeforeRemoveEvent`
> with `event.remove()` to confirm. If you do not call `event.remove()`, the
> placeholder will not be deleted. This cancel/confirm pattern is unchanged from V24.

#### Feature 8: Toolbar Slot System

The slot API is unchanged. V25 adds 3 new slots for the new style (color/background)
group. Existing slot references work without changes:

```java
// Works in both V24 and V25
import com.vaadin.componentfactory.toolbar.ToolbarSlot;

editor.addToolbarComponents(ToolbarSlot.AFTER_GROUP_EMPHASIS,
    new Button("Custom"));
editor.addCustomToolbarComponents(new Button("End"));  // GROUP_CUSTOM
```

**New V25 slots:**
- `ToolbarSlot.BEFORE_GROUP_STYLE`
- `ToolbarSlot.AFTER_GROUP_STYLE`

These slots surround the new color/background button group (between emphasis and
heading groups).

#### Feature 9: Toolbar Button Visibility

Update `DEINDENT` references to `OUTDENT`:

```java
// --- ERTE 1 (V24) ---
editor.setToolbarButtonsVisibility(Map.of(
    ToolbarButton.DEINDENT, false
));

// --- ERTE 2 (V25) ---
editor.setToolbarButtonsVisibility(Map.of(
    ToolbarButton.OUTDENT, false
));
```

#### Feature 11: HTML Sanitization

No API changes. The V25 sanitizer is an internal improvement with stronger security:

- CSS `style` attributes are now filtered to a whitelist of safe properties
- `data:` URLs in images are restricted to safe MIME types (PNG, JPEG, GIF, WebP, BMP)
- SVG data URLs are blocked (can contain embedded scripts)
- CSS functions are restricted to safe ones (`rgb()`, `rgba()`, `hsl()`, `calc()`)

This is transparent to application code. Content that was safe in V24 remains safe
in V25. Content with unsafe CSS/data URLs will be sanitized more aggressively.

### Tier 2 -- Moderate Changes

These features require more substantial code updates.

#### Feature 10: Custom Keyboard Shortcuts

Two changes: (1) numeric keyCode to string key name, (2) method name typo fix.

```java
// --- ERTE 1 (V24) ---
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.ALIGN_CENTER, 120, false, true, false);  // Shift+F9
editor.addToobarFocusShortcut(121, false, true, false);    // Shift+F10

// --- ERTE 2 (V25) ---
editor.addStandardToolbarButtonShortcut(
    ToolbarButton.ALIGN_CENTER, "F9", false, true, false); // Shift+F9
editor.addToolbarFocusShortcut("F10", false, true, false); // Shift+F10
```

**Parameter signature comparison:**

```java
// V24
addStandardToolbarButtonShortcut(ToolbarButton, Number keyCode,
    Boolean shortKey, Boolean shiftKey, Boolean altKey)
addToobarFocusShortcut(Number keyCode,
    Boolean shortKey, Boolean shiftKey, Boolean altKey)

// V25
addStandardToolbarButtonShortcut(ToolbarButton, String key,
    boolean shortKey, boolean shiftKey, boolean altKey)
addToolbarFocusShortcut(String key,
    boolean shortKey, boolean shiftKey, boolean altKey)
```

Note that the parameter types also changed from `Boolean` (wrapper) to `boolean`
(primitive) and from `Number` to `String`.

#### Feature 12: I18n

See [Section 3.3](#33-i18n-class-hierarchy) for the full class hierarchy change.

Complete V25 I18n example:

```java
EnhancedRichTextEditor.EnhancedRichTextEditorI18n i18n =
        new EnhancedRichTextEditor.EnhancedRichTextEditorI18n();

// Standard labels (inherited from RTE 2)
i18n.setUndo("Rückgängig");
i18n.setRedo("Wiederholen");
i18n.setBold("Fett");
i18n.setItalic("Kursiv");
i18n.setUnderline("Unterstreichen");
i18n.setStrike("Durchgestrichen");
i18n.setH1("Überschrift 1");
i18n.setH2("Überschrift 2");
i18n.setH3("Überschrift 3");
i18n.setSubscript("Tiefgestellt");
i18n.setSuperscript("Hochgestellt");
i18n.setListOrdered("Nummerierte Liste");
i18n.setListBullet("Aufzählungsliste");
i18n.setOutdent("Weniger einrücken");  // was setDeindent()
i18n.setIndent("Einrücken");
i18n.setAlignLeft("Linksbündig");
i18n.setAlignCenter("Zentriert");
i18n.setAlignRight("Rechtsbündig");
i18n.setImage("Bild");
i18n.setLink("Link");
i18n.setBlockquote("Zitat");
i18n.setCodeBlock("Code");
i18n.setClean("Formatierung entfernen");

// ERTE-specific labels (new in V25)
i18n.setReadonly("Schreibschutz");
i18n.setWhitespace("Leerzeichen anzeigen");
i18n.setPlaceholder("Platzhalter");
i18n.setPlaceholderAppearance("Darstellung");
i18n.setPlaceholderDialogTitle("Platzhalter einfügen");
i18n.setPlaceholderComboBoxLabel("Platzhalter wählen");
i18n.setPlaceholderAppearanceLabel1("Normal");
i18n.setPlaceholderAppearanceLabel2("Wert");
i18n.setAlignJustify("Blocksatz");

editor.setI18n(i18n);
```

> **Note:** All setter methods return `EnhancedRichTextEditorI18n` for fluent
> chaining, including the inherited ones from `RichTextEditorI18n`.

#### Feature 14: Programmatic Text Insertion

The `addText()` methods are unchanged. Only `getTextLength()` changed to async:

```java
// addText -- unchanged in both V24 and V25
editor.addText("Hello", 0);       // insert at position 0
editor.addText("World");           // insert at cursor

// getTextLength -- V24 was synchronous, V25 is async
// V24: int len = editor.getTextLength();
// V25:
editor.getTextLength(length -> {
    System.out.println("Text length: " + length);
});
```

**Additional V25 behavior change:** `addText(text, position)` now clamps
out-of-bounds positions to the nearest valid index (V24 silently rejected them).

#### Feature 16: extendOptions Hook

See [Section 3.11](#311-extendoptions-hook----deprecated) for the full migration
guide. Summary:

| V24 | V25 |
|-----|-----|
| `extendOptions` | `extendQuill` (pre-init) + `extendEditor` (post-init) |
| Receives `(options, Quill)` | `extendQuill`: `(Quill)`, `extendEditor`: `(editor, Quill)` |
| Single timing | Two separate timings |

---

## 5. Known Limitations

These are inherent limitations of the Quill 2 / Parchment 3 platform that affect
ERTE 2 behavior. They are not bugs in ERTE 2 but platform constraints.

### 5.1 Quill 2 Guard Nodes

Quill 2 Embed blots (tabs, placeholders, soft-breaks) place invisible "guard"
characters (`\uFEFF`, zero-width no-break space) **inside** the embed element,
flanking the content node:

```
<span class="ql-tab">
  [guard text \uFEFF]      <-- invisible, inside domNode
  [contentNode]             <-- contenteditable="false"
  [guard text \uFEFF]      <-- invisible, inside domNode
</span>
```

**Effects on user experience:**

- **Cursor position:** The browser caret may appear at the guard node position
  rather than at the visual edge of the embed. This is most noticeable with
  tabstops where the caret appears at the left edge of the tab span rather
  than at the tabstop position.

- **Format toggles at embed boundaries:** Pressing Ctrl+B or Ctrl+I immediately
  before or after an embed may not apply formatting as expected. The cursor may
  be positioned inside the guard node context where format state is ambiguous.

- **Selection behavior:** Selecting text that includes embeds may produce
  unexpected selection ranges due to guard node characters being included.

> **Note:** These limitations exist in any Quill 2-based editor, not just ERTE.
> They are inherent to how Quill 2 handles cursor placement around non-editable
> content.

### 5.2 History and Undo Limitations

- **Undo removes readonly formatting:** When content with readonly sections is
  edited and then undone, the undo operation may strip the `readonly` attribute
  from the inline span. This is a Quill 2 history module limitation -- it does
  not track attribute changes on inline blots the same way as text formatting.

- **Undo does not reliably restore embed blots:** Deleting a placeholder embed
  and pressing Ctrl+Z may not restore the placeholder. Quill 2's history module
  has difficulty reconstructing complex embed blots from undo deltas.

### 5.3 Copy-Paste Limitations

- **Embed blots do not survive clipboard roundtrip:** When content containing
  placeholders or tabs is copied and pasted, the embed blots may not survive
  the HTML-to-Delta-to-HTML clipboard conversion. The pasted content will
  contain plain text instead of the embedded component.

- **Cross-editor paste:** Copying content from one ERTE instance to another does
  not preserve custom blot types. The clipboard uses HTML as the intermediate
  format, and the receiving editor's sanitizer may strip unknown elements.

### 5.4 Browser-Specific Notes

- **Chrome inline rendering:** TabBlot uses `display: inline-block` instead of
  `inline-flex` due to Chrome treating `inline-flex` elements as atomic inlines,
  which breaks vertical arrow navigation through tab-filled lines.

- **ArrowUp/ArrowDown in tab-filled lines:** Custom keyboard handlers ensure
  correct vertical navigation. Without these handlers, the browser would skip
  lines containing tab embeds.

### 5.5 Feature Interaction Matrix

| Feature A | Feature B | Status | Notes |
|-----------|-----------|--------|-------|
| Tabstops | Soft-Break | Works | Tabs are copied after soft-break |
| Tabstops | Whitespace Indicators | Works | Tab arrows displayed |
| Tabstops | Readonly Sections | Works | Tabs inside readonly are protected |
| Tabstops | Arrow Navigation | Works | Custom handlers ensure correct navigation |
| Placeholders | Readonly Sections | Works | Placeholders inside readonly are protected |
| Placeholders | Undo/Redo | Limited | Undo may not restore deleted placeholders |
| Placeholders | Copy-Paste | Limited | Embeds may not survive clipboard roundtrip |
| Readonly Sections | Undo/Redo | Limited | Undo can remove readonly formatting |
| All Embeds | Format Toggle | Limited | Ctrl+B/I at embed boundary unreliable |
| Whitespace Indicators | All Embeds | Works | Indicators shown for tabs, soft-breaks, paragraphs |
| I18n | Toolbar Buttons | Works | All buttons (standard + ERTE) receive labels |
| I18n | Placeholder Dialog | Works | Dialog title and combo-box label translated |
| Toolbar Visibility | All Buttons | Works | Standard + ERTE buttons controlled uniformly |
| Keyboard Shortcuts | Toolbar Buttons | Works | Any shortcut can trigger any button |

---

## 6. Step-by-Step Migration Strategy

### 6.1 Choose Your Approach

**Approach A: Big-Bang Migration (small to medium projects)**

Suitable for projects with limited ERTE usage (1-5 editor instances) and good
test coverage. All changes are made at once.

1. Create a new branch from your main branch
2. Update all dependencies (Vaadin 25, Spring Boot 4, Java 21, ERTE 6)
3. Fix all compilation errors (the compiler will guide you to breaking changes)
4. Handle the Delta-to-HTML storage format change
5. Test each editor instance manually
6. Run automated tests
7. Deploy

**Approach B: Gradual Migration (recommended for larger projects)**

Suitable for projects with extensive ERTE usage, complex placeholder configurations,
or Delta storage in databases.

**Phase 1: Get it compiling**

1. Update `pom.xml` with new dependency coordinates
2. Fix Java 21 compilation issues
3. Update Spring Boot 4 configuration
4. Fix all import statements and renamed methods
5. Verify the application starts (even if editor behavior is wrong)

**Phase 2: Fix API changes**

6. Update `ToolbarButton.DEINDENT` to `OUTDENT` everywhere
7. Update keyboard shortcut registrations (keyCode to key name)
8. Fix placeholder API typos
9. Update I18n class to `EnhancedRichTextEditorI18n`
10. Convert `getTextLength()` calls to async pattern
11. Update `extendOptions` hooks to `extendQuill`/`extendEditor`

**Phase 3: Handle data format**

12. Decide on Delta vs HTML storage strategy (see [Section 2.6](#26-the-delta-vs-html-storage-decision))
13. If converting: build a migration script/view
14. Update all `getValue()`/`setValue()` calls
15. Test with production data (or a copy)

**Phase 4: Verify and deploy**

16. Test each feature systematically (see checklist below)
17. Run automated tests
18. Test with production data
19. Deploy to staging environment
20. Monitor for issues before production deployment

### 6.2 Testing Strategy

**Unit tests:**
- Update any test that calls `getValue()` (now returns HTML, not Delta)
- Update `getTextLength()` assertions to use async pattern
- Update `ToolbarButton.DEINDENT` references
- Update I18n test code

**Integration tests:**
- Verify Delta round-trips via `asDelta().setValue()` / `asDelta().getValue()`
- Test HTML round-trips via `setValue()` / `getValue()`
- Verify sanitizer preserves ERTE-specific content (readonly spans, placeholders)

**UI tests (Playwright/TestBench):**
- Test all toolbar buttons render and function
- Test keyboard shortcuts (new key name format)
- Test placeholder dialog (insert, remove, appearance toggle)
- Test readonly section toggle
- Test tabstop rendering and ruler interaction
- Test whitespace indicator toggle
- Verify I18n labels display correctly

### 6.3 Feature Verification Checklist

Test each feature after migration:

| Feature | How to Verify |
|---------|--------------|
| Tabstops | Press Tab in editor. Tab renders at defined positions. |
| Rulers | Rulers visible above editor (unless `noRulers=true`). Click ruler to add tabstop. |
| Soft-Break | Shift+Enter inserts line break. Tabs copied on next line. |
| Readonly | Click readonly button. Selected text becomes gray/protected. Cannot delete. |
| Placeholders | Click placeholder button. Dialog opens. Select and insert. Click to select in editor. |
| NBSP | Shift+Space inserts non-breaking space. |
| Whitespace | Toggle whitespace button. Tab arrows, paragraph marks visible. |
| Toolbar Slots | Custom components appear in correct toolbar positions. |
| Button Visibility | Hidden buttons not shown. Groups auto-hide when all buttons hidden. |
| Shortcuts | Custom keyboard shortcuts trigger correct buttons. |
| Sanitization | Paste HTML with scripts. Scripts stripped. ERTE classes preserved. |
| I18n | Set translated labels. All buttons and dialog show translated text. |
| Theme | Lumo theme applied. Dark mode works. Compact variant works. |
| addText | Programmatic text insertion at position and cursor. |
| Value Change | EAGER/LAZY/TIMEOUT modes work. |
| extendOptions | Custom Quill extensions load (via extendQuill/extendEditor). |
| Indent | Indent/outdent buttons work on list items. |
| Align Justify | Justify alignment button works. |
| Replace Icons | Custom icons appear on toolbar buttons. |
| Arrow Nav | ArrowUp/Down through tab-filled lines works correctly. |

---

## 7. Troubleshooting

Common issues encountered during migration, presented in symptom / cause / solution
format.

### 7.1 Compilation Errors

**Symptom:** `cannot find symbol: class RichTextEditor`

**Cause:** Vaadin 25 moved RichTextEditor from `vaadin-core` to the commercial
`vaadin` artifact.

**Solution:** Replace `vaadin-core` with `vaadin` in your `pom.xml`:
```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin</artifactId>
</dependency>
```

---

**Symptom:** `cannot find symbol: variable DEINDENT`

**Cause:** `ToolbarButton.DEINDENT` was renamed to `ToolbarButton.OUTDENT` in V25.

**Solution:** Replace all occurrences:
```java
// Before
ToolbarButton.DEINDENT
// After
ToolbarButton.OUTDENT
```

---

**Symptom:** `cannot find symbol: method setPlacehoderAltAppearance(boolean)`

**Cause:** Method name typo fixed in V25.

**Solution:** Fix the spelling:
```java
// Before
editor.setPlacehoderAltAppearance(true);
// After
editor.setPlaceholderAltAppearance(true);
```

---

**Symptom:** `cannot find symbol: method addToobarFocusShortcut(...)`

**Cause:** Method name typo fixed in V25.

**Solution:** Fix the spelling:
```java
// Before
editor.addToobarFocusShortcut(121, false, false, false);
// After
editor.addToolbarFocusShortcut("F10", false, false, false);
```

---

**Symptom:** `cannot find symbol: method getTextLength()` (no-arg version)

**Cause:** `getTextLength()` changed from synchronous `int` return to async
callback.

**Solution:** Use the callback pattern:
```java
// Before
int len = editor.getTextLength();
// After
editor.getTextLength(len -> { /* use len */ });
```

---

**Symptom:** `cannot find symbol: class RichTextEditorI18n` in ERTE context

**Cause:** The inner class was renamed to `EnhancedRichTextEditorI18n`.

**Solution:** Update the class reference:
```java
// Before
new EnhancedRichTextEditor.RichTextEditorI18n()
// After
new EnhancedRichTextEditor.EnhancedRichTextEditorI18n()
```

---

**Symptom:** `cannot find symbol: method setDeindent(String)`

**Cause:** Renamed to `setOutdent(String)` in V25.

**Solution:** Replace all occurrences:
```java
// Before
i18n.setDeindent("Weniger einrücken");
// After
i18n.setOutdent("Weniger einrücken");
```

---

**Symptom:** `incompatible types: Number cannot be converted to String` in shortcut
methods

**Cause:** Keyboard shortcut API changed from numeric keyCodes to string key names.

**Solution:** Replace numeric keyCodes with string key names:
```java
// Before
editor.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, 66, true, false, false);
// After
editor.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, "b", true, false, false);
```

---

**Symptom:** `cannot find symbol: method getHtmlValue()`

**Cause:** `getHtmlValue()` was removed. `getValue()` now returns HTML directly.

**Solution:** Replace `getHtmlValue()` with `getValue()`:
```java
// Before
String html = editor.getHtmlValue();
// After
String html = editor.getValue();
```

### 7.2 Runtime Issues

**Symptom:** Toolbar buttons show raw SVG images instead of Lumo icons

**Cause:** Missing Lumo stylesheet injection in Vaadin 25.

**Solution:** Add `@StyleSheet(Lumo.STYLESHEET)` to your `AppShellConfigurator`:
```java
@StyleSheet(Lumo.STYLESHEET)
public class Application implements AppShellConfigurator { }
```

---

**Symptom:** Editor content disappears after calling `setValue()` with Delta JSON

**Cause:** `setValue()` now expects HTML, not Delta JSON. Passing Delta JSON is
treated as literal HTML text.

**Solution:** Use `asDelta()` wrapper for Delta input:
```java
// Before (V24: setValue accepted Delta)
editor.setValue(deltaJsonString);
// After (V25: use asDelta() for Delta input)
editor.asDelta().setValue(deltaJsonString);
```

---

**Symptom:** `extendOptions` callbacks don't seem to run / console warning about
deprecation

**Cause:** `extendOptions` is deprecated in V25. It still runs but prints a warning.

**Solution:** Migrate to the new hooks:
```javascript
// Replace extendOptions with extendQuill + extendEditor
window.Vaadin.Flow.vcfEnhancedRichTextEditor = {
    extendQuill: [(Quill) => { /* pre-init registration */ }],
    extendEditor: [(editor, Quill) => { /* post-init access */ }]
};
```

---

**Symptom:** Cursor appears stuck or invisible near tab embeds

**Cause:** Quill 2 guard nodes affect cursor positioning around embed blots.
This is a known platform limitation.

**Solution:** This is expected behavior with Quill 2. The cursor is technically
positioned at the guard node. Clicking directly on the tab or using arrow keys
will move the cursor correctly. See [Section 5.1](#51-quill-2-guard-nodes).

---

**Symptom:** Readonly formatting disappears after pressing Ctrl+Z (undo)

**Cause:** Quill 2's history module does not track inline blot attribute changes
perfectly. This is a known limitation.

**Solution:** No workaround available. Advise users to avoid using undo immediately
after applying readonly formatting. See [Section 5.2](#52-history-and-undo-limitations).

---

**Symptom:** Spring Boot application fails to start with Vaadin 25

**Cause:** Vaadin 25 requires Spring Boot 4.x. Spring Boot 3.x is not compatible.

**Solution:** Update to Spring Boot 4.x:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.2</version>
</parent>
```

---

**Symptom:** Jackson deserialization errors or `ClassNotFoundException` for
`com.fasterxml.jackson`

**Cause:** Vaadin 25 uses Jackson 3 (`tools.jackson` package).

**Solution:** Update import statements from `com.fasterxml.jackson` to
`tools.jackson`. Note that most ERTE users will not encounter this unless they
extend ERTE classes or directly manipulate JSON objects from the API.

---

**Symptom:** Keyboard shortcut does nothing (V25)

**Cause:** Using numeric keyCodes instead of string key names. Quill 2 creates
the binding under a wrong key and silently fails.

**Solution:** Use string key names:
```java
// Wrong (silently fails)
// editor.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, 66, ...);
// Correct
editor.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, "b", true, false, false);
```

---

**Symptom:** Custom toolbar component appears in wrong position after upgrading

**Cause:** V25 adds a new style group (color/background) between emphasis and
heading groups, shifting toolbar group positions.

**Solution:** Verify your `ToolbarSlot` references. If you placed components
relative to the emphasis or heading group, they should still be correct. If you
used absolute positioning or index-based placement, verify the visual position.

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

- [ ] Update `pom.xml`: change artifactId to `enhanced-rich-text-editor-v25`
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

---

## 9. Additional Considerations

### 9.1 Tables Extension

The Enhanced Rich Text Editor Tables addon (`enhanced-rich-text-editor-tables`) is
being migrated separately to V25 (`enhanced-rich-text-editor-tables-v25`, version
`2.0.0`). The tables addon migration includes:

- Quill 2 / Parchment 3 blot rewrites (5 critical API changes)
- Updated overlay/popup positioning (Polymer to Lit)
- `extendQuill` hook integration (replacing `extendOptions`)

If you use the tables extension, wait for its V25 release before migrating. The
tables extension requires the core ERTE 2 component.

### 9.2 Performance

ERTE 2 benefits from several performance improvements:

- **Lit-based web component** (replacing Polymer): Faster rendering, smaller
  bundle size, more efficient DOM updates
- **Quill 2**: Improved rendering performance for large documents
- **Improved sanitizer**: More efficient regex-based filtering

No performance-related code changes are needed on the application side.

### 9.3 Rollback Strategy

If the migration encounters blocking issues:

1. **Keep the V24 branch available** in version control
2. **Do not modify database schema** during the migration (the editor content
   format is the only data change)
3. If you performed a Delta-to-HTML conversion: keep a backup of the original
   Delta content
4. The V24 ERTE (v5.x) and V25 ERTE (v6.x) use different artifact IDs, so you
   can maintain both versions in separate branches without Maven coordinate
   conflicts

### 9.4 Getting Help

- **ERTE GitHub:** Report issues on the Enhanced Rich Text Editor repository
- **Vaadin Forum:** Ask migration questions with the `rich-text-editor` tag
- **Vaadin 25 Migration Guide:** For general Vaadin 24 to 25 migration guidance

### 9.5 Security Improvements

ERTE 2 includes security hardening that addresses known vulnerabilities in ERTE 1:

| Issue | ERTE 1 | ERTE 2 |
|-------|--------|--------|
| PlaceholderBlot XSS | Used `innerHTML` with dynamic content | Uses `textContent` (safe) |
| SoftBreakBlot | Used `innerHTML = '<br>'` | Uses `createElement('br')` |
| CSS injection | No `style` attribute filtering | Whitelist of safe CSS properties |
| Data URL abuse | No MIME type restriction | Restricted to safe image types |
| CSS function injection | No function filtering | Whitelist (rgb, hsl, calc only) |

These improvements are transparent to application code. No action is required
unless you were deliberately using unsafe patterns (which is unlikely).

---

*This guide covers the migration from ERTE v5.x to v6.x. For the latest
updates, consult the project repository and release notes.*
