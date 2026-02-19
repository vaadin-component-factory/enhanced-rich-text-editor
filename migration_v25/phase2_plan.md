# Phase 2: ERTE Shell — Implementation Plan

## Goal

Stock RTE 2 wrapped in ERTE's own web component tag (`vcf-enhanced-rich-text-editor`). **Zero visual difference** to plain `RichTextEditor` after Phase 2. The shell proves the JS extension mechanism works end-to-end; features are added in Phase 3+.

## Acceptance Criteria

1. `EnhancedRichTextEditor` renders identically to `RichTextEditor`
2. Tag in DOM is `vcf-enhanced-rich-text-editor` (not `vaadin-rich-text-editor`)
3. All standard RTE 2 functionality works (typing, formatting, undo/redo, color, link, image)
4. Content settable via Java API (`setValue(htmlString)` for HTML, `asDelta().setValue(deltaJson)` for Delta)
5. `_editor` (Quill instance) accessible after `super.ready()`
6. Dev bundle builds without errors
7. `mvn clean install -DskipTests` succeeds
8. Demo view shows ERTE with no console errors (except Vaadin Copilot dev-mode noise)
9. Automated Playwright test passes (tag, toolbar, typing, setValue, Delta round-trip)
10. Theme variants work (`LUMO_COMPACT`, `LUMO_NO_BORDER`)

## Architecture (confirmed by spike)

```
JS Layer:
  @vaadin/rich-text-editor (RTE 2 package entry point)
    └── vcf-enhanced-rich-text-editor.js (ERTE shell — new file)
         - extends RTE 2 class via customElements.get()
         - render() { return super.render(); }  ← passthrough
         - static get styles() { return super.styles ? [...super.styles] : []; }
         - customElements.define('vcf-enhanced-rich-text-editor', ...)

Java Layer:
  RichTextEditor (com.vaadin.flow.component.richtexteditor)
    └── RteExtensionBase (same package — bridge, lifts package-private → protected)
         └── EnhancedRichTextEditor (com.vaadin.componentfactory)
              - @Tag("vcf-enhanced-rich-text-editor")
              - @JsModule("./vcf-enhanced-rich-text-editor.js")
```

### Bridge Lifting Rule (applies to all phases)

When `RteExtensionBase` needs to expose a package-private member from `RichTextEditor`, **always use a visibility-widening `@Override protected`** that calls `super.xxx()`. Never invent new method names with prefixes like `erteXxx()`.

```java
// CORRECT — visibility-widening override
@Override
protected void runBeforeClientResponse(SerializableConsumer<UI> command) {
    super.runBeforeClientResponse(command);
}

// WRONG — prefix rename
protected void erteRunBeforeClientResponse(SerializableConsumer<UI> command) { ... }
```

This keeps the API clean: `EnhancedRichTextEditor` calls `runBeforeClientResponse()` like any normal inherited method. Same pattern for `sanitize()` in Phase 3 and any future lifts.

## Verified API Facts (from bytecode analysis of RTE 2 v25.0.5)

| Member | Visibility | Notes |
|--------|-----------|-------|
| `RichTextEditor()` | public constructor | No-arg, validated in spike Item 8 |
| `setValue(String)` | public | Accepts HTML. Throws `IllegalArgumentException` if value starts with `[` or `{` (Delta guard). Use `asDelta().setValue()` for Delta JSON. |
| `asDelta()` | public | Returns `HasValue<..., String>` wrapper for Delta format |
| `asHtml()` | public | Returns `HasValue<..., String>` wrapper (bypasses Delta guard) |
| `runBeforeClientResponse(SerializableConsumer<UI>)` | **package-private** | Must be lifted in `RteExtensionBase` |
| `sanitize(String)` | **static package-private** | Must be lifted — but deferred to Phase 3 (not needed for shell) |
| `static get properties()` | inherited from Lit | Reactive properties inherited via prototype chain, no override needed |

## Steps

### Step 1: Create JS subclass (`vcf-enhanced-rich-text-editor.js`)

**File:** `enhanced-rich-text-editor-v25/src/main/resources/META-INF/resources/frontend/vcf-enhanced-rich-text-editor.js`

This is the standard Vaadin addon frontend resource path (`META-INF/resources/frontend/`), same convention as V24 and confirmed working in V25 by the spike. The file needs a CVALv3 license header (the Maven license plugin only covers `*.java`, so the JS header must be added manually).

**Content:**
```javascript
/*-
 * #%L
 * Enhanced Rich Text Editor V25
 * %%
 * Copyright (C) 2019 - 2025 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file license.html distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */

/**
 * ERTE V25 shell — extends RTE 2's web component with ERTE's own tag.
 * Phase 2: pure passthrough. Features added in Phase 3+.
 *
 * Import path: package entry point (@vaadin/rich-text-editor), NOT the
 * internal /src/ path. customElements.get() decouples from internal module
 * structure. Import path is stable as of Vaadin 25.0.5.
 */
import '@vaadin/rich-text-editor';

const RteBase = customElements.get('vaadin-rich-text-editor');
if (!RteBase) {
  throw new Error(
    'vcf-enhanced-rich-text-editor: vaadin-rich-text-editor not registered. '
    + 'Ensure @vaadin/rich-text-editor is loaded first.'
  );
}

class VcfEnhancedRichTextEditor extends RteBase {

  static get is() {
    return 'vcf-enhanced-rich-text-editor';
  }

  static get styles() {
    // Defensive: guard against undefined (shouldn't happen, RTE 2 has 5 sheets)
    return super.styles ? [...super.styles] : [];
  }

  /** @protected */
  render() {
    return super.render();
  }

  /**
   * Vaadin-specific lifecycle hook (inherited from Polymer compat layer,
   * NOT a standard Lit lifecycle method). Called from within the Lit update
   * cycle after connectedCallback → willUpdate → firstUpdated → updated.
   * _editor (Quill instance) is available immediately after super.ready().
   * See SPIKE_RESULTS.md Item 14 for full lifecycle timeline.
   * @protected
   */
  ready() {
    super.ready();
    // _editor (Quill instance) is now available.
    // Phase 3+ will initialize ERTE features here.
    console.debug('[ERTE] Shell ready, _editor:', !!this._editor);
  }
}

customElements.define(VcfEnhancedRichTextEditor.is, VcfEnhancedRichTextEditor);

// Exported for potential extension by tables addon or test utilities.
export { VcfEnhancedRichTextEditor };
```

**Key decisions:**
- **Package entry point import** (`@vaadin/rich-text-editor`), NOT deep `/src/` path — decouples from Vaadin's internal file layout
- **Guard on `customElements.get()`** — turns cryptic "extends undefined" into clear diagnostic
- **Defensive `super.styles` check** — `super.styles ? [...super.styles] : []` guards against undefined (unlikely but safe)
- `customElements.get()` to get the actual class — avoids tight coupling to module export structure
- `render()` passthrough — proves the override mechanism, ready for Phase 3 toolbar additions
- `ready()` override — documented as Vaadin-specific hook, not Lit lifecycle. Includes `console.debug` canary for Phase 3 debugging (remove before release)
- No `@NpmPackage` needed — inherited from parent (spike Item 17). V24's `@NpmPackage(vaadin-license-checker)` is also dropped (license checking changed in V25)
- No `@JavaScript` annotation needed — that was for the V24 Polymer-era connector script (`richTextEditorConnector.js`), not applicable in V25/Lit
- Export — kept for potential extension by tables addon, documented with rationale
- **CVALv3 license header** — added manually since Maven plugin only covers `*.java`

### Step 2: Update `RteExtensionBase.java` — lift `runBeforeClientResponse` only

**File:** `enhanced-rich-text-editor-v25/src/main/java/com/vaadin/flow/component/richtexteditor/RteExtensionBase.java`

**Bytecode analysis confirmed:** `runBeforeClientResponse(SerializableConsumer<UI>)` is **package-private** on `RichTextEditor`. It is NOT the same as `Component.getElement().executeJs()` — it is a convenience wrapper for deferred JS execution via `beforeClientResponse()`. Must be lifted for ERTE to use it from `com.vaadin.componentfactory`.

**`sanitize(String)` is also package-private** but is NOT lifted in Phase 2. It will be lifted in Phase 3 when the sanitizer override is actually implemented. This follows the minimal-bridge-surface principle: every method in `RteExtensionBase` is a coupling point reviewed on Vaadin upgrades — unused methods are dead weight.

```java
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;

public abstract class RteExtensionBase extends RichTextEditor {

    /**
     * Visibility-widening override: package-private → protected.
     * {@code RichTextEditor.runBeforeClientResponse()} is package-private,
     * invisible to {@code EnhancedRichTextEditor} in
     * {@code com.vaadin.componentfactory}. This override widens it to
     * {@code protected} so subclasses in other packages can use it.
     */
    @Override
    protected void runBeforeClientResponse(
            SerializableConsumer<UI> command) {
        super.runBeforeClientResponse(command);
    }
}
```

**Open question resolved:** Progress file Open Question #1 ("Which package-private members need lifting?") — Answer: Only `runBeforeClientResponse` for Phase 2. `sanitize()` deferred to Phase 3.

### Step 3: Update `EnhancedRichTextEditor.java` — add annotations

**File:** `enhanced-rich-text-editor-v25/src/main/java/com/vaadin/componentfactory/EnhancedRichTextEditor.java`

```java
@Tag("vcf-enhanced-rich-text-editor")
@JsModule("./vcf-enhanced-rich-text-editor.js")
public class EnhancedRichTextEditor extends RteExtensionBase {
    // Phase 2: empty shell, features in Phase 3+.
    // RichTextEditor has a public no-arg constructor (validated in spike Item 8).
    // No explicit constructor needed — default inherited constructor suffices.
}
```

**Key:**
- `@Tag` on the subclass overrides the parent's `@Tag("vaadin-rich-text-editor")` — standard Vaadin pattern, confirmed in spike Item 9
- `@JsModule` points to our frontend resource — Vaadin bundles it automatically
- No `@NpmPackage` — inherited from parent (spike Item 17)
- No constructor needed — `RichTextEditor` has a public no-arg constructor

**Risk note (`@Tag` override):** If Vaadin framework code ever does tag-name-based lookup instead of using the element instance, it could miss our custom tag. The spike passing (Item 9, 13, 15) mitigates this significantly, but it's worth monitoring on Vaadin upgrades.

### Step 4: Update demo view to use ERTE

**File:** `enhanced-rich-text-editor-demo/src/main/java/com/vaadin/componentfactory/V25DemoView.java`

```java
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("")
@PageTitle("ERTE V25 - Phase 2")
public class V25DemoView extends VerticalLayout {
    public V25DemoView() {
        var erte = new EnhancedRichTextEditor();
        erte.setWidthFull();
        erte.setMaxHeight("400px");
        // setValue() accepts HTML. Use asDelta().setValue() for Delta JSON.
        // Throws IllegalArgumentException if value starts with [ or { (Delta guard).
        erte.setValue("<p>ERTE V25 — Phase 2 (Shell)</p>");

        // Delta output — verifies asDelta() round-trip (AC #4)
        var deltaOutput = new Pre();
        deltaOutput.setId("delta-output");
        deltaOutput.getStyle().set("white-space", "pre-wrap")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "200px")
                .set("overflow", "auto");
        deltaOutput.getElement().setAttribute("tabindex", "0");
        deltaOutput.getElement().setAttribute("aria-label", "Delta output");
        erte.asDelta().addValueChangeListener(e ->
                deltaOutput.setText(e.getValue()));

        add(
            new H2("ERTE V25 — Phase 2 (ERTE Shell)"),
            erte,
            new H3("Delta Output"),
            deltaOutput
        );
    }
}
```

**Changes from v1 plan:**
- Added Delta output element to verify `asDelta()` round-trip (exercises both HTML and Delta API paths)
- `H2` for page title, `H3` for subsection — correct heading hierarchy (WCAG 2.1 SC 1.3.1)
- `tabindex="0"` + `aria-label` on scrollable Pre (WCAG 2.1.1 keyboard accessibility)
- Comment on `setValue()` documenting the Delta guard behavior
- Explicit imports listed (removed `RichTextEditor` import, `EnhancedRichTextEditor` is same package — no import needed)

### Step 5: Build & manual verification

1. `bash v25-build.sh` — must succeed (compiles ERTE addon JAR)
2. `bash v25-build-clean.sh` — if dev bundle issues (forces fresh frontend build)
3. `bash v25-server-start.sh` — must start without errors
4. Browser check:
   - Tag is `vcf-enhanced-rich-text-editor` (inspect DOM)
   - Toolbar renders with all standard RTE 2 buttons + icons
   - Typing, formatting (bold/italic), undo/redo work
   - Content displays correctly
   - Delta output updates on editor changes
   - No console errors (except dev-mode noise)
   - Console shows `[ERTE] Shell ready, _editor: true`
5. Java API check:
   - `setValue("<p><strong>Bold</strong> text</p>")` renders correctly
   - `asDelta()` returns valid Delta JSON (visible in Delta output)
6. Theme variant check:
   - Temporarily add `erte.addThemeVariants(RichTextEditorVariant.LUMO_COMPACT)` — verify toolbar becomes denser
   - Remove after verification
7. Visual fidelity spot-checks:
   - Focus ring appears when tabbing into editor
   - Color picker overlay opens at correct position
   - Toolbar button tooltips show on hover
   - Toolbar wraps at narrow viewport (~400px wide)

### Step 6: Playwright config update + automated test

**Step 6a: Update Playwright config for V25 port**

The existing `playwright.config.ts` uses `baseURL: 'http://127.0.0.1:8080'` (V24 server port). The V25 server runs on port **8082** (`v25-server-start.sh`). Options:

1. **Environment variable** (recommended): `baseURL: process.env.BASE_URL || 'http://127.0.0.1:8080'`. Run V25 tests with `BASE_URL=http://127.0.0.1:8082 npx playwright test tests/erte/erte-shell.spec.ts`.
2. **Separate config**: `playwright-v25.config.ts` with port 8082.
3. **Update baseURL**: Change to 8082 (breaks V24 tests).

Option 1 is best — both V24 and V25 tests coexist. The existing `ERTE_TEST_BASE` in `helpers.ts` also needs the same treatment.

**Step 6b: Create `erte-shell.spec.ts`**

**File:** `enhanced-rich-text-editor-demo/tests/erte/erte-shell.spec.ts`

Test view: `V25DemoView` at route `/`. This test does NOT use `waitForEditor()` from helpers.ts (that requires `#test-ready` which is only in dedicated test views). Instead, it waits for the custom element to be defined and the editor to be ready.

**Test cases (6 tests):**
1. **Tag is correct** — `document.querySelector('vcf-enhanced-rich-text-editor')` exists, tagName is correct
2. **Toolbar renders** — `[part="toolbar"]` visible in shadow DOM, contains standard button groups (history, emphasis, style, heading, etc.)
3. **Editor is editable** — click into editor, type text, verify it appears in content
4. **setValue works** — initial content "ERTE V25 — Phase 2 (Shell)" is visible in editor
5. **Quill instance available** — `element._editor` exists, `getLength() > 0`
6. **Delta round-trip** — `#delta-output` contains valid Delta JSON after editor loads

**Note:** This is NOT the full ERTE test suite (173 tests). It is a minimal smoke test for the extension mechanism. Full feature tests come in Phase 3.

### Step 7: Manual lifecycle verification (via Playwright MCP)

In addition to the automated test, a one-time manual check:
1. `document.querySelector('vcf-enhanced-rich-text-editor')` returns the element
2. `element.tagName` === `'VCF-ENHANCED-RICH-TEXT-EDITOR'`
3. `element._editor` is a Quill instance (has `.getContents()`, `.getLength()`)
4. `element._editor.getLength() > 0` after setValue from Java
5. Typing in editor produces `text-change` events
6. Delta output element updates when editor content changes

### Step 8: Update progress file + memory

Update `migration_v25/progress/2_erte_shell.md` with completed steps. Close Open Question #1: "Only `runBeforeClientResponse` needs lifting for Phase 2. `sanitize()` deferred to Phase 3."

Add Phase 3 sanitizer security checklist to progress file:
- [ ] PlaceholderBlot: use `textContent`/`createElement`, NOT `innerHTML`
- [ ] CSS value sanitizer on `style` attributes (strip `url()`, `expression()`, `-moz-binding`)
- [ ] `data:` protocol restricted to `image/*` MIME types only
- [ ] Table color values validated against CSS color allowlist

## Files Changed

| Action | File |
|--------|------|
| CREATE | `enhanced-rich-text-editor-v25/src/main/resources/META-INF/resources/frontend/vcf-enhanced-rich-text-editor.js` |
| EDIT | `enhanced-rich-text-editor-v25/.../RteExtensionBase.java` |
| EDIT | `enhanced-rich-text-editor-v25/.../EnhancedRichTextEditor.java` |
| EDIT | `enhanced-rich-text-editor-demo/.../V25DemoView.java` |
| EDIT | `enhanced-rich-text-editor-demo/playwright.config.ts` (port env var) |
| EDIT | `enhanced-rich-text-editor-demo/tests/erte/helpers.ts` (port env var) |
| CREATE | `enhanced-rich-text-editor-demo/tests/erte/erte-shell.spec.ts` |
| EDIT | `migration_v25/progress/2_erte_shell.md` |

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| ThemableMixin styles not inherited by custom tag | Toolbar buttons without icons | Spike Item 11 confirmed styles ARE inherited. Fallback: explicit `static get styles()` copy. |
| `super.render()` doesn't work in subclass | Blank component | Spike Item 4 & 13 confirmed it works. Fallback: copy render template (last resort). |
| Dev bundle doesn't pick up the new JS module | Component not found at runtime | `@JsModule` ensures Vaadin bundles the file. Use `v25-build-clean.sh` if needed. |
| `@Tag` override missed by framework tag-name lookup | Silent feature loss | Spike Item 9, 13, 15 all pass. Monitor on Vaadin upgrades. |
| RTE 2 import path changes in patch release | JS module not found at runtime | Using package entry point (not `/src/` path) + guard on `customElements.get()` gives clear error. |
| Host app uses `vaadin-rich-text-editor { --vaadin-* }` tag selectors | ERTE misses custom properties | Document in migration guide: use `html { }` or shared class for custom properties, not tag selector. |

## Definition of Done

- [ ] JS file exists at `META-INF/resources/frontend/vcf-enhanced-rich-text-editor.js`
- [ ] JS file has CVALv3 license header, guard on `customElements.get()`, defensive styles spread, exports class
- [ ] `RteExtensionBase` lifts `runBeforeClientResponse` only (no `sanitize` yet), with correct Javadoc
- [ ] `EnhancedRichTextEditor` has `@Tag` + `@JsModule`, no explicit constructor
- [ ] `mvn clean install -DskipTests` passes
- [ ] Server starts without errors on port 8082
- [ ] DOM shows `vcf-enhanced-rich-text-editor` tag
- [ ] Standard toolbar renders with all icons (history, emphasis, style, heading, etc.)
- [ ] Typing, formatting, undo/redo, color, link, image all work
- [ ] `setValue()` from Java renders HTML content correctly
- [ ] `asDelta()` returns valid Delta JSON (visible in demo Delta output)
- [ ] Theme variants work (compact verified)
- [ ] Focus ring, color picker, tooltips, responsive wrapping verified
- [ ] No console errors (except Vaadin Copilot dev-mode noise)
- [ ] `[ERTE] Shell ready, _editor: true` in console
- [ ] Playwright config supports V25 port (env var)
- [ ] Automated Playwright shell test passes (6 tests)
- [ ] Progress file updated, Open Question #1 closed
- [ ] Phase 3 sanitizer security checklist added to progress file

## Out of Scope (Phase 3+)

- Custom toolbar buttons (NBSP, whitespace, readonly, placeholder)
- Sanitizer override (whitelist ERTE classes on `<span>`) — includes `sanitize()` lifting in `RteExtensionBase`
- Custom blot registration (Tab, Placeholder, ReadOnly, SoftBreak, Nbsp)
- Rulers
- Keyboard bindings
- Slots for custom components
- Any visual or behavioral difference from stock RTE
- `user_description.md` update (should be aligned with two-package bridge pattern after Phase 2)
- Migration guide documentation (tag selector vs html selector for custom properties, `::part()` dual selectors)

## Dependencies

- Phase 1 complete (DONE)
- Vaadin 25.0.5 BOM (DONE)
- Java 21 (DONE)
- Spring Boot 4.0.2 (DONE)

## Security Notes

Phase 2 is security-neutral: identical attack surface to stock RTE 2. No new inputs, outputs, or trust boundary changes. The known RTE 2 inherited concerns (unrestricted `style` attribute values, unrestricted `data:` protocol on images) are tracked for Phase 3 resolution in the sanitizer security checklist above. See `SECURITY.md` for full details.
