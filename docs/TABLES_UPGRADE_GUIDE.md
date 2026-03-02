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
| Jackson | 2.x (elemental `Json`) | 3.x (`tools.jackson`) |
| Maven artifact | `enhanced-rich-text-editor-tables` | `enhanced-rich-text-editor-tables` |
| Package | `com.vaadin.componentfactory.erte.tables` | (same) |
| License | CVALv3 | CVALv3 |

---

## Breaking Changes

### 1. Jackson 3 Migration (Json → ObjectNode)

```java
// V1: elemental Json
Json parsedJson = TemplateParser.parseJson(jsonString);
String name = templates.getObject("myTemplate").getString("name");

// V2: Jackson 3 ObjectNode
ObjectNode templates = TemplateParser.parseJson(jsonString);
String name = templates.get("myTemplate").get("name").asText();
```

### 2. I18n Field Names (Placeholders → Labels)

Vaadin 25 uses labels. Change: `*FieldPlaceholder` → `*FieldLabel`

```java
// V1: i18n.setInsertTableRowsFieldPlaceholder("Number of rows")
// V2: i18n.setInsertTableRowsFieldLabel("Rows")
```

### 3. Color Validation

V2 validates strictly (throws `IllegalArgumentException`). Accepted: hex (`#2196f3`), named (`blue`), rgb/rgba, hsl/hsla, CSS variables (`var(--color)`). Add try-catch or validate inputs.

### 4. Template ID Validation

V2 requires valid CSS class names: `[A-Za-z][A-Za-z0-9\-]*`. Update hardcoded IDs and stored JSON ("my-template@123" → "my-template-123").

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

**7. Test:** `bash build.sh && bash server-start.sh` then verify tables work

---

## New Features in V2

- **11 CSS custom properties** for table styling
- **Programmatic hover/focus colors**: `tables.setTableHoverColor(...)`
- **Template ID scanning**: `getAssignedTemplateIds(delta)`
- **Custom CSS injection**: `tables.setCustomStyles(...)`
- **Better keyboard navigation** (Quill 2)

---

## Known Limitations & Notes

- **Cell styling UI (planned):** Dialog doesn't provide visual editor yet. Edit JSON directly: `tables.getTemplates()`, modify `"cells"` array, reload.
- **Delta format unchanged:** `td` attribute format same as V1
- **Undo/redo:** Table removal may shuffle row IDs (Quill 2 limitation)
- **All V1 features supported** — no features dropped

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

