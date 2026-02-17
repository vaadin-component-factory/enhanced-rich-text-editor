# ERTE v25 Migration - Agents Analysis

Analysis of `user_description.md` by all specialist agents. Each section summarizes what's clear, what's missing, and what the user needs to clarify before implementation can begin.

---

## 1. Requirements Reviewer

### What's CLEAR
- Goal: migrate ERTE from Vaadin 24/Quill 1 to Vaadin 25/Quill 2
- Architecture principle: extend RTE 2 at runtime, don't fork
- Table extension needs special attention (use original Quill 2 addon as base)
- Full use-case inventory required before migration starts
- Target: Vaadin 25.0.x stable releases only

### What's AMBIGUOUS
- "Recreate from scratch" vs "migrate features" -- is the Java API a clean break or must it be backward-compatible?
- "Hook into existing code base at runtime" -- no concrete mechanism defined, only an idea (toolbar slot injection via selectors)
- "Features not already included in RTE 2" -- who determines feature overlap? When?

### What's MISSING
- **Acceptance criteria** for each migration step (what does "done" look like?)
- **Phased delivery plan** (can ERTE 2 ship without tables initially?)
- **Breaking changes policy** -- are Java API changes acceptable? Tag name changes? Delta format changes?
- **Data migration strategy** -- customers have stored Delta JSON from ERTE 1. Must it load in ERTE 2 unchanged?
- **Performance requirements** -- must runtime extension match fork performance?
- **Timeline / priority** -- no dates, no ordering of features
- **Rollback plan** -- what if the "extend at runtime" approach fails for certain features?

---

## 2. Architecture Guard

### Key Findings
- Current ERTE is a **full fork** of RTE 1: 2,774-line Polymer web component + 253-line blots file + vendored Quill 1.3.6
- The toolbar is the **single biggest risk** -- 270+ lines of hardcoded HTML with 26 named slots, entirely copied from RTE 1
- The `extendOptions` callback pattern (used by tables addon) is a proven extension mechanism that could be reused
- Java layer uses `@Tag("vcf-enhanced-rich-text-editor")` with its own `GeneratedEnhancedRichTextEditor` base class

### Missing Architectural Decisions (P0 -- must answer before any code)
1. **RTE 2 internals audit**: Does RTE 2 expose toolbar `<slot>` elements? Does it expose the Quill 2 instance? Does it have a hook before Quill instantiation?
2. **Tag identity**: Will ERTE 2 use RTE 2's tag (`<vaadin-rich-text-editor>`) or define its own?
3. **Quill 2 Parchment API delta**: Exact breaking changes for all 5 core blots + 4 table blots
4. **Delta backward compatibility**: Must ERTE 2 read ERTE 1 deltas without modification?

### Recommended Architecture
- Hybrid approach: composition wrapper (Java) + `@JsModule` enhancement scripts (JS)
- Bootstrap JS loaded BEFORE Quill instantiation (register blots, keyboard bindings, clipboard matchers)
- Runtime JS loaded AFTER RTE ready (toolbar modification, tab engine, rulers)
- Toolbar fallback plan needed if RTE 2 has no slots

---

## 3. Fullstack Developer

### Complete Feature Inventory (from codebase analysis)
Features that must be preserved or consciously dropped:

| Feature | Complexity | Notes |
|---------|-----------|-------|
| Tab stop system (3 alignments, rulers, iterative width calc) | HIGH | 100% ERTE-specific |
| Soft-break system (visual line breaks with tab copying) | HIGH | 100% ERTE-specific |
| Placeholder system (embed blot, 8 server events, alt appearance) | MEDIUM | 100% ERTE-specific |
| Read-only sections (inline blot, delete protection) | LOW-MEDIUM | 100% ERTE-specific |
| Toolbar customization (24 slots, visibility control) | MEDIUM | May overlap with RTE 2 |
| Non-breaking space (Shift+Space) | LOW | 100% ERTE-specific |
| Whitespace indicators (CSS pseudo-elements) | LOW | 100% ERTE-specific |
| HTML sanitization (extended safelist) | LOW | Must be updated for Quill 2 output |
| Programmatic text insertion (addText) | LOW | Uses Quill API directly |
| I18n (30+ fields including ERTE-specific ones) | LOW | Must extend RTE 2 i18n |
| Theme variants (no-border, compact) | LOW | |
| Extension hook (`extendOptions` callback) | LOW | Must be recreated or adapted |
| Tables (merge/split, templates, cell selection, history) | HIGH | Separate module, ~4000 lines |

### Quill 1 to Quill 2 Breaking Changes
- `Quill.import('blots/embed')` -- Embed API changed (cursor guards differ)
- `Inline.order.push()` -- may not exist in Quill 2's Parchment 3.x
- `keyboard.bindings[keyCode]` -- Quill 2 uses key names, not keyCodes
- `editor.history.stack.undo/redo` -- internal API, likely changed
- 8 clipboard matchers use `quill.clipboard.addMatcher()` -- API restructured
- `Parchment` version jumps from 1.x to 3.x

### Suggested Phased Approach
| Phase | Scope | Est. Effort |
|-------|-------|-------------|
| Phase 0: Discovery | Audit RTE 2 internals, validate extension approach | 1-2 weeks |
| Phase 1: Core ERTE | Tabs, soft-breaks, readonly, nbsp, whitespace, toolbar | 3-5 weeks |
| Phase 2: Placeholders | Full placeholder system with events | 2-3 weeks |
| Phase 3: Tables | Table extension with merge/split, templates | 3-5 weeks |
| Phase 4: Integration | Demo app, tests, data migration verification | 2-3 weeks |
| **Total** | | **11-18 weeks** |

---

## 4. Security Reviewer

### Pre-Existing Vulnerabilities (Fix Before or During Migration)
- **CRITICAL: PlaceholderBlot XSS** -- `vcf-enhanced-rich-text-editor-blots.js:197` uses `node.innerHTML = text` where `text` comes from placeholder data (user-controllable via stored delta). This is a stored XSS vector. **Fix: use `textContent` instead of `innerHTML`.**
- **WARNING: CSS injection in table colors** -- `EnhancedRichTextEditorTables.java:390-413` concatenates `tableHoverColor`, `cellHoverColor`, etc. directly into CSS strings without validation. Values like `red; } * { display:none` break out of declarations.
- **WARNING: TemplateParser CSS values** -- `isValidPropertyValue()` is commented out (line 336-356). CSS property values are not validated, only template IDs are.
- **WARNING: Unrestricted `style` attribute** -- HTML sanitizer allows `.addAttributes(":all", "style")` on all elements without sanitizing style values (no filter for `url()`, `expression()`, `behavior()`).

### Runtime Injection Risks
- Extending RTE 2 via DOM manipulation in Shadow DOM is a **new attack surface** -- all runtime DOM injection must use `createElement`/`appendChild`, never `innerHTML` with dynamic content
- Quill 2 has stricter format allowlists -- custom blots must be registered **synchronously before** any `setContents()` call, or unregistered blots render as raw HTML spans bypassing format restrictions
- If RTE 2 uses closed shadow roots, ERTE cannot inject at all

### HTML Sanitization Concerns
- Current sanitizer allows `span` with `class` and `contenteditable`, then post-filters class values to `ql-tab`, `ql-soft-break`, `ql-readonly`, `ql-placeholder`
- Quill 2 produces different HTML output -- sanitizer allowlist must be updated to match
- `data:` protocol allowed on images without MIME type restriction -- permits `data:text/html` payloads

### Missing Security Considerations
- No mention of Content Security Policy (CSP) implications of runtime DOM injection
- No mention of how `executeJs()` calls (20+ in codebase) will be secured in the new architecture
- No security regression test plan -- sanitizer should be tested against XSS payload corpus after migration
- Stored delta migration should happen server-side in batch, not on-the-fly during `setValue()`

---

## 5. QA Tester

### Existing Test Coverage
- **75 Playwright tests** for tab-stop prototype (comprehensive, well-structured)
- **21 JUnit tests** for TabConverter (delta format conversion)
- JUnit tests for core component (basic, using JUnit 4 + PowerMock)
- **No Playwright tests for**: placeholders, read-only sections, toolbar customization, tables

### Test Migration Assessment
- Playwright tests **can largely be reused** if CSS classes (`ql-tab`, `ql-soft-break`) and Delta format stay the same
- Page selectors will need updating if the web component tag changes
- JUnit tests need migration from JUnit 4 to JUnit 5 (PowerMock 1.7.1 incompatible with Java 21+)

### Missing Test Scenarios for Migration
- **Feature parity tests**: Every ERTE 1 feature needs a test before migration starts (baseline)
- **Table extension tests**: Zero automated tests exist for tables (merge/split, templates, selection)
- **Placeholder system tests**: No automated tests
- **Cross-browser testing**: Only Chromium tested currently
- **Data migration tests**: Load ERTE 1 deltas in ERTE 2, verify rendering
- **Performance regression tests**: Compare rendering speed, bundle size

### Missing Acceptance Criteria
- No definition of "feature parity achieved"
- No performance benchmarks to meet
- No compatibility matrix (which Vaadin 25.x versions supported)

---

## 6. Dependency Auditor

### Current Dependencies
| Dependency | Version | Issue |
|-----------|---------|-------|
| Java | 17 | **Must upgrade to 21+** for Vaadin 25 |
| Vaadin BOM | 24.9.7 | Must change to 25.0.x |
| Spring Boot | 3.5.9 | Verify Vaadin 25 compatibility |
| Quill | 1.3.6 (vendored) | **Entire file must be removed** -- RTE 2 ships Quill 2 |
| Parchment | 1.1.4 | Jumps to 3.x in Quill 2 -- **all blots must be rewritten** |
| quill-delta | 5.1.0 (npm) vs 3.6.x (bundled in Quill 1) | Version mismatch resolves in Quill 2 |
| @polymer/polymer | 3.5.2 | **May be dropped** if extending Lit-based RTE 2 |
| PowerMock | 1.7.1 | **Incompatible with Java 21+** -- must replace with Mockito 5.x |
| Mockito | 1.10.19 | **Severely outdated** -- must upgrade to 5.x |
| Dockerfile | eclipse-temurin:17 | Must change to JDK 21+ |

### Missing Dependency Information
- No target Vaadin 25.x version pinned
- No Quill 2 version specified
- No concrete Quill 2 table module identified (candidates: `quill-table-better`, `quill2-table`)
- No mention of Parchment 3.x migration (the biggest JS breaking change)
- No mention of Polymer-to-Lit migration

---

## 7. Performance Auditor

### Runtime Extension Overhead Concerns
- DOM manipulation in Shadow DOM (toolbar injection) may trigger layout recalculation
- If using MutationObservers to detect RTE 2 changes, risk of layout thrashing
- Tab width calculation engine iterates over all tabs on every text change -- must verify Quill 2 doesn't make this worse

### Potential Improvements
- Quill 2 is generally faster than Quill 1 (modern JS, better tree shaking)
- Removing vendored Quill (172K minified) in favor of RTE 2's bundled Quill 2 may reduce total bundle size
- ES module format (Quill 2) enables better dead code elimination

### Missing
- No performance baseline defined for ERTE 1
- No target metrics for ERTE 2 (max latency, bundle size budget)

---

## 8. Docs Engineer

### Document Quality Issues
- Written as informal notes, not a migration specification
- No section numbering or formal structure
- Terminology introduced in "wording" section but used inconsistently (e.g., "rte" vs "rte 2" vs "rte 25")
- Several typos and informal language ("analyized", "i would imagine")

### Missing Standard Sections
- **Timeline / milestones** -- no dates or phase boundaries
- **Rollback plan** -- what if runtime extension doesn't work?
- **Compatibility matrix** -- which browser/Java/Vaadin versions?
- **API migration guide** -- what changes for ERTE users?
- **Risk register** -- identified risks with mitigation strategies
- **Success criteria** -- measurable definition of done
- **Dependencies / prerequisites** -- what must be ready before starting?

---

## Summary: Blocker Questions -- ANSWERED by Source Code Analysis

*The following questions were originally blockers. Research agents analyzed the actual RTE 2 source code (v25.0.4/25.0.5 from GitHub) and Quill v1/v2 documentation to answer them.*

### Blocker 1: RTE 2 Internals Audit -- ANSWERED

| Question | Answer |
|----------|--------|
| Does it expose `<slot>` elements in the toolbar? | **NO.** Zero toolbar slots. Toolbar is hardcoded Lit HTML with `<button>` elements in `<span>` groups. Only slots are for overlays (tooltip, link-dialog, color-popup, background-popup). |
| Does it expose the Quill instance as a public property? | **YES, but private.** `element._editor` (underscore-prefixed). Not a `#private` field, so accessible via JS. RTE 2 considers it internal API. |
| Does it have a lifecycle hook before Quill instantiation? | **NO.** Quill is created directly in `ready()` with no `extendOptions` pattern. However, **global `Quill.register()` before RTE 2 module import works** -- RTE 2 itself uses this pattern for its CodeBlockContainer override. |
| What is its Shadow DOM structure? | **Open shadow DOM.** Accessible via `element.shadowRoot.querySelector(...)`. See `implementation_notes.md` section 1 for full DOM tree. |
| Framework? | **Lit (LitElement)**, not Polymer. Uses `PolylitMixin` for Polymer-style observer compatibility. |
| Tag name? | `<vaadin-rich-text-editor>` |

**Conclusion**: Runtime extension IS viable, but toolbar injection is the hardest part (no slots, Lit re-renders can destroy injected DOM). Blot registration is the EASY part (global `Quill.register()` works before any element is created).

### Blocker 2: Breaking Changes -- PARTIALLY ANSWERED

| Question | Answer |
|----------|--------|
| Java API backward-compatible? | **Possible if extending `RichTextEditor` directly.** Most ERTE API methods can be added as new methods on the subclass. The main breaking change is the primary value format shift from Delta to HTML. |
| HTML tag name? | **Recommend keeping `vaadin-rich-text-editor`** (same tag) -- this allows ERTE to enhance the existing web component at runtime via `@JsModule` without needing a separate web component registration. If a different tag is needed, ERTE must register its own web component. |
| Stored Delta JSON? | **RTE 2's `setValue()` throws for Delta input** (guards against values starting with `[` or `{`). But `asDelta()` wrapper provides Delta-compatible `HasValue` interface. Existing ERTE 1 deltas can be loaded via the Delta API, but the primary `setValue()`/`getValue()` API now expects HTML. |

**Still needs YOUR decision**: Is the HTML-primary value format acceptable? Or must ERTE 2 override this to keep Delta-primary?

### Blocker 3: Table Extension Strategy -- DECIDED

**Decision: Rewrite ERTE table blots for Quill 2.**

Fork analysis completed (see `implementation_notes.md` section 8):
- Forked from `dclement8/quill1-table` (itself from `acatec/quill-table-tbf` ← `dost/quilljs-table`)
- Original has **no Quill 2 port** and no active development
- No existing Quill 2 table addon uses the same pipe-separated delta format
- Core blots (~1,800 LOC JS) need 100% rewrite for Parchment v3
- TableHistory (~200 LOC) needs 100% rewrite (Quill 2 history stack incompatible)
- Java server-side (~3,500 LOC) stays **completely untouched** (format-agnostic)
- Delta format is preserved -- existing table data remains compatible

### Blocker 4: Java Version -- Confirmed Required

Vaadin 25 requires Java 21+. This forces:
- PowerMock 1.7.1 → remove (incompatible with Java 21)
- Mockito 1.10.19 → Mockito 5.x
- Dockerfile: `eclipse-temurin:17` → `eclipse-temurin:21`

---

### Remaining TODOs (For User)

**Must decide:**
1. **Value format**: Accept HTML-primary with `asDelta()` wrapper? Or override to keep Delta-primary?
2. **Phased delivery**: Must tables ship with initial release? (You said: no)
3. ~~Verify Quill version~~ -- **Confirmed: Quill 2.0.3** (vendored in `vendor/vaadin-quill.js`). All Quill v1→v2 migration notes apply immediately.

**Already answered by research (see companion docs):**
- Feature parity inventory → `feature_comparison.md` (20 features compared, 17 need CUSTOM migration)
- Quill API migration guide → `quill_v1_to_v2_api_diff.md` (22 API areas, 4 HIGH impact)
- RTE 2 extension points → `implementation_notes.md` (viable approach with risks documented)
- Rollback plan → If toolbar injection fails, fallback is prototype `render()` override or partial fork of the web component only

### Recommended First Step (Updated)

**Time-boxed spike (2-3 days):** Create a minimal Vaadin 25 project and validate:
1. Check `element._editor.constructor.version` to confirm Quill version
2. Register ONE custom Embed blot via `Quill.register()` before element creation
3. Insert content containing that blot and verify it renders
4. Inject ONE button into the toolbar shadow DOM
5. Trigger a property change (e.g., `setI18n()`) and verify the button survives Lit re-render
6. Extend `RichTextEditor` in Java, verify `@Tag` inheritance and `@JsModule` loading order

See `implementation_notes.md` section 9 for the complete 10-item spike checklist.
