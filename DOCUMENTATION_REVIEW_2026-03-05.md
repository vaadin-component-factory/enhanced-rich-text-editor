# Documentation Review — 2026-03-05

Consolidated findings from 5 parallel review agents (2× Code Reviewer Opus, End-User Reviewer, Docs Engineer, QA Tester). Deduplicated and filtered: **only real errors, factual problems, missing information, and typos/inconsistencies**. Intentional writing style (tone, "that's it", "we recommend", etc.) is explicitly excluded.

---

## Critical (5)

### C1. BASE_USER_GUIDE — No link to Tables Guide
**Location:** `docs/BASE_USER_GUIDE.md` — entire file
**Issue:** The main user guide covers ERTE core features but never mentions the Tables addon anywhere — not in the TOC, not in the intro, not in the "Getting Help" section. A user reading only the User Guide has no path to discover table support.
**Fix:** Add a cross-reference in Section 4 (Getting Help): `See [Tables Guide](TABLES_GUIDE.md) for adding table support (separate addon).`

### C2. EXTENDING — Embed Blot Lifecycle section has no code example and no link to registration
**Location:** `docs/dev/EXTENDING.md`, lines 117–128
**Issue:** The "Embed Blot Lifecycle" section lists three prose rules but — unlike every other section in the file — provides no code example. It also never mentions WHERE to call `Quill.register()` for a new blot. The answer (`extendQuill` hook) is in the previous section, but there's no back-reference. A developer writing their first embed blot would write the class and then not know where to register it.
**Fix:** (a) Add a minimal embed blot skeleton showing `static create()`, constructor with `contentNode`, and the guard node constraint. (b) Add one sentence connecting back: "Register your blot in the `extendQuill` hook (see above) using `Quill.register('formats/your-blot', YourBlot, true)`."

### C3. EXTENDING — `JsonNode` without Jackson 3 import hint
**Location:** `docs/dev/EXTENDING.md`, lines 383–392
**Issue:** The "Building a Java Extension" code sample uses `JsonNode data = event.getEventData()` without mentioning the import. Vaadin 25 uses Jackson 3 (`tools.jackson.databind.JsonNode`), not the familiar Jackson 2 (`com.fasterxml.jackson`). A developer will instinctively add the wrong import. The Tables Upgrade Guide mentions this, but a new user (not an upgrader) reads EXTENDING.md.
**Fix:** Add a comment in the code block: `// import tools.jackson.databind.JsonNode; — Vaadin 25 uses Jackson 3` or a tip box above the example.

### C4. TABLES_GUIDE — `ObjectNode` without Jackson 3 import hint
**Location:** `docs/TABLES_GUIDE.md`, Section 4 (first use of `ObjectNode`, ~line 93)
**Issue:** Same Jackson 3 issue as C3. Section 4 uses `ObjectNode` throughout but never mentions `tools.jackson.databind.node.ObjectNode`. The upgrade guide calls this out — but new users won't read the upgrade guide.
**Fix:** Add a note near the first `ObjectNode` use: "`ObjectNode` is from `tools.jackson.databind.node` (Jackson 3, shipped with Vaadin 25) — not `com.fasterxml.jackson`."

### C5. Java Source — Javadoc `{@link}` references to non-existent method signatures
**Location:** `EnhancedRichTextEditor.java`
**Issue:** Multiple Javadoc `{@link}` references use wrong signatures:
- Line 60, 1388, 1418, 1474, 1487: `{@link #setPlaceholders(List)}` — actual signature is `setPlaceholders(Collection)`
- Line 64: `{@link #setPlaceholderTags(List)}` — actual signature is `setPlaceholderTags(String, String)`
- Line 77: Javadoc example uses `new Placeholder("{{name}}", "user", Map.of(...), Map.of(...))` — no 4-argument constructor exists. Actual constructors: `Placeholder()`, `Placeholder(String)`, `Placeholder(JsonNode)`.

**Fix:** Correct all `{@link}` references to match actual signatures. Replace the 4-arg constructor example with actual API usage.

---

## Major (8)

### M1. TABLES_GUIDE — CSS Custom Properties show hardcoded values as if they were defaults
**Location:** `docs/TABLES_GUIDE.md`, lines 329–342
**Issue:** The CSS Custom Properties code block shows values like `--vaadin-erte-table-border-color: #e0e0e0` and `--vaadin-erte-table-cell-focus-width: 2px` as if they were the actual defaults. The real CSS source uses Vaadin cross-theme tokens:
- Doc: `#e0e0e0` → Actual: `var(--vaadin-border-color, var(--lumo-contrast-30pct))`
- Doc: `var(--vaadin-focus-ring-color)` → Actual: `var(--vaadin-focus-ring-color, var(--lumo-primary-color-50pct))`
- Doc: `2px` → Actual: `var(--vaadin-focus-ring-width, 2px)`
- Doc: `var(--lumo-primary-color-10pct)` → Actual: `var(--lumo-primary-color-10pct, rgba(25, 118, 210, 0.1))`

A reader will believe `#e0e0e0` is the default border color when it actually follows the Vaadin theme.
**Fix:** Show the actual token-based defaults from the CSS source, or clearly label the block as "Example overrides" rather than a property definition list.

### M2. Multiple files — `--` vs `—` inconsistency
**Location:** Titles and prose in multiple files
**Issue:** Most of the suite uses proper em-dash `—`, but several places use double-hyphen `--`:
- `enhanced-rich-text-editor/README.md` line 1: `V25 -- Core Component`
- `enhanced-rich-text-editor-demo/README.md` line 1: `-- Demo Application`
- `README.md` lines 11–12: doc list entries use `--`
- `BASE_USER_GUIDE.md` line 1: title uses `--`
- `BASE_USER_GUIDE.md` lines 271, 281: `**Display tags** -- wrap...`, `**Alt appearance pattern** -- regex...`
- `BASE_USER_GUIDE.md` line 660: `-- see`

**Fix:** Replace `--` with `—` in all listed locations.

### M3. Multiple files — "Tables addon" / "Table addon" / "Tables Extension" inconsistent
**Location:** Across all docs
**Issue:** Five distinct phrasings used interchangeably:
- "Tables Extension addon" (ROOT README)
- "Tables addon" (ROOT README, USER_GUIDE, ARCHITECTURE)
- "Table addon" (EXTENDING — consistently singular)
- "Tables extension" (DEVELOPER_GUIDE)
- "ERTE Tables Extension" (TABLES_GUIDE H1)

**Fix:** Standardize on **"Tables addon"** (plural, one word) in body text everywhere. EXTENDING.md's "Table addon" (singular) → "Tables addon". README's "Tables Extension addon" → "Tables addon". TABLES_GUIDE H1 can stay as-is (title).

### M4. README — Documentation hub missing Tables and Dev docs links
**Location:** `README.md`, lines 9–13
**Issue:** The Documentation section only lists User Guide and Upgrade Guide. No links to TABLES_GUIDE, TABLES_UPGRADE_GUIDE, or any dev docs (ARCHITECTURE, DEVELOPER_GUIDE, EXTENDING). A developer visiting the repo root has no visible entry point to these.
**Fix:** Expand the Documentation section to list all guides, grouped by audience (User / Developer).

### M5. Multiple files — Pro subscription wording inconsistent, USER_GUIDE weakest
**Location:**
- `README.md:26–27`: "A Vaadin Pro subscription or higher is required for production use."
- `enhanced-rich-text-editor/README.md:17–18`: Same (good)
- `BASE_USER_GUIDE.md:46–47`: "Make sure your project uses `vaadin` as dependency." (no mention of Pro)
- `DEVELOPER_GUIDE.md:24`: Full sentence in prerequisites table

**Issue:** The User Guide — the most-read document — is the only one that doesn't explicitly state the Pro subscription requirement.
**Fix:** Strengthen the USER_GUIDE note: `> The base Rich Text Editor is part of the commercial \`vaadin\` artifact (not \`vaadin-core\`). A Vaadin Pro subscription or higher is required for production use.`

### M6. README + module README — Quick Start code duplicated
**Location:** `README.md` lines 30–51 and `enhanced-rich-text-editor/README.md` lines 22–33
**Issue:** Both contain Quick Start Java examples for `EnhancedRichTextEditor`. The root README is more complete; the module README is a subset. When API changes, both need updating independently.
**Fix:** In the core module README, replace the full code block with a one-liner and reference: "For a complete Quick Start, see the [root README](../README.md#quick-start)." Keep only Maven dependency + requirements.

### M7. EXTENDING — Claims Table addon demonstrates non-`ql-` prefix pattern
**Location:** `docs/dev/EXTENDING.md`, line 321
**Issue:** States "The Table addon demonstrates this pattern" (i.e., not using `ql-` prefix for CSS classes). But the Tables addon actually DOES use `ql-` prefixed classes (`ql-cell-selected`, `ql-editor__table--hideBorder`) because it's a first-party extension pre-registered in ERTE core's `ALLOWED_ERTE_CLASSES`.
**Fix:** Clarify: the Tables addon is a first-party extension that pre-registers its classes in ERTE core. Third-party extensions should use non-`ql-` prefixes and register via `addAllowedHtmlClasses()`.

### M8. BASE_UPGRADE_GUIDE — No cross-reference to TABLES_UPGRADE_GUIDE
**Location:** `docs/BASE_UPGRADE_GUIDE.md` — entire file
**Issue:** A developer upgrading ERTE who also uses tables needs both upgrade guides. The base guide mentions tables zero times — no cross-reference.
**Fix:** Add a note in the introduction or migration checklist: `> **Using tables?** Also complete the [Tables Upgrade Guide](TABLES_UPGRADE_GUIDE.md).`

---

## Warning / Significant (10)

### W1. BASE_USER_GUIDE — `getAltAppearance()` returns `Boolean`, not `boolean`
**Location:** `docs/BASE_USER_GUIDE.md`, line 403
**Issue:** Doc shows `boolean isAlt = event.getAltAppearance();` but the method returns `Boolean` (nullable). Auto-unboxing a null `Boolean` throws `NullPointerException`.
**Fix:** Change to `Boolean isAlt = event.getAltAppearance();` or add null check.

### W2. TABLES_UPGRADE_GUIDE — "Jackson" column header misleading for V1
**Location:** `docs/TABLES_UPGRADE_GUIDE.md`, line 15
**Issue:** Version matrix column header says "Jackson" but V1 used Vaadin's elemental Json, not Jackson at all.
**Fix:** Rename column to "JSON Library" with values "elemental Json" (V1) and "Jackson 3" (V2).

### W3. BASE_USER_GUIDE — `asDelta()` not explained
**Location:** `docs/BASE_USER_GUIDE.md`, lines 751–756
**Issue:** Introduces `editor.asDelta()` saying it returns a "wrapper" with `setValue()/getValue()` but never names the return type or says it's inherited from Vaadin's `RichTextEditor`. A developer can't find `asDelta()` in ERTE source.
**Fix:** Add: "The `asDelta()` method (inherited from Vaadin's `RichTextEditor`) returns a wrapper..."

### W4. BASE_USER_GUIDE — `ToolbarSlot` import package not mentioned
**Location:** `docs/BASE_USER_GUIDE.md`, Section 2.1 (~lines 98–119)
**Issue:** Uses `ToolbarSlot.START`, `ToolbarSlot.BEFORE_GROUP_HISTORY` etc. without mentioning that `ToolbarSlot` is in `com.vaadin.componentfactory.toolbar`, not `com.vaadin.componentfactory`.
**Fix:** Add import to first code example: `import com.vaadin.componentfactory.toolbar.ToolbarSlot;` (same for `ToolbarSwitch`, `ToolbarPopover`, `ToolbarSelectPopup`, `ToolbarDialog`).

### W5. BASE_USER_GUIDE — Gate events warning too subtle
**Location:** `docs/BASE_USER_GUIDE.md`, lines 313–315
**Issue:** Says "When you register a listener, the default action is suppressed" but this is easy to miss. A developer adding a listener for logging purposes accidentally prevents the insert/delete action.
**Fix:** Add a warning callout before the flow diagram emphasizing: registering even one gate event listener suppresses the default action. Use notification events for passive observation.

### W6. BASE_USER_GUIDE — Content classes without shadow DOM selector example
**Location:** `docs/BASE_USER_GUIDE.md`, lines 712–723
**Issue:** Lists `.ql-placeholder`, `.ql-tab` etc. and says "use them in your own CSS" but doesn't explain HOW, given shadow DOM. A developer will write `.ql-placeholder { ... }` in app CSS and see nothing happen.
**Fix:** Add a brief CSS example showing how to reach these classes (e.g., via `::part(editor)` or noting they're inside shadow DOM).

### W7. EXTENDING — `executeJs()` without lifecycle hint
**Location:** `docs/dev/EXTENDING.md`, lines 219–231
**Issue:** Shows adding a Quill keyboard binding via `executeJs()` but doesn't say when to call it in the component lifecycle.
**Fix:** Add: "Vaadin queues `executeJs()` until the component is attached — safe to call in the constructor."

### W8. ARCHITECTURE — `lumoInjector` mentioned without clarification for extensions
**Location:** `docs/dev/ARCHITECTURE.md`, lines 93–96
**Issue:** Mentions "ERTE overrides `static get lumoInjector()`" with a warning "easy to miss." An extension author might think they need this too.
**Fix:** Add: "This is handled in ERTE core. Extension authors don't need to override `lumoInjector`."

### W9. TABLES_GUIDE — Cell coordinate convention warning comes after the example
**Location:** `docs/TABLES_GUIDE.md`, line 167
**Issue:** The note that `x` means row and `y` means column (opposite to typical math convention) appears AFTER the 30-line JSON template example.
**Fix:** Move the `x`/`y` convention note immediately BEFORE the JSON example.

### W10. EXTENDING — `disconnectedCallback` assumes web component lifecycle knowledge
**Location:** `docs/dev/EXTENDING.md`, lines 433–444
**Issue:** Shows patching `disconnectedCallback` without explaining when it fires or why cleanup matters.
**Fix:** Add one sentence: "The `disconnectedCallback` fires when the web component is removed from the DOM (e.g., on view navigation). Patch it to clean up event listeners and prevent memory leaks."

---

## Minor (8)

### m1. BASE_USER_GUIDE — "pixels from the left edge, inside any padding" is ambiguous
**Location:** `docs/BASE_USER_GUIDE.md`, line 459
**Fix:** Rephrase: "...in CSS pixels, measured from the left edge of the text content area (after any editor padding)."

### m2. BASE_UPGRADE_GUIDE — Section 2.9 scope note comes at the end instead of the start
**Location:** `docs/BASE_UPGRADE_GUIDE.md`, lines 193–197
**Fix:** Move "Only relevant if you develop your own ERTE extension" to the START of section 2.9.

### m3. BASE_UPGRADE_GUIDE — Section 3 opener repeats the heading
**Location:** `docs/BASE_UPGRADE_GUIDE.md`, line 201–203: `## 3. New in ERTE 2` followed by `New in ERTE 2:`
**Fix:** Replace the redundant opener with a summary sentence.

### m4. TABLES_GUIDE — "Dimension Units" and "Injecting Custom CSS" not in Table of Contents
**Location:** `docs/TABLES_GUIDE.md`
**Fix:** Add both as subsections under Section 4 in the TOC.

### m5. ARCHITECTURE — "globally" deserves clarification
**Location:** `docs/dev/ARCHITECTURE.md`, line 64: "registered globally via `Quill.register()`"
**Fix:** Add: "once per page load — all editor instances on the page share the same blot registry."

### m6. ARCHITECTURE — Key Source Files table omits Tables addon files
**Location:** `docs/dev/ARCHITECTURE.md`, lines 118–132
**Fix:** Add a cross-reference: "Tables addon source files are covered in the [Tables Guide](../TABLES_GUIDE.md)."

### m7. TABLES_UPGRADE_GUIDE — Inconsistent `./` prefix on cross-reference
**Location:** `docs/TABLES_UPGRADE_GUIDE.md`, line 152: `./TABLES_GUIDE.md` vs line 153: `dev/ARCHITECTURE.md`
**Fix:** Remove `./` prefix for consistency.

### m8. Dev docs — V25 in titles inconsistent
**Location:** ARCHITECTURE.md and EXTENDING.md include "V25" in H1, DEVELOPER_GUIDE.md does not.
**Fix:** Either add "V25" to DEVELOPER_GUIDE or remove from all three (preferred — they all implicitly target V25).

---

## Excluded from this report

The following agent suggestions were intentionally excluded as they concern writing style/tone, which is by design:
- "That's it" phrases (EXTENDING.md, TABLES_GUIDE.md)
- "We recommend" (TABLES_GUIDE.md)
- "Think of them as CSS classes" (TABLES_GUIDE.md)
- "No prior experience with Quill tables required" (TABLES_GUIDE.md)
- Casual/warm tone patterns throughout
- First-person plural usage

---

## Statistics

| Agent | Model | Tool Calls | Duration | Findings |
|-------|-------|-----------|----------|----------|
| Code Reviewer A (User Docs) | Opus | 43 | 3.6 min | 1 |
| Code Reviewer B (Tables+Dev) | Opus | 65 | 4.1 min | 5 |
| End-User Reviewer | Sonnet | 67 | 4.8 min | 18 |
| Docs Engineer | Sonnet | 47 | 4.4 min | 28 |
| QA Tester | Sonnet | 159 | 11.9 min | 3 (+85 PASS) |

Total unique findings after deduplication: **31** (5 Critical, 8 Major, 10 Warning, 8 Minor)
