# Tables Upgrade Guide (V1 → V2)

**For developers migrating from ERTE Tables V1 (Vaadin 24 / Quill 1) to ERTE Tables V2 (Vaadin 25 / Quill 2).** This guide walks through breaking changes and migration steps.

---

## Quick Version Matrix

| Aspect | Tables V1 | Tables V2 |
|--------|-----------|-----------|
| ERTE version | 5.x | 6.x |
| Vaadin | 24.x | 25.0.x |
| Quill | 1.3.6 | 2.0.3 |
| Parchment | 1.x | 3.x |
| JSON Library | elemental `Json` | Jackson 3 (`tools.jackson`) |
| Maven artifact | `enhanced-rich-text-editor-tables` | `enhanced-rich-text-editor-tables` |
| Package | `com.vaadin.componentfactory.erte.tables` | (same) |
| License | CVALv3 | CVALv3 |

---

## Breaking Changes

### 1. Jackson 3 Migration (Json → ObjectNode)

Vaadin 25 ships Jackson 3 (`tools.jackson`) instead of the elemental JSON library. If you work with template JSON in your code, you'll need to update those calls — but the changes are mechanical and IDE-friendly.

```java
// V1: elemental Json
Json parsedJson = TemplateParser.parseJson(jsonString);
String name = templates.getObject("myTemplate").getString("name");

// V2: Jackson 3 ObjectNode
ObjectNode templates = TemplateParser.parseJson(jsonString);
String name = templates.get("myTemplate").get("name").asText();
```

### 2. Table Delta Format Change

The internal Delta format for table cells has changed slightly. Don't worry — ERTE handles this for you. It reads old V1 deltas just fine and automatically produces the clean V2 format on output. For normal editor usage, there's nothing to do.

This only matters if you have your own code that parses table deltas directly — for instance a PDF exporter or a server-side content processor that reads raw Delta JSON. If that's you, read on.

Quill 1 used to add five extra attributes (`"0":"T"`, `"1":"A"`, `"2":"B"`, `"3":"L"`, `"4":"E"`) to every table cell op. Quill 2 dropped them — only the `td` attribute remains. The `td` value itself (pipe-separated: `tableId|rowId|cellId|mergeId|colspan|rowspan|templateClass`) hasn't changed at all.

```json
// V1: five extra attributes alongside td
{"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"id|id|id||||template1"},"insert":"\n"}

// V2: just td
{"attributes":{"td":"id|id|id||||template1"},"insert":"\n"}
```

### 3. Color Validation on Hover/Focus Setters

V1 accepted any string in `setTableHoverColor()` and similar setters without validation. V2 now validates those values and throws `IllegalArgumentException` if the color isn't valid CSS. Accepted formats: hex (`#2196f3`), named colors (`blue`), rgb/rgba, hsl/hsla, and CSS variables (`var(--color)`).

If you're passing hardcoded colors, you're probably fine. But if you accept user input, wrap the call in a try-catch:

```java
// V1: silently accepted any string
tables.setTableHoverColor("not-a-color"); // no error

// V2: validates and throws
try {
    tables.setTableHoverColor(userColor);
} catch (IllegalArgumentException e) {
    // handle invalid color
}
```

---

## Migration Steps

Walk through these in order. Most are straightforward search-and-replace — your IDE will do the heavy lifting.

**1. Update pom.xml**
```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-tables</artifactId>
    <version>2.0.0</version>
</dependency>
```
Also upgrade to Vaadin 25.0.x and ERTE 6.0.0.

**2. Update template parsing**
```java
// Change all calls
ObjectNode templates = TemplateParser.parseJson(jsonString); // was: Json
// Add import: tools.jackson.databind.node.ObjectNode
```

**3. Update template JSON access**
```java
// Change from Json API to Jackson
JsonNode template = templates.get("myTemplate"); // was: getObject()
String name = template.get("name").asText(); // was: getString()
```

**4. Update custom Delta parsers (if applicable)**

Only needed if you parse table deltas outside of ERTE (e.g., for PDF export). Remove any logic that relies on the `"0":"T"` through `"4":"E"` attributes — see [Breaking Change 2](#2-table-delta-format-change).

**5. Handle color validation**

If you pass user-provided colors to `setTableHoverColor()` or `setTableFocusColor()`, add error handling — V2 now validates and throws `IllegalArgumentException` for invalid values:

```java
try {
    tables.setTableHoverColor(userColor);
} catch (IllegalArgumentException e) {
    showErrorDialog("Invalid color: " + e.getMessage());
}
```

---

## New Features in V2

New in V2:

- **CSS custom properties** (`--vaadin-erte-table-*`) for fine-grained control over borders, padding, and selection colors
- **Better keyboard navigation** within tables (Quill 2 improvement)

---

## Known Limitations & Notes

- **Undo/redo:** Undoing a table removal may produce slightly different row ordering. This is a Quill 2 limitation, not an ERTE bug.
- **All V1 features supported** — no features have been dropped in V2.

---

## Validation Checklist

Before deploying V2 to production:

- [ ] Dependencies updated in `pom.xml`
- [ ] All `Json` → `ObjectNode` references updated
- [ ] Custom Delta parsers updated (no more `"0":"T"` character-spread attributes)
- [ ] Color values validated (test invalid colors to confirm exceptions on hover/focus setters)
- [ ] Stored templates reloaded and verified visually
- [ ] Events fire correctly (add/delete/update listeners)
- [ ] Custom CSS properties work (test via theme CSS)

---

## Getting Help

- **Detailed API:** See [TABLES_GUIDE.md](TABLES_GUIDE.md)
- **Architecture:** See [ARCHITECTURE.md](dev/ARCHITECTURE.md)
- **Issues:** Open an issue on the ERTE GitHub repo

