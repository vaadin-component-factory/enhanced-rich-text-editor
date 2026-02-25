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
| Maven artifact | `enhanced-rich-text-editor-tables` | `enhanced-rich-text-editor-tables-v25` |
| Package | `com.vaadin.componentfactory.erte.tables` | (same) |
| License | CVALv3 | CVALv3 |

---

## Breaking Changes

### 1. Jackson 3 Migration (Json → ObjectNode)

**V1 used Vaadin's elemental JSON API.** V2 uses Jackson 3 from the tools namespace.

**Before (V1):**

```java
// V1: Parse with Vaadin's Json
Json parsedJson = TemplateParser.parseJson(jsonString);
tables.setTemplates(parsedJson);

// V1: Read/write with Json API
Json tableNode = templates.getObject("myTemplate");
String name = tableNode.getString("name");
```

**After (V2):**

```java
// V2: Parse with Jackson ObjectNode
ObjectNode templates = TemplateParser.parseJson(jsonString);
tables.setTemplates(templates);

// V2: Read/write with Jackson API
JsonNode tableNode = templates.get("myTemplate");
String name = tableNode.get("name").asText();
```

**What changed:**
- `Json` → `ObjectNode` (from `tools.jackson.databind.node`)
- `TemplateParser.parseJson()` now returns `ObjectNode` instead of `Json`
- All Jackson methods are in the `tools.jackson` namespace (not `com.fasterxml.jackson`)

### 2. I18n Field Names (TextFields in V25 Use Labels, Not Placeholders)

Vaadin 25's `TextField` uses labels instead of placeholders for form hints.

**Before (V1):**

```java
i18n.setInsertTableRowsFieldPlaceholder("Number of rows");
i18n.setInsertTableColumnsFieldPlaceholder("Number of columns");
```

**After (V2):**

```java
i18n.setInsertTableRowsFieldLabel("Rows");
i18n.setInsertTableColumnsFieldLabel("Columns");
```

**What changed:**
- Method names changed from `setInsertTableRowsFieldPlaceholder` → `setInsertTableRowsFieldLabel`
- Field labels now appear above the input (Vaadin 25 behavior), not as placeholder text inside

### 3. Color Validation (Invalid Colors Now Throw Exceptions)

**V1 silently accepted invalid colors.** V2 validates and throws exceptions.

**Before (V1):**

```java
// V1: invalid color was silently accepted
tables.setTableHoverColor("not-a-color");
```

**After (V2):**

```java
// V2: invalid color throws IllegalArgumentException
try {
    tables.setTableHoverColor("not-a-color"); // throws!
} catch (IllegalArgumentException e) {
    log.error("Invalid color: {}", e.getMessage());
}

// Valid formats:
tables.setTableHoverColor("#2196f3"); // hex
tables.setTableHoverColor("blue"); // named
tables.setTableHoverColor("rgb(33, 150, 243)"); // rgb/rgba
tables.setTableHoverColor("var(--my-color)"); // CSS variables
```

**Accepted color formats:** hex (`#fff`, `#ffffff`), named colors (`red`, `blue`), `rgb()`/`rgba()`, `hsl()`/`hsla()`, CSS variables `var(--name)`.

### 4. Template ID Validation (Now Enforced)

**V1 accepted any string as template ID.** V2 validates IDs as CSS class names.

**Before (V1):**

```java
// V1: accepted any string
tables.insertTableAtCurrentPosition(3, 3, "my-template@123"); // OK
```

**After (V2):**

```java
// V2: only [A-Za-z][A-Za-z0-9\-]* allowed
tables.insertTableAtCurrentPosition(3, 3, "my-template@123"); // throws!
tables.insertTableAtCurrentPosition(3, 3, "my-template-123"); // OK
```

**Valid template IDs:** must start with a letter, contain only letters, numbers, and hyphens. Examples: `blue`, `header-row-1`, `myTemplate2`.

---

## Migration Steps

### Step 1: Update Dependencies

Change your `pom.xml`:

```xml
<!-- Remove V1 -->
<!-- <dependency>
    <groupId>org.vaadin.addons.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-tables</artifactId>
    <version>5.x</version>
</dependency> -->

<!-- Add V2 -->
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>enhanced-rich-text-editor-tables-v25</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

Also ensure you're on Vaadin 25.0.x and ERTE V25 6.0.0-SNAPSHOT.

### Step 2: Update Template Parsing

Search for all calls to `TemplateParser.parseJson()`:

**Before:**

```java
Json templates = TemplateParser.parseJson(jsonString);
```

**After:**

```java
ObjectNode templates = TemplateParser.parseJson(jsonString);
```

Add the import:

```java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
```

### Step 3: Update Template JSON References

If your code reads/writes template JSON, update to Jackson API:

**Before:**

```java
Json template = templates.getObject("myTemplate");
String name = template.getString("name");
template.put("name", "New Name");
```

**After:**

```java
JsonNode template = templates.get("myTemplate");
String name = template.get("name").asText();
((ObjectNode) template).put("name", "New Name");
```

### Step 4: Update I18n Labels

Find all `TablesI18n` setters with "Placeholder" in the name:

**Before:**

```java
i18n.setInsertTableRowsFieldPlaceholder("Rows");
i18n.setInsertTableColumnsFieldPlaceholder("Columns");
```

**After:**

```java
i18n.setInsertTableRowsFieldLabel("Rows");
i18n.setInsertTableColumnsFieldLabel("Columns");
```

### Step 5: Add Color Validation

If your code sets colors programmatically, add try-catch:

**Before:**

```java
String userColor = getUserInput(); // might be invalid
tables.setTableHoverColor(userColor);
```

**After:**

```java
String userColor = getUserInput();
try {
    tables.setTableHoverColor(userColor);
} catch (IllegalArgumentException e) {
    showErrorDialog("Invalid color: " + e.getMessage());
}
```

Or validate before setting:

```java
if (TemplateJsonConstants.isValidColor(userColor)) {
    tables.setTableHoverColor(userColor);
} else {
    showErrorDialog("Invalid color format");
}
```

### Step 6: Update Template IDs

Search your code for hardcoded template IDs. Rename any that don't match the pattern `[A-Za-z][A-Za-z0-9\-]*`:

**Before:**

```java
tables.insertTableAtCurrentPosition(3, 3, "template@v1");
tables.insertTableAtCurrentPosition(3, 3, "default.template");
```

**After:**

```java
tables.insertTableAtCurrentPosition(3, 3, "template-v1");
tables.insertTableAtCurrentPosition(3, 3, "default-template");
```

Also update any stored template JSON:

```json
{
  "template@v1": { ... }
}
```

to:

```json
{
  "template-v1": { ... }
}
```

### Step 7: Test and Validate

1. **Build:** `mvn clean install -DskipTests`
2. **Start server:** `bash v25-server-start.sh`
3. **Run tests:** `cd enhanced-rich-text-editor-demo && npx playwright test tests/erte/tables.spec.ts`
4. **Visual check:** Navigate to `/erte-test/tables` in the demo and verify:
   - Tables can be inserted
   - Rows/columns can be added/removed
   - Cell selection works
   - Templates load and apply correctly
5. **Integration test:** Load your stored templates and verify they display correctly

---

## New Features in V2

You don't have to use them, but they may be useful:

### 1. CSS Custom Properties (11 total)

V2 provides CSS variables for table styling:

```css
vcf-enhanced-rich-text-editor {
  --vaadin-erte-table-border-color: #e0e0e0;
  --vaadin-erte-table-cell-padding: 8px 12px;
  --vaadin-erte-table-cell-min-height: 2em;
  --vaadin-erte-table-cell-selected-background: var(--lumo-primary-color-10pct);
  /* ... etc. See TABLES_GUIDE.md Section 6 ... */
}
```

### 2. Programmatic Hover/Focus Colors

V2 makes it easy to set these at runtime without CSS:

```java
tables.setTableHoverColor("var(--lumo-primary-color)");
tables.setTableCellHoverColor("var(--lumo-primary-color-10pct)");
```

### 3. Delta Template ID Scanning

Find which templates are actually in use:

```java
Set<String> used = EnhancedRichTextEditorTables.getAssignedTemplateIds(delta);
```

### 4. Custom Styles Injection

Inject CSS before or after auto-generated template CSS:

```java
tables.setCustomStyles("table { margin: 1em 0; }", true); // before
tables.setCustomStyles("table td { font-size: 14px; }", false); // after
```

### 5. Keyboard Navigation Improvements

V2's Quill 2 integration provides better arrow key and Tab navigation between cells.

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
- **Architecture:** See [/workspace/docs/ARCHITECTURE.md](/workspace/docs/ARCHITECTURE.md)
- **Issues:** Open an issue on the ERTE GitHub repo

