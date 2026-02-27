# Tables Upgrade Guide (V1 → V2)

**For developers migrating from ERTE Tables V1 (Vaadin 24 / Quill 1) to ERTE Tables V2 (Vaadin 25 / Quill 2).** This guide walks through breaking changes and migration steps.

---

## Quick Version Matrix

| Aspect | Tables V1 | Tables V2 |
|--------|-----------|-----------|
| ERTE version | 5.x | 6.x |
| Vaadin | 24.x | 25.0.x |
| Java | 17+ | 21+ |
| Quill | 1.3.6 | 2.0.3 |
| Parchment | 1.x | 3.x |
| Jackson | 2.x (elemental `Json`) | 3.x (`tools.jackson`) |
| Maven artifact | `enhanced-rich-text-editor-tables` | `enhanced-rich-text-editor-tables` |
| Package | `com.vaadin.componentfactory.erte.tables` | (same) |
| License | CVALv3 | CVALv3 |

---

## Breaking Changes

### 1. Jackson 3 Migration (Json → ObjectNode)

V1 used Vaadin's elemental JSON API. V2 uses Jackson 3 from the tools namespace.

**Before:**
```java
Json parsedJson = TemplateParser.parseJson(jsonString);
Json tableNode = templates.getObject("myTemplate");
String name = tableNode.getString("name");
```

**After:**
```java
ObjectNode templates = TemplateParser.parseJson(jsonString);
JsonNode tableNode = templates.get("myTemplate");
String name = tableNode.get("name").asText();
```

Changes: `Json` → `ObjectNode`, `getObject()` → `get()`, `getString()` → `get().asText()`. Import from `tools.jackson.databind`.

### 2. I18n Field Names (Labels Not Placeholders)

Vaadin 25's `TextField` uses labels, not placeholders.

**Before:**
```java
i18n.setInsertTableRowsFieldPlaceholder("Number of rows");
```

**After:**
```java
i18n.setInsertTableRowsFieldLabel("Rows");
```

Change: `*FieldPlaceholder` → `*FieldLabel`.

### 3. Color Validation (Invalid Colors Throw Exceptions)

V1 silently accepted invalid colors. V2 validates strictly.

**Before:**
```java
tables.setTableHoverColor("not-a-color"); // silently accepted
```

**After:**
```java
tables.setTableHoverColor("not-a-color"); // throws IllegalArgumentException
tables.setTableHoverColor("#2196f3"); // hex OK
tables.setTableHoverColor("blue"); // named OK
tables.setTableHoverColor("rgb(33, 150, 243)"); // rgb OK
tables.setTableHoverColor("var(--my-color)"); // CSS variables OK
```

Add try-catch or validate inputs before setting.

### 4. Template ID Validation (Strict CSS Class Names)

V1 accepted any string. V2 requires valid CSS class names: start with letter, use only letters/numbers/hyphens.

**Before:**
```java
tables.insertTableAtCurrentPosition(3, 3, "my-template@123"); // OK
```

**After:**
```java
tables.insertTableAtCurrentPosition(3, 3, "my-template@123"); // throws!
tables.insertTableAtCurrentPosition(3, 3, "my-template-123"); // OK
```

Update stored template JSON and hardcoded IDs to match pattern.

---

## Migration Steps

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

**4. Update i18n method names**
```java
// Change all setters from *FieldPlaceholder to *FieldLabel
i18n.setInsertTableRowsFieldLabel("Rows"); // was: setInsertTableRowsFieldPlaceholder
```

**5. Add color validation**
```java
try {
    tables.setTableHoverColor(userColor);
} catch (IllegalArgumentException e) {
    showErrorDialog("Invalid color: " + e.getMessage());
}
```

**6. Rename template IDs**
Search for hardcoded IDs. Must match pattern `[A-Za-z][A-Za-z0-9\-]*`:
```java
tables.insertTableAtCurrentPosition(3, 3, "template-v1"); // was: "template@v1"
```
Also update stored JSON template keys.

**7. Test**
```bash
mvn clean install -DskipTests
mvn -pl enhanced-rich-text-editor-demo spring-boot:run
cd enhanced-rich-text-editor-it && npx playwright test tests/erte/tables.spec.ts
```
Verify tables insert, modify, and apply templates correctly.

---

## New Features in V2

### 1. CSS Custom Properties
11 variables for table styling (see TABLES_GUIDE.md Section 6).

### 2. Programmatic Hover/Focus Colors
```java
tables.setTableHoverColor("var(--lumo-primary-color)");
tables.setTableCellHoverColor("var(--lumo-primary-color-10pct)");
```

### 3. Template ID Scanning
Find which templates are in use:
```java
Set<String> used = EnhancedRichTextEditorTables.getAssignedTemplateIds(delta);
```

### 4. Custom Styles Injection
```java
tables.setCustomStyles("table { margin: 1em 0; }", true); // before
tables.setCustomStyles("table td { font-size: 14px; }", false); // after
```

### 5. Keyboard Navigation
Quill 2 provides better arrow key and Tab navigation between cells.

---

## Known Limitations & Migration Notes

### Limitation: Cell-Level Styling in Dialog (Planned)

V2 supports cell-level styling in templates (x/y coordinates), but the UI dialog doesn't yet provide a visual editor for it. You can:

1. **Edit the JSON directly:** Get templates via `tables.getTemplates()`, modify the `"cells"` array, and reload.
2. **Wait for 6.0.1:** A future release will add cell styling UI.

### Note: Delta Format Is Unchanged

The pipe-separated `td` attribute format is the same between V1 and V2:

```
tableId|rowId|cellId|rowspan|colspan|templateId|uniqueId
```

If you parse deltas outside ERTE, no changes needed (but test thoroughly — Quill 2's serialization may differ slightly).

### Note: Undo/Redo Behavior

Undo/redo of table removal may produce slightly different row ordering compared to V1 (a known Quill 2 limitation). The table structure remains correct, just row IDs may be reshuffled.

### Note: Unsupported V1 Features (if any)

All V1 table features are supported in V2. No features were dropped.

---

## Validation Checklist

Before deploying V2 to production:

- [ ] Dependencies updated in `pom.xml`
- [ ] All `Json` → `ObjectNode` references updated
- [ ] All `Placeholder` → `Label` i18n methods updated
- [ ] Template IDs validated against regex `[A-Za-z][A-Za-z0-9\-]*`
- [ ] Color values validated (test invalid colors to confirm exceptions)
- [ ] Stored templates reloaded and verified visually
- [ ] Test suite passes: `npx playwright test tests/erte/tables.spec.ts`
- [ ] Demo app tables work: `/erte-test/tables`
- [ ] Events fire correctly (add/delete/update listeners test)
- [ ] Custom CSS properties work (test via theme CSS)

---

## Getting Help

- **Detailed API:** See [TABLES_GUIDE.md](./TABLES_GUIDE.md)
- **Architecture:** See [ARCHITECTURE.md](dev/ARCHITECTURE.md)
- **Issues:** Open an issue on the ERTE GitHub repo

