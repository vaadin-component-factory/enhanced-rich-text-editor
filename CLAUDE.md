# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Enhanced Rich Text Editor (ERTE) for Vaadin — a rich text editor component extending Vaadin's built-in RTE with tabstops, placeholders, non-breaking space, rulers, customizable toolbar, read-only sections, and more.

**Active work:** Migrating from Vaadin 24 / Quill 1 (ERTE 1) to Vaadin 25 / Quill 2 (ERTE 2). The old V24 modules remain in the repo as dead reference code (excluded from build). New V25 modules are created from scratch, using the old code as context.

## Root Scripts

Convenience scripts in the repo root for build, server, and test operations. **Always use these instead of running Maven/server commands manually.**

| Script | Purpose |
|--------|---------|
| `v24-build.sh [-q]` | `mvn clean install -DskipTests` for V24 modules |
| `v24-build-clean.sh [-q]` | Same + `vaadin:clean-frontend` (wipes dev bundle) |
| `v24-server-start.sh [port]` | Start V24 demo on port 8080 (default) |
| `v24-server-stop.sh` | Stop V24 demo server |
| `v24-server-logs.sh [-f\|-errors]` | Print V24 server logs |
| `v24-server-status.sh` | Check V24 server status |
| `v25-build.sh [-q]` | Build V25 modules |
| `v25-build-clean.sh [-q]` | Same + `vaadin:clean-frontend` |
| `v25-server-start.sh [port]` | Start V25 demo on port 8082 (default) |
| `v25-server-stop.sh` | Stop V25 demo server |

**Workflow:** After changing addon code (ERTE JS/Java), always `v24-build.sh` before `v24-server-start.sh`. Tests require a running server.

**Tests should run as background tasks** (`run_in_background: true`) to avoid blocking.

## Build Commands

```bash
# V24 build + server (preferred — use root scripts)
bash v24-build.sh
bash v24-server-start.sh

# Run Playwright tests (server must be running first)
cd enhanced-rich-text-editor-demo
npx playwright test

# Run only ERTE tests
npx playwright test tests/erte/

# Playwright with UI for debugging
npx playwright test --ui

# Maven commands (fallback if scripts unavailable)
mvn clean install -DskipTests
mvn -pl enhanced-rich-text-editor-demo spring-boot:run
mvn verify
mvn clean package -Pproduction -DskipTests
```

## Project Structure

Multi-module Maven project. V25 target: Java 21+, Vaadin 25.0.x, Spring Boot 4.x.

- **enhanced-rich-text-editor/** — Core component (V24, reference only, excluded from build)
- **enhanced-rich-text-editor-tables/** — Tables addon (V24, reference only, excluded from build)
- **enhanced-rich-text-editor-demo/** — Demo application
- **enhanced-rich-text-editor-v25/** — *(new)* V25 core component, extends Vaadin's RTE 2
- **enhanced-rich-text-editor-tables-v25/** — *(new)* V25 tables addon

### Data Format
Content is stored as Quill Delta JSON. Tables encode cell metadata in a pipe-separated format within the `td` attribute:
`{tableId}|{rowId}|{cellId}|{rowspan}|{colspan}|{templateId}|{uniqueId}`

The V25 primary value format is HTML (matching RTE 2), with Delta access via `asDelta()` wrapper.

## Migration Rules

### Golden Rules

1. **No feature regression.** Every ERTE 1 feature must exist in ERTE 2. If you encounter a feature where no clear migration path exists, STOP and ask.
2. **Updatability over convenience.** Never copy RTE 2 source code. Extend at runtime. The only acceptable "fork" is the `render()` override in the JS subclass.
3. **One feature at a time.** Complete and verify each feature before starting the next.
4. **Security fixes are mandatory.** See `SECURITY.md` — known XSS vectors from ERTE 1 must be fixed during migration, not reproduced.

### Architecture Decisions (confirmed by spike — all PASS)

- **JS extension strategy:** ES class extension of RTE 2's web component (not composition, not prototype patching). See `implementation_notes.md` section 6.
- **Java packages:** `RteExtensionBase` in `com.vaadin.flow.component.richtexteditor` (bridge lifting package-private → protected), `EnhancedRichTextEditor` in `com.vaadin.componentfactory` (all ERTE logic). Only the bridge class lives in the foreign package.
- **Tag:** `vcf-enhanced-rich-text-editor` (own tag, own web component registration).
- **Value format:** HTML-primary (matching RTE 2). Delta access via `asDelta()` wrapper. Clean break from ERTE 1's Delta-primary API.
- **Theme:** Lumo. Use `--vaadin-*` custom properties where they exist. Loads via `@StyleSheet(Lumo.STYLESHEET)` on host app.
- **Blot registration:** Global `Quill.register()` before element creation (proven pattern, used by RTE 2 itself).
- **Toolbar:** `render()` override in JS subclass. Standard RTE 2 groups + ERTE-specific groups + `<slot>` elements for custom components. No MutationObserver hacks.

### Migration Order

Follow this exact sequence. Do not skip ahead. Full spec in `user_description.md`.

1. **Step 0: Use Case Analysis** — Cross-reference each ERTE 1 feature with `feature_comparison.md`
2. **Step 1: Project Base** — Vaadin 25.0.x, all dependencies, Maven profiles, Dockerfile
3. **Step 2: ERTE Shell** — JS subclass + Java subclass, `render()` override, verify Lit lifecycle
4. **Step 3: Feature Migration** (in order):
    1. Custom Slots / Custom Components
    2. Readonly Mode
    3. Tabstops (+ Rulers, Soft Wraps)
    4. Placeholders
    5. Remaining features (any order)
5. **Step 4: Table Extension** (last) — Rewrite blots for Quill 2 / Parchment 3

### Confirmed Patterns (from Spike)

These patterns were validated in the spike (`SPIKE_RESULTS.md`). Use them as-is.

- **Keyboard binding priority:** `addBinding()` appends to END of handler array. To override defaults, clear the array first, add ERTE handler, then re-add originals. See Item 7.
- **Guard nodes:** Quill 2 Embed guard nodes (`\uFEFF`) are INSIDE the embed element. Measure the OUTER `.ql-tab` rect for tab widths, NOT `contentNode`. See Item 20.
- **Sanitizer strips `class`:** RTE 2's `sanitize()` strips `class` and `contenteditable` from `<span>`. Override `sanitize()` to whitelist ERTE classes, or prefer `asDelta().setValue()`. See Item 19.
- **Lumo loading:** V25 requires `@StyleSheet(Lumo.STYLESHEET)` on `AppShellConfigurator`. No ERTE-specific theme registration needed. See Item 11.
- **Lifecycle:** `_editor` available immediately after `super.ready()`. Content set in `ready()` gets overwritten by Java value sync — always set content via Java API. See Item 14.
- **Parchment 3 table blots:** 5 critical API changes required. See Phase 5.

### What NOT to Do

- Do not use `innerHTML` with dynamic content — use `createElement`/`appendChild`
- Do not access `Quill.imports.delta` — use `Quill.import('delta')`
- Do not use numeric keyCodes for keyboard bindings — use string key names (`'Tab'`, `'Enter'`, etc.). Numeric codes create bindings under wrong keys and silently fail.
- Do not access `domNode.__quill` — use `Quill.find(domNode)`
- Do not access `domNode.__blot` — use `Quill.find(domNode)`
- Do not use `Parchment.create()` — removed in Parchment 3, use `this.scroll.create(blotName, value)`
- Do not copy RTE 2 toolbar HTML — override `render()` in the JS subclass
- Do not use Polymer patterns — this is Lit
- Do not set content in `ready()` — it gets overwritten by Java value sync

## Tech Stack (V25)

- Java 21+
- Vaadin 25.0.x (stable, no pre-releases) — RTE is now a **Pro component** (`vaadin` artifact, not `vaadin-core`)
- Spring Boot 4.x / Spring Framework 7
- Quill 2.0.3 (vendored by RTE 2)
- Parchment 3.x
- Jackson 3 (`tools.jackson`, replaces `com.fasterxml.jackson`)
- Mockito 5.x (replaces PowerMock + Mockito 1.x)
- JUnit 5

## Reference Documents

Use these as needed, do NOT try to load all of them as context simultaneously.

| Document | When to consult |
|----------|----------------|
| `user_description.md` | Always — primary migration spec with full feature inventory |
| `SPIKE_RESULTS.md` | Architecture/lifecycle questions, confirmed patterns, Parchment 3 breaking changes |
| `feature_comparison.md` | Before implementing any feature — check what RTE 2 already provides |
| `implementation_notes.md` | JS/Java architecture, shadow DOM structure, pre-spike design notes |
| `quill_v1_to_v2_api_diff.md` | When touching any Quill API — keyboard bindings, blots, history, clipboard |
| `SECURITY.md` | When implementing blots, sanitization, or any DOM manipulation |
| `migration_v25/progress/` | **Always update** after completing a task — one file per phase (e.g., `0_use_cases_tests.md`, `1_project_base.md`) |
| `migration_v25/USE_CASE_ANALYSIS.md` | Feature inventory with migration paths for all 20 ERTE features |
| `tests/TEST_INVENTORY.md` | Full test listing grouped by feature (update when adding/removing tests) |

## Playwright Tests

248 total tests: 75 prototype + 173 ERTE. Full listing in [TEST_INVENTORY.md](enhanced-rich-text-editor-demo/tests/TEST_INVENTORY.md).

**Running tests:**
```bash
# Build first
mvn clean package -DskipTests
# Start server (from demo dir)
cd enhanced-rich-text-editor-demo && bash server-start.sh
# Run all tests
npx playwright test
# Run only ERTE tests
npx playwright test tests/erte/
# Stop server
bash server-stop.sh
```

### ERTE Test Suite (173 tests in `tests/erte/`)

| Spec File | Tests | Covers |
|-----------|-------|--------|
| `tabstops.spec.ts` | 75 | Tabstops, rulers, soft-break, whitespace indicators |
| `placeholders.spec.ts` | 32 | Placeholder dialog, events, appearance, keyboard |
| `readonly.spec.ts` | 18 | Readonly sections, protection, whole-editor readonly |
| `toolbar.spec.ts` | 24 | Slot system, visibility, shortcuts, icons, keyboard nav |
| `features.spec.ts` | 24 | NBSP, addText, align, indent, i18n, sanitizer, focus |

**Test views** (Java, in `com.vaadin.componentfactory`): `ErteTabStopTestView`, `ErtePlaceholderTestView`, `ErteReadonlyTestView`, `ErteToolbarTestView`, `ErteFeatureTestView`. Each provides a single editor (`id="test-editor"`), delta/HTML output elements, event log, and a ready indicator.

**Shared helpers** (`tests/erte/helpers.ts`): `waitForEditor()`, `getDelta()`, `getDeltaFromEditor()`, `getRuler()`, `getRulerMarkers()`, etc.

### Prototype Tests (75 tests in `tab-stop-prototype.spec.ts`)

Original tabstop tests against the prototype view at `/tab-stop`. See [prototype_tests.md](enhanced-rich-text-editor-demo/prototype_tests.md).

### Key Test Patterns
- Shadow DOM: Playwright locators pierce it, but `page.evaluate()`/`waitForFunction` do NOT — use `el.shadowRoot.querySelector()`
- Ready indicator (`#test-ready`) has `display:none` — use `state: 'attached'`
- Readonly blot is a FORMAT attribute `{"attributes":{"readonly":true}}`, NOT an embed
- Placeholder dialog: use `[aria-label="Placeholders"]` to disambiguate from Link dialog
- Ruler markers are `vaadin-icon` elements inside `[part~="horizontalRuler"]`

## Custom Agents

Custom Claude Code agents in `.claude/agents/`. The **agents-manager** orchestrates them (tech stack discovery, project-specific injection, task delegation).

| Agent | Purpose |
|-------|---------|
| **agents-manager** | Delegates tasks to agents, discovers tech stack, injects project patterns |
| **fullstack-developer** | End-to-end feature implementation (backend + frontend) |
| **code-reviewer** | Quick code review during development (no builds/tests) |
| **qa-tester** | Comprehensive QA: code review + build + tests + responsive checks |
| **ui-explorer** | Playwright-based visual verification of running app |
| **ui-designer** | UI design review: styling, accessibility, responsiveness |
| **docs-engineer** | Documentation creation, updates, and review |
| **security-reviewer** | Security audit: auth, injection, access control, secrets |
| **architecture-guard** | Architectural compliance, import violations, structural patterns |
| **performance-auditor** | Performance issues: N+1 queries, memory leaks, rendering |
| **dependency-auditor** | Dependency vulnerabilities, outdated versions, licenses |
| **migration-auditor** | Database migration safety and backward compatibility |
| **devcontainer-auditor** | Devcontainer/Docker setup review |
| **requirements-reviewer** | Requirements review before implementation |
| **housekeeper** | Cleanup: servers, Docker, temp files, screenshots |

**Usage:** Agents are launched via the `Task` tool with `subagent_type`. The agents-manager has three modes:
- **Update mode:** Run after CLAUDE.md changes or `/init` to inject project-specific patterns
- **Review mode:** Evaluate whether the agent suite fits the project
- **Task assignment (default):** Delegate a task to the right agent(s)

## License

CVALv3 (Commercial Vaadin Add-On License). License headers are enforced on Java files.