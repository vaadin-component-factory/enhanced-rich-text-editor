# Migrating ERTE to Vaadin 25

## Terminology

| Term | Meaning |
|------|---------|
| **ERTE** | Enhanced Rich Text Editor — this project |
| **RTE** | Vaadin's built-in Rich Text Editor (commercial component) |
| **RTE 1** | The RTE in Vaadin 24 and earlier, based on Quill 1 |
| **RTE 2** | The RTE starting from Vaadin 25, based on Quill 2 |
| **ERTE 1** | The current (unmigrated) ERTE, built on top of RTE 1 |
| **ERTE 2** | The target: migrated ERTE, built on top of RTE 2 |

## Background

ERTE 1 was originally forked from RTE 1. Over time, the codebase gap between the two has grown — new features and fixes added to RTE 1 were never ported back into ERTE 1. Additionally, Quill 1 is now outdated and RTE 2 has moved to Quill 2.

The goal of this document is to describe the migration from ERTE 1 to ERTE 2, using RTE 2 as the new base.

## Non-Regression

All features currently available in ERTE 1 must remain available in ERTE 2. The complete feature inventory is documented in Step 0 below. Every feature must be validated against this inventory after migration.

A previous attempt simply cut out features like the placeholder dialog. This is not acceptable. For the user, there must be no feature regression. If you come to a feature where no clear path of migration is obvious, you are obliged to ASK FOR NEXT STEPS.

## Updatability

A key goal is to avoid repeating the mistake of maintaining a full copy of the RTE codebase. Instead of forking, ERTE 2 should **extend** RTE 2, so that future updates, fixes, and new features in RTE 2 are automatically inherited by ERTE 2.

This means ERTE 2 will not copy and modify the RTE source code. Instead, it should hook into the existing codebase at runtime wherever possible.

The decided approach is **JS class extension**: ERTE 2's web component extends RTE 2's class, overriding `render()` for toolbar customization and using `ready()` hooks for Quill configuration. This is the only template-level "fork" — all other extension happens through Quill's public APIs (blot registration, keyboard bindings, clipboard matchers).

This approach was validated by a comprehensive spike (22 items, all PASS). See `SPIKE_RESULTS.md` for full results. Key confirmed findings:
- RTE 2 uses open Shadow DOM (accessible via `element.shadowRoot`)
- Quill instance is available as `element._editor` (not #private), set during `super.ready()`
- Global `Quill.register()` before element creation works (RTE 2 uses this pattern itself)
- RTE 2 toolbar has zero `<slot>` elements — `render()` override is the clean solution
- Toolbar buttons (including custom ones) survive all Lit re-render scenarios
- `::part()` selectors work with natural tag-specific isolation
- Production build works out of the box
- Table blots are portable to Parchment 3 (5 breaking changes documented)

SIDE NOTE: I would like to see the toolbar not plain copied, but with a "injection" approach of injecting the custom slots into the
resulting template from super.render(), so that we prevent too much copy and paste. This has not been tested in a spike yet and should be checked,
as it may allow to not need to copy the whole html, but simply work with Lits template result dynamically.

## Decided Blockers

These questions were blockers before the migration. They have been resolved by source code analysis and **confirmed by spike** (see `SPIKE_RESULTS.md`).

| Decision | Answer                                                                                                       | Spike Item |
|----------|--------------------------------------------------------------------------------------------------------------|------------|
| RTE 2 framework | Lit (LitElement with PolylitMixin), NOT Polymer.                                                             | 10 |
| Shadow DOM | Open — fully accessible                                                                                      | 4, 12 |
| Quill version in RTE 2 | 2.0.3 (vendored at `vendor/vaadin-quill.js`)                                                                 | 2 |
| Toolbar extensibility | No slots. `render()` override required. Survives all re-renders.                                             | 4, 5, 13 |
| Pre-Quill-init hook | None in RTE 2. Global `Quill.register()` before import is the solution.                                      | 2, 10 |
| Value format | HTML-primary with `asDelta()` wrapper. Clean break from ERTE 1.                                              | 3, 21 |
| Table extension strategy | Rewrite blots for Quill 2. 5 Parchment 3 breaking changes identified. Java untouched. Delta format preserved. | Phase 5 |
| Java version | 21+ required by Vaadin 25. Spring Boot 4.x required.                                                         | 8 |
| RTE is Pro component | Requires `vaadin` artifact (not `vaadin-core`). Pro subscription needed.                                     | 8 |
| Theme inheritance | CSS custom properties cascade through shadow DOM. No `registerStyles()` needed.                              | 11, 12 |
| Guard nodes | Inside embed element. Measure outer `.ql-tab` rect. Delta unaffected.                                        | 20, 22 |
| Keyboard bindings | String key names. Priority via clear-and-prepend pattern.                                                    | 6, 7 |
| Production build | Works out of the box with custom `@JsModule`.                                                                | 23 |

## Open Decisions

> ⚠️ These must be answered before or during implementation:

1. **Phased delivery:** Can ERTE 2 ship without tables initially? (Recommended: yes, tables are Step 4)
2. **Data migration:** Must ERTE 1 deltas load in ERTE 2 unchanged? Or is a server-side batch migration acceptable? -> 
3. **Test strategy:** How will feature parity be validated? Existing Playwright tests cover tab stops only. Placeholders, read-only sections, toolbar customization, and tables have zero automated tests.

## Migration Steps

### Step 0: Use Case Analysis (prerequisite)

Before any migration work begins, every ERTE 1 feature must be checked against RTE 2 and Quill 2 to determine:

- **Already available in RTE 2 / Quill 2?** → No migration needed, just verify behavior.
- **Not available, clear migration path?** → Document the approach.
- **Not available, no clear path?** → Flag and ask for next steps before proceeding.

The detailed comparison is in `feature_comparison.md`. Summary: of 20 features, 3 are free (inherited from RTE 2), 17 need custom migration.

#### Complete ERTE 1 Feature Inventory

**Custom Slots / Custom Components**
Allows adding custom Java components (buttons, or arbitrary components) to the editor toolbar via custom slots. This is the primary extensibility mechanism and the architectural foundation for several other features.

**Readonly Mode**
Programmatic control to toggle the editor into a non-editable state. Note: RTE 2 has whole-editor readonly. ERTE also has *inline* read-only sections (ReadOnlyBlot) — these are separate features.

**Tabstops**
Tabstops can be set in the UI by clicking on a horizontal ruler above the editor. Three directions are supported:
- **Left:** left side of text aligns to right side of tab stop (>text)
- **Right:** right side of text aligns to left side of tab stop (text<)
- **Middle:** text is centered on the tab stop (te|xt)

Clicking the ruler creates a left tabstop. Clicking an existing tabstop cycles through left → right → middle. Tabstops can also be set programmatically via the `tabStops` property. Tightly coupled with: Rulers (horizontal/vertical), Soft-break system (Shift+Enter with tab copying).

**Placeholders**
Insertable placeholder tokens with a dedicated dialog, combo box, alt appearance toggle, keyboard shortcut (Ctrl+P), and full lifecycle events. This is the second most complex ERTE feature after tabstops.

**Non-Breaking Space**
Pressing `Shift+Space` inserts a non-breaking space at the caret position.

**Toolbar Button Visibility**
`setToolbarButtonsVisibility` allows showing/hiding individual standard toolbar buttons programmatically.

**Icon Replacement**
Each standard toolbar button includes a slot for its icon, allowing icon replacement from Java.

**Soft Wraps**
Visual indication / control of soft line wraps in the editor.

**Whitespace Indicators**
Visual display of whitespace characters (spaces, tabs, paragraph marks, etc.) via CSS pseudo-elements. Toggle via toolbar button.

**Inline Read-Only Sections**
ReadOnlyBlot — inline `contenteditable=false` spans with delete protection. Separate from whole-editor readonly.

**Custom Keyboard Shortcuts**
`addStandardButtonBinding` / `addToolbarFocusBinding` — bind key combos to toolbar actions.

**Programmatic Text Insertion**
`addText(String, int)` — insert text at specific position or cursor via Quill API.

**I18n (ERTE-specific labels)**
30+ translatable strings including ERTE-specific ones (readonly, placeholder, whitespace, etc.).

**List Indentation Buttons**
Explicit indent/outdent toolbar buttons (Quill handles Tab-key indentation natively).

**Align Justify**
Justify alignment button (RTE 2 only has left/center/right).

**HTML Sanitization**
Extended Jsoup safelist with ERTE-specific class filtering.

**Extension Hook (extendOptions)**
Pre-Quill-instantiation callback for extensions (used by tables addon). Must be recreated or replaced.

**Tables (separate addon)**
Table support provided by the Table Extension. Includes merge/split, templates, cell selection, history. ~4000 lines JS, ~3500 lines Java. Originates from a fork chain with no Quill 2 port.

### Step 1: Update the Project Base to Vaadin 25

Update to stable Vaadin 25.0.x (no pre-releases). This includes all dependencies, Maven profiles, Dockerfile, test infrastructure — nothing may be dropped silently.

Key dependency changes:
- Java 17 → 21+
- Vaadin 25.0.x: `RichTextEditor` is now a **Pro component** (requires `vaadin` artifact, not `vaadin-core`)
- Spring Boot 3 → **4.x** (4.0.2 confirmed in spike)
- `vaadin-dev` must be explicitly added for dev mode
- Mockito 1.x + PowerMock → Mockito 5.x (PowerMock incompatible with Java 21)
- Jackson: `com.fasterxml.jackson` → `tools.jackson`
- Dockerfile: `eclipse-temurin:17` → `eclipse-temurin:21`
- Remove vendored Quill 1.3.6 (RTE 2 ships Quill 2.0.3)
- Lumo: no longer auto-loaded, host app needs `@StyleSheet(Lumo.STYLESHEET)`

**Done when:** The V25 modules are created with all dependencies up to date and the project compiles cleanly.

### Step 2: Rebuild ERTE on Top of RTE 2

Create the ERTE 2 shell: JS subclass extending RTE 2's web component, Java subclass extending `RichTextEditor`.

**JS side:** Override `render()` to provide ERTE's toolbar template (standard RTE 2 groups + ERTE groups + slots). Override `ready()` for post-Quill-init setup. Register all custom blots globally before element creation. Use `@click` Lit event handlers on custom buttons. Use optional chaining for `__effectiveI18n` properties (may be undefined during initial render).

**Java side:** `EnhancedRichTextEditor extends RichTextEditor` in package `com.vaadin.flow.component.richtexteditor`. Own `@Tag("vcf-enhanced-rich-text-editor")` and `@JsModule`. No `@NpmPackage` needed — inherited from parent.

**RteExtensionBase:** If package-private methods need elevation, create a minimal bridge class in the RTE package. ⚠️ This class must be reviewed on every RTE update — if Vaadin changes the underlying methods, it breaks. Note: `sanitize()` and `runBeforeClientResponse()` are already accessible from same package (confirmed in spike Items 16, 19).

**Styling:** Follow V25 conventions. No Polymer patterns. Lumo loads via `@StyleSheet(Lumo.STYLESHEET)` on the host app — CSS custom properties cascade through shadow DOM without per-tag registration. ERTE 1's Polymer-based `<custom-style>` and `registerStyles()` must be rewritten as Lit `static get styles()` adopted stylesheets. See `SPIKE_RESULTS.md` Items 11, 12.
note: ERTE 1 has also custom stylings in js files. These styles also need to be rewritten in an V25 appropriate way. For this an additional analysis on how the RTE 2 defines its styles (base and theme related) has to be done.

**Sanitizer:** RTE 2's `sanitize()` strips `class` from `<span>`. Must override to whitelist ERTE-specific classes (`ql-tab`, `ql-placeholder`, `ql-readonly`, `ql-soft-break`). See `SPIKE_RESULTS.md` Item 19 and `SECURITY.md`.

**Done when:** An empty ERTE 2 renders with the standard RTE 2 toolbar, is controllable from Java (readonly, disabled, setValue), Lit lifecycle works correctly (toolbar survives property changes), and production build succeeds. All of this was verified in the spike.

### Step 3: Feature Migration

Migrate features one at a time in the following order. This order is intentional — custom slots come first because they establish the runtime-injection pattern that other features depend on.

1. **Custom Slots / Custom Components** — architectural foundation, must work first
2. **Readonly Mode** (whole-editor + inline ReadOnlyBlot)
3. **Tabstops** (including Rulers, Soft-break system — tightly coupled)
4. **Placeholders**
5. Remaining features in the following order:
    - Soft-break system / soft wraps
    - Extension hook (extendOptions replacement)
    - HTML sanitization
    - Non-breaking space
    - Toolbar button visibility
    - Icon replacement
    - Whitespace indicators
    - Custom keyboard shortcuts
    - List indentation buttons
    - Align justify
    - Programmatic text insertion
    - I18n extension

For each feature:
1. Read the corresponding section in `feature_comparison.md`
2. Check if RTE 2 / Quill 2 already provides it
3. Consult `quill_v1_to_v2_api_diff.md` for any Quill API changes
4. Consult `SPIKE_RESULTS.md` for confirmed patterns (especially: keyboard binding priority → Item 7, guard node measurement → Item 20, lifecycle timing → Item 14)
5. Consult `SECURITY.md` for known vulnerabilities to fix
6. Implement, test, verify

### Step 4: Table Extension (last)

Rewrite table blots for Quill 2 / Parchment 3. The Java server-side code (~3,500 LOC) stays completely untouched — it is format-agnostic. The delta format (pipe-separated `td` attribute) is preserved, so existing table data remains compatible.

The table spike (Phase 5 in `SPIKE_RESULTS.md`) confirmed feasibility and identified 5 critical Parchment 3 breaking changes:
1. `Parchment.create()` → removed, use `this.scroll.create(blotName, value)`
2. `replace()` → `replaceWith()` (reversed semantics)
3. `defaultChild` must be a class reference, not a string
4. `checkMerge()` must be overridden to prevent unwanted merging of same-tag blots
5. Merge loops in `optimize()` need `while` instead of `if` for 3+ siblings

Additional changes: `domNode.__blot.blot` → `Quill.find(domNode)`, TableHistory complete redesign (Quill 2 history stack uses `{delta, range}` not `{undo, redo}`), `ContainBlot.formats()` must return `{}` not tagName string.

**Feasibility must be assessed first.** If the rewrite approach is not viable, the fallback is migrating the existing backported fork directly to Quill 2 / Parchment 3.

**Done when:** Tables render, merge/split works, templates work, undo/redo works, existing table deltas load correctly.

### Test Strategy

> ⚠️ TODO: Define before Step 3 begins.

Current state:
- 75 Playwright tests for tab stops (comprehensive, reusable if selectors updated)
- 21 JUnit tests for TabConverter
- Basic JUnit tests for core component (need JUnit 4 → 5 migration)
- Zero automated tests for: placeholders, read-only sections, toolbar customization, tables

Minimum requirement: every feature in the inventory must have at least one automated test before it can be considered migrated.
